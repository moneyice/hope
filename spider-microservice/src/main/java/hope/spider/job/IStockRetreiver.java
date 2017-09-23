package hope.spider.job;


import hope.spider.model.Stock;

import java.io.IOException;
import java.util.List;

public interface IStockRetreiver {
    List<Stock> getAllStockSymbols() throws IOException;

    Stock getStockInfo(Stock stock) throws IOException;
}