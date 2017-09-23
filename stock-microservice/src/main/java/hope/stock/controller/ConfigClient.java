package hope.stock.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * 这边的@RefreshScope注解不能少，否则即使调用/refresh，配置也不会刷新
 *
 * @author eacdy
 */
@RefreshScope
@Component
@Service
public class ConfigClient {
    //@Value("${local.data.cache.folder}")
    private String stockRepositoryPath="/home/working/temp/stocks/";

    public String getStockRepositoryPath() {
        return this.stockRepositoryPath;
    }
}