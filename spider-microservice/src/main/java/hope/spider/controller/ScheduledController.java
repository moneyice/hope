package hope.spider.controller;

import hope.spider.job.StockInfoSpider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class ScheduledController {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource(name = "stockInfoSpider")
    private StockInfoSpider stockInfoSpider = null;
    @Autowired
    private DiscoveryClient discoveryClient;

    @Scheduled(initialDelay=1000*60,fixedDelay = 1000*60*60*5)
    //每5个小时
    public void retrieveStockDailyData() {
        logger.info("scheduled to retrieve stock daily data");
        stockInfoSpider.run();
    }

    @GetMapping("/ribbon")
    public String findById() {
        return stockInfoSpider.findById();
    }

    @GetMapping("/lastUpdateTime")
    public String getLastUpdateTime() {
        Date date = stockInfoSpider.getLastUpdateTime();
        return date.toString();
    }

    /**
     * 本地服务实例的信息
     *
     * @return
     */
    @GetMapping("/instance-info")
    public ServiceInstance showInfo() {
        ServiceInstance localServiceInstance = this.discoveryClient.getLocalServiceInstance();
        return localServiceInstance;
    }

    @RequestMapping("/force_refresh_stocks")
    @Async
    public void forceRefreshStockInfo() {
        stockInfoSpider.setCheckOutOfDate(false);
        stockInfoSpider.run();
        stockInfoSpider.setCheckOutOfDate(true);
    }
}
