package hope.spider.job;

/**
 * Created by bing.a.qian on 12/9/2016.
 */
public class StockFilter {
    public boolean select(String code) {
        boolean result = false;
        if (code.startsWith("00") || code.startsWith("60") || code.startsWith("30")) {
            result = true;
        }
        return result;
    }
}
