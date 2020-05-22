package hope.stock.controller;

import com.alibaba.fastjson.JSON;
import hope.stock.dao.IStockDAO;
import hope.stock.filter.StockFilter;
import hope.stock.model.KLineInfo;
import hope.stock.model.StatisticsInfo;
import hope.stock.model.Stock;
import hope.stock.model.SymbolList;
import hope.stock.util.KUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@RestController
public class StockController {
    private Logger logger = LoggerFactory.getLogger(getClass());
    ExecutorService threadPool = Executors.newFixedThreadPool(2);
    @Resource(name = "stockDAO4Redis")
    private IStockDAO stockDAO;

    public StockController() {
    }

    @GetMapping(value = "/pool")
    public String pool() {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) threadPool;
        StringBuilder sb = new StringBuilder();
        sb.append("task count " + pool.getTaskCount()).append(" queue size " + pool.getQueue().size()).append(" completed tasks " + pool.getCompletedTaskCount());
        sb.append(" active count " + pool.getActiveCount());
        return sb.toString();

    }

    @PostMapping(value = "/stock")
    public void storeStock(@RequestBody Stock stock) {
        logger.info("running   ======================================= " + stock.getCode());
        MyTaskDaily myTaskd = new MyTaskDaily(stock);
        threadPool.execute(myTaskd);
        //myTaskd.run();
    }

    @RequestMapping(value = "/data/kline/", method = RequestMethod.POST)
    public Stock getStock(@RequestBody Stock input) {
        if (input.getkLineType().equals("dailylite") || input.getkLineType().equals("daily")) {
            return stockDAO.getStockLite(input.getCode());
        } else if (input.getkLineType().equals("weekly")) {
            return stockDAO.getStockWeeklyInfo(input.getCode());
        } else if (input.getkLineType().equals("monthly")) {
            return stockDAO.getStockMonthlyInfo(input.getCode());
        }

        return input;
    }

    @RequestMapping(value = "/data/kline/daily/{code}", method = RequestMethod.GET)
    public Stock getStock(@PathVariable String code) {
        Stock stock = stockDAO.getStock(code);
        return stock;
    }

    @RequestMapping(value = "/data/kline/dailylite/{code}", method = RequestMethod.GET)
    public Stock getStockDailyLite(@PathVariable String code) {
        Stock stock = stockDAO.getStockLite(code);
        return stock;
    }

    @RequestMapping(value = "/data/kline/weekly/{code}", method = RequestMethod.GET)
    public Stock getStockWeeklyInfo(@PathVariable String code) {
        Stock stock = stockDAO.getStockWeeklyInfo(code);
        return stock;
    }

    @RequestMapping(value = "/data/kline/monthly/{code}", method = RequestMethod.GET)
    public Stock getStockYearlyInfo(@PathVariable String code) {
        Stock stock = stockDAO.getStockMonthlyInfo(code);
        return stock;
    }

    @RequestMapping(value = "/stockList", method = RequestMethod.GET)
    public SymbolList getStockList() {
        StockFilter filter = new StockFilter();
        List<Stock> list = stockDAO.getAllSymbols();

        List<Stock> result = new LinkedList<>();
        for (Stock stock : list) {
            if (filter.select(stock.getCode())) {
                result.add(stock);
            }
        }
        SymbolList symbolList = new SymbolList();
        symbolList.setSymbols(result);
        return symbolList;
    }

    @RequestMapping(value = "/stockList", method = RequestMethod.POST)
    public void storeStockList(@RequestBody List<Stock> stockList) {
        stockDAO.storeAllSymbols(stockList);
    }

    class MyTaskDaily implements Runnable {
        Stock stock;

        public MyTaskDaily(Stock stock) {
            this.stock = stock;
        }

        @Override
        public void run() {
//            stockDAO.storeStock(stock);
            {
                Stock stockWeekly = KUtils.daily2Weekly(stock);
                KUtils.appendMacdInfo(stockWeekly.getkLineInfos());
                stockDAO.storeStockWeeklyInfo(stockWeekly);
                stockWeekly = null;
            }
            {
                Stock stockMonthly = KUtils.daily2Monthly(stock);
                KUtils.appendMacdInfo(stockMonthly.getkLineInfos());
                stockDAO.storeStockMonthlyInfo(stockMonthly);
                stockMonthly = null;
            }

            {
                KUtils.appendMacdInfo(stock.getkLineInfos());
                if (stock.getCode().startsWith("i")) {
                    //this is index
                    stockDAO.storeStock(stock);
                } else {
                    stockDAO.storeStock(stock);
                    stockDAO.storeStockLite(stock);
                }
                stock = null;
            }
        }
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate redisTemplate;


    @GetMapping(value = "/getStatistics")
    public List<StatisticsInfo> getStatistics() {
        LocalDate from = LocalDate.parse("2004-01-05");
        String statistics = stringRedisTemplate.opsForValue().get("statistics");
        List<StatisticsInfo> list = JSON.parseArray(statistics, StatisticsInfo.class);

        StatisticsInfo toFind = new StatisticsInfo(from);
        int index = list.indexOf(toFind);

        List<StatisticsInfo> result = new ArrayList<>(list.subList(index, list.size() - 1));
        return result;
    }


    @GetMapping(value = "/makeStatistics")
    @Async
    public void makeStatistics() {

        Map<LocalDate, StatisticsInfo> totalStockInfo = new HashMap<>();

        SymbolList stockList = getStockList();
        logger.info("total number is {}", stockList.getSymbols().size());
        stockList.getSymbols().parallelStream().forEach(item -> {
            Stock stock = getStock(item.getCode());

            if (stock.getkLineInfos().isEmpty()) {
                logger.info("{} {} kline is empty", stock.getName(), stock.getCode());
                return;
            }
            {
                LocalDate firstDay = stock.getkLineInfos().get(0).getDate();

                Optional<StatisticsInfo> op = Optional.ofNullable(totalStockInfo.get(firstDay));
                if (!op.isPresent()) {
                    op = Optional.of(new StatisticsInfo(firstDay));
                    totalStockInfo.put(firstDay, op.get());
                }
                op.get().addTotalStockNumber();
            }

            for (KLineInfo kLineInfo : stock.getkLineInfos()) {
                if (isLimitUp(kLineInfo.getChangePercent())) {
                    Optional<StatisticsInfo> opUp = Optional.ofNullable(totalStockInfo.get(kLineInfo.getDate()));
                    if (!opUp.isPresent()) {
                        opUp = Optional.of(new StatisticsInfo(kLineInfo.getDate()));
                        totalStockInfo.put(kLineInfo.getDate(), opUp.get());
                    }
                    opUp.get().addLimitUpNumber();


                } else if (isLimitDown(kLineInfo.getChangePercent())) {
                    Optional<StatisticsInfo> opDown = Optional.ofNullable(totalStockInfo.get(kLineInfo.getDate()));
                    if (!opDown.isPresent()) {
                        opDown = Optional.of(new StatisticsInfo(kLineInfo.getDate()));
                        totalStockInfo.put(kLineInfo.getDate(), opDown.get());
                    }
                    opDown.get().addLimitDownNumber();
                }
            }
        });

        List<StatisticsInfo> result = totalStockInfo.values().parallelStream().sorted(Comparator.comparing(StatisticsInfo::getDate)).collect(Collectors.toList());

        for (int i = 1; i < result.size(); i++) {
            StatisticsInfo info = result.get(i);
            info.setTotalStockNumber(info.getTotalStockNumber() + result.get(i - 1).getTotalStockNumber());
        }
        stringRedisTemplate.opsForValue().set("statistics", JSON.toJSONString(result));

    }

    private boolean isLimitDown(double changePercent) {
        return changePercent < -9.90;
    }

    private boolean isLimitUp(double changePercent) {
        return changePercent >= 9.90;
    }


}
