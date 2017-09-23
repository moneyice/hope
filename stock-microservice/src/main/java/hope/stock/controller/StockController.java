package hope.stock.controller;

import hope.stock.dao.IStockDAO;
import hope.stock.filter.StockFilter;
import hope.stock.model.*;
import hope.stock.util.KUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
public class StockController {
    private Logger logger = LoggerFactory.getLogger(getClass());
    ExecutorService threadPool = Executors.newFixedThreadPool(2);
    @Resource(name = "stockDAO4Redis")
    private IStockDAO stockDAO;

    public StockController() {
    }

    @GetMapping(value="/pool")
    public String pool(){
        ThreadPoolExecutor pool=(ThreadPoolExecutor) threadPool;
        StringBuilder sb=new StringBuilder();
        sb.append("task count "+ pool.getTaskCount()).append(" queue size "+ pool.getQueue().size()).append(" completed tasks "+pool.getCompletedTaskCount());
        sb.append(" active count "+pool.getActiveCount());
        return sb.toString();

    }

    @RequestMapping(value = "/stock", method = RequestMethod.POST)
    public void storeStock(@RequestBody Stock stock) {
        logger.info("running   ======================================= " +stock.getCode());
        MyTaskDaily myTaskd = new MyTaskDaily(stock);
        threadPool.execute(myTaskd);
        //myTaskd.run();
    }

    @RequestMapping(value = "/data/kline/", method = RequestMethod.POST)
    public Stock getStock(@RequestBody Stock input) {
        if(input.getkLineType().equals("dailylite")||input.getkLineType().equals("daily")){
            return stockDAO.getStockLite(input.getCode());
        }else if (input.getkLineType().equals("weekly")){
            return stockDAO.getStockWeeklyInfo(input.getCode());
        }else if (input.getkLineType().equals("monthly")){
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
            //stockDAO.storeStock(stock);
            {
                Stock stockWeekly = KUtils.daily2Weekly(stock);
                KUtils.appendMacdInfo(stockWeekly.getkLineInfos());
                stockDAO.storeStockWeeklyInfo(stockWeekly);
                stockWeekly=null;
            }
            {
                Stock stockMonthly= KUtils.daily2Monthly(stock);
                KUtils.appendMacdInfo(stockMonthly.getkLineInfos());
                stockDAO.storeStockMonthlyInfo(stockMonthly);
                stockMonthly=null;
            }

            {
                KUtils.appendMacdInfo(stock.getkLineInfos());
                stockDAO.storeStockLite(stock);
                stock=null;
            }
        }
    }
}
