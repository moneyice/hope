package hope.spider.job;


import hope.spider.model.Stock;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public abstract class WebStockRetreiver implements IStockRetreiver {
    private Logger logger = LoggerFactory.getLogger(getClass());
    String codeListHTML = "http://quote.eastmoney.com/stock_list.html";

    @Override
    public List<Stock> getAllStockSymbols() throws IOException {
        List<Stock> list = new ArrayList<Stock>();
        Document doc = Jsoup.connect(codeListHTML).get();
        Elements codeList = doc.select("#quotesearch ul li a");
        StockFilter sf = new StockFilter();
        for (Element element : codeList) {
            String text = element.text();
            String linkHref = element.attr("href"); // http://quote.eastmoney.com/sz300409.html
            String market = linkHref.substring(27, 29).toUpperCase();
            String symbol = linkHref.substring(29, 35);
            String name = text.substring(0, text.length() - 8);
            logger.info(market + "  " + symbol + "  " + name);
            if (sf.select(symbol)) {
                Stock stock = new Stock();
                stock.setName(name);
                stock.setCode(symbol);
                stock.setMarket(market);
                list.add(stock);
            }
        }
        return list;
    }
}
