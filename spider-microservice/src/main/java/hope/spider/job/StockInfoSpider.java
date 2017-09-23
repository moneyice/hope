package hope.spider.job;


import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hope.spider.model.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


@Component("stockInfoSpider")
public class StockInfoSpider {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private RestTemplate restTemplate;

    @Resource(name = "neteaseWebStockRetreiver")
    private IStockRetreiver stockRetreiver;
    private Date lastUpdateTime = null;
    // if it's needed to check the stock info is out of date
    // if true, check
    // if false, no need to check, will refresh all the data forcedly.
    // it's useless for getAllStockSymbols
    private boolean checkOutOfDate = true;

    public StockInfoSpider() {

    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public String findById() {
        return this.restTemplate.getForObject("http://stock-microservice/stock/000001?name=123", String.class);
    }

    public boolean isCheckOutOfDate() {
        return checkOutOfDate;
    }

    public void setCheckOutOfDate(boolean checkOutOfDate) {
        this.checkOutOfDate = checkOutOfDate;
    }

    public void run() {
        try {
            if (!isStockOutOfDate(lastUpdateTime)) {
                System.out.println("==============================不需要更新 " + new Date());
                return;
            }


            List<Stock> stockSymbols = stockRetreiver.getAllStockSymbols();
            storeAllSymbols(stockSymbols);

            logger.info("==============================需要更新 " + new Date());
            for (Stock stock : stockSymbols) {
                String code = stock.getCode();
                if (code.startsWith("0") || code.startsWith("3")
                        || code.startsWith("6")) {
                    String stockJsonString=JSON.toJSONString(stock);
                    Stock newCopy=JSON.parseObject(stockJsonString,Stock.class);
                    Stock info = stockRetreiver.getStockInfo(newCopy);
                    storeStock(info);
                }
            }
            lastUpdateTime = new Date();
            startAnalyze();
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
    }

    private void startAnalyze() {
        restTemplate.getForObject("http://analyzer-microservice/startAnalyze", Stock.class);
        logger.info("trigger start analyzing ");
    }

    private void storeStock(Stock info) {
        restTemplate.postForObject("http://stock-microservice/stock", info, Stock.class);
        logger.info("saved stock "+info.getCode());
    }

    private void storeAllSymbols(List<Stock> stockSymbols) {
        restTemplate.postForObject("http://stock-microservice/stockList", stockSymbols, List.class);
    }

    private boolean isAllSymbosOutOfDate(Date lastUpdateTime) {
        if (!isCheckOutOfDate()) {
            return true;
        }

        if (lastUpdateTime == null) {
            return true;
        }
        Calendar lastDay = Calendar.getInstance();
        lastDay.setTime(lastUpdateTime);
        // 超过10天没有更新过 算过期
        lastDay.add(Calendar.DAY_OF_YEAR, 10);
        Calendar today = Calendar.getInstance();
        return lastDay.before(today);
    }

    protected boolean isStockOutOfDate(Date lastUpdateTime) {
        return isStockOutOfDate(new Date(), lastUpdateTime);
    }

    protected boolean isStockOutOfDate(Date compareTime, Date lastUpdateTime) {

        if (!isCheckOutOfDate()) {
            return true;
        }
        // 每天16:00 以前，最新数据是昨天的， 每天16:00 以后，最新数据是今天的，就什么都不做。
        // 否则更新数据
        if (lastUpdateTime == null) {
            return true;
        }
        Calendar lastUpdateCalendar = Calendar.getInstance();
        lastUpdateCalendar.setTime(lastUpdateTime);

        Calendar nowTime = Calendar.getInstance();
        nowTime.setTime(compareTime);

        //基准时间为当天的16:00
        Calendar alignmentTime = Calendar.getInstance();
        alignmentTime.setTime(compareTime);
        alignmentTime.set(Calendar.HOUR_OF_DAY, 16);
        alignmentTime.set(Calendar.MINUTE, 0);
        alignmentTime.set(Calendar.SECOND, 0);
        alignmentTime.set(Calendar.MILLISECOND, 0);

        logger.info("上次修改时间" + lastUpdateCalendar.getTime());
        logger.info("当前时间" + nowTime.getTime());
        logger.info("基准时间" + alignmentTime.getTime());

        //如果当前时间过了基准时间，最后更新时间没有过基准时间，就说明过期了
        return isLater(nowTime, alignmentTime) && !isLater(lastUpdateCalendar, alignmentTime);
    }

    private boolean isLater(Calendar c1, Calendar c2) {
        return c1.compareTo(c2) > 0;
    }
}
