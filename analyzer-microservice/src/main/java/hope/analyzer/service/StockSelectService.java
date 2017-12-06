package hope.analyzer.service;

import com.alibaba.fastjson.JSON;
import hope.analyzer.analyzer.IStockAnalyzer;
import hope.analyzer.model.*;
import hope.analyzer.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.metrics.servo.ServoMonitorCache;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
@Scope("prototype")
public class StockSelectService {
    private Logger logger = LoggerFactory.getLogger(getClass());
    List<IStockAnalyzer> analyzers = new ArrayList<IStockAnalyzer>();
    List<ResultInfo> selectResultList = null;
    @Autowired
    private RestTemplate restTemplate;

    public StockSelectService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public AnalyzeResult getAnalyzeResult() {
        AnalyzeResult ar=new AnalyzeResult();
        ar.setResultList(selectResultList);
        StringBuilder sb=new StringBuilder();
        for (IStockAnalyzer a:analyzers) {
            sb.append(a.getDescription()).append("\n");
        }
        ar.setDescription(sb.toString());
        ar.setGenerateTime(LocalDateTime.now().toString());
        return ar;
    }

    public void addAnalyzer(IStockAnalyzer analyzer) {
        analyzers.add(analyzer);
    }

    public void startAnalyze(String klineType) {
        logger.info("start analyzing "+ klineType);
        startAnalyzeInSequence(klineType);
        // startAnalyzeInParallel();

        long now = System.currentTimeMillis();
        logger.info("ending analyzing "+ klineType);
    }
    public Stock testJson() {
        Stock stock = getStock("000001", "daily");
        return stock;
    }

    private void startAnalyzeInParallel() {
//		selectResultList = new CopyOnWriteArrayList<ResultInfo>();
//
//		ExecutorService es = Executors.newFixedThreadPool(15);
//
//		List<Stock> allSymbols = stockDAO.getAllSymbols();
//		for (Stock stock : allSymbols) {
//			Task task = new Task(stock.getCode(), selectResultList);
//			es.execute(task);
//		}
//		es.shutdown();
//		try {
//			es.awaitTermination(60, TimeUnit.MINUTES);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
    }

    public void startAnalyzeInSequence(String klineType) {
        selectResultList = new ArrayList<ResultInfo>();
        SymbolList allSymbols = getAllSymbols();
        for (Stock stock : allSymbols.getSymbols()) {
            stock = getStock(stock.getCode(),klineType);
            if (stock != null && !outOfDate(stock)) {
                ResultInfo resultInfo = analyze(stock);
                if (resultInfo != null) {
                    selectResultList.add(resultInfo);
                }
            }
        }
    }

    private boolean outOfDate(Stock stock) {
        List<KLineInfo> list=stock.getkLineInfos();
        if(list.isEmpty()){
            return true;
        }
        KLineInfo lastOne=stock.getkLineInfos().get(list.size()-1);
        return lastOne.getDate().plusDays(5).isBefore(LocalDate.now());
    }

    private ResultInfo analyze(Stock stock) {
        ResultInfo resultInfo = new ResultInfo();
        resultInfo.appendMessage(stock.getCode() + " "+ stock.getName());
        for (IStockAnalyzer analyzer : analyzers) {
            if (!analyzer.analyze(resultInfo, stock)) {
                resultInfo = null;
                return resultInfo;
            }
        }
        //get stock url
        //http://quotes.money.163.com/0601899.html
        resultInfo.setUrl(Utils.convert163StockURL(stock.getCode()));
        return resultInfo;
    }

    private Stock getStock(String symbol, String klineType) {
        //"timerCache is above the warning threshold of 1000 with size XXX"
        //https://my.oschina.net/u/2408085/blog/733900
//        这个告警主要是说创建的timer已经超过默认阈值1000了，可以通过增大配置netflix.metrics.servo.cacheWarningThreshold来解决
        // /data/kline/daily/{code}
        //-Djava.net.preferIPv4Stack=true
        Stock input=new Stock();
        input.setCode(symbol);
        input.setkLineType(klineType);

        try{
            Stock stock=restTemplate.postForObject("http://stock-microservice/data/kline/",input, Stock.class);
            if(stock==null||stock.getkLineInfos()==null){
                logger.warn(symbol+ " has no kline data");
                return null;
            }
            return stock;
        }catch (Exception e){
            logger.error(JSON.toJSONString(input));
            logger.error(e.getMessage(),e);

        }
        return null;
    }

    private SymbolList getAllSymbols() {
        SymbolList symbolList = restTemplate.getForObject("http://stock-microservice/stockList", SymbolList.class);
        return symbolList;
    }

    class Task implements Runnable {
        String code = null;
        List<ResultInfo> list = null;

        public Task(String code, List<ResultInfo> selectResultList) {
            this.code = code;
            this.list = selectResultList;
        }

        public void run() {
//			Stock stock = null;
//			stock = stockDAO.getStock(code);
//			if (stock != null) {
//				ResultInfo resultInfo = analyze(stock);
//				if (resultInfo != null) {
//					selectResultList.add(resultInfo);
//				}
//			}
        }
    }
}
