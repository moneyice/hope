package hope.analyzer.controller;

import com.alibaba.fastjson.JSON;
import hope.analyzer.analyzer.EStockAnalyzer;
import hope.analyzer.analyzer.IStockAnalyzer;
import hope.analyzer.analyzer.StockAnalyzerFacotry;
import hope.analyzer.model.AnalyzeResult;
import hope.analyzer.model.ResultInfo;
import hope.analyzer.model.Stock;
import hope.analyzer.model.SymbolList;
import hope.analyzer.service.AliyunOSSStorageService;
import hope.analyzer.service.StockSelectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
public class AnalysisController {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AliyunOSSStorageService aliyunOSSStorageService;

    public AnalysisController() {
    }

    @Async
    @RequestMapping(value = "/startAnalyze", method = RequestMethod.GET)
    public void startAnalyzeInBackgroud() {
        for (EStockAnalyzer enumAnalyzer:EStockAnalyzer.values()){
            IStockAnalyzer analyzer = StockAnalyzerFacotry.createStockAnalyzer(enumAnalyzer);
            StockSelectService hs = new StockSelectService(restTemplate);
            hs.addAnalyzer(analyzer);
            hs.startAnalyze("dailylite");
            AnalyzeResult result = hs.getAnalyzeResult();
            storeAnalysisResult(result,enumAnalyzer,"daily");
        }

        {
        IStockAnalyzer analyzer = StockAnalyzerFacotry.createStockAnalyzer(EStockAnalyzer.MACD);
        StockSelectService hs = new StockSelectService(restTemplate);
        hs.addAnalyzer(analyzer);
        hs.startAnalyze("weekly");
        AnalyzeResult result = hs.getAnalyzeResult();
        storeAnalysisResult(result,EStockAnalyzer.MACD,"weekly");

        analyzer = StockAnalyzerFacotry.createStockAnalyzer(EStockAnalyzer.MACD);
        hs = new StockSelectService(restTemplate);
        hs.addAnalyzer(analyzer);
        hs.startAnalyze("monthly");
        result = hs.getAnalyzeResult();
        storeAnalysisResult(result,EStockAnalyzer.MACD,"monthly");
        }
    }

    private void storeAnalysisResult(AnalyzeResult result, EStockAnalyzer enumAnalyzer, String type) {
        String filename=enumAnalyzer.name()+ "-"+type;
        String content= JSON.toJSONString(result);
        aliyunOSSStorageService.put(filename,content );
        logger.info("aliyunOSS storage successful "+ filename);
        filename=LocalDate.now().toString()+"-"+filename;
        aliyunOSSStorageService.put(filename,content );
        logger.info("aliyunOSS storage successful "+ filename);
    }

    @RequestMapping(value = "/analysis/{analyzerName}/daily", method = RequestMethod.GET)
    public String analyze(@PathVariable String analyzerName) {
        String filename=analyzerName+ "-daily";
        String s=aliyunOSSStorageService.get(filename);
        return s;
    }

    @RequestMapping(value = "/analysis/{analyzerName}/weekly", method = RequestMethod.GET)
    public String analyzeByWeekly(@PathVariable String analyzerName) {
        String filename=analyzerName+ "-weekly";
        String s=aliyunOSSStorageService.get(filename);
        return s;
    }

    @RequestMapping(value = "/analysis/{analyzerName}/monthly", method = RequestMethod.GET)
    public String analyzeByMonthly(@PathVariable String analyzerName) {
        String filename=analyzerName+ "-monthly";
        String s=aliyunOSSStorageService.get(filename);
        return s;
    }

    @RequestMapping(value = "/test_json", method = RequestMethod.GET)
    public Stock testJson() {
        StockSelectService hs = new StockSelectService(restTemplate);
        Stock stock = hs.testJson();
        return stock;
    }

    @RequestMapping(value = "/test_large_json", method = RequestMethod.GET)
    public void testLargeJson() {
        SymbolList allSymbols = getAllSymbols();
        String type="dailylite";
        int i=0;
//        for (Stock stock:allSymbols.getSymbols()) {
//            String code=stock.getCode();
////            code="300319";
//            StringBuilder sb=new StringBuilder();
//            sb.append("http://stock-microservice/data/kline/").append(type).append("/").append(code);
//            stock=restTemplate.getForObject(sb.toString(), Stock.class);
////            stock=restTemplate.getForObject("http://stock-microservice/data/kline/"+type+"/"+code, Stock.class);
//            logger.info( i+" --"+stock.getCode());
//            i++;
//        }
         i=0;
        for (Stock stock:allSymbols.getSymbols()) {
            String code=stock.getCode();
//            code="300319";
            StringBuilder sb=new StringBuilder();
            stock.setkLineType("dailylite");
            sb.append("http://stock-microservice/data/kline/").append(type).append("/").append(code);
            stock=restTemplate.postForObject("http://stock-microservice/data/kline/", stock,Stock.class);
//            stock=restTemplate.getForObject("http://stock-microservice/data/kline/"+type+"/"+code, Stock.class);
            logger.info( i+" --"+stock.getCode());
            i++;
        }
    }
    private SymbolList getAllSymbols() {
        SymbolList symbolList = restTemplate.getForObject("http://stock-microservice/stockList", SymbolList.class);
        return symbolList;
    }
}
