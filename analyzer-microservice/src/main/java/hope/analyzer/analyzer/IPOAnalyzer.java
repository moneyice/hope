package hope.analyzer.analyzer;

import hope.analyzer.model.KLineInfo;
import hope.analyzer.model.ResultInfo;
import hope.analyzer.model.Stock;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import static java.lang.System.out;


public class IPOAnalyzer extends AbstractStockAnalyzer {
    public final static long MONTH = 10;

    String descTemplate = "上市10个月以内，当前价格低于最高价的1/2";

    public IPOAnalyzer() {
    }

    public boolean analyze(ResultInfo resultInfo, Stock stock) {
        boolean ok = false;
        List<KLineInfo> infos = stock.getkLineInfos();
        if (infos.isEmpty()) {
            return ok;
        }
        LocalDate localDate = LocalDate.now();
        Period period = Period.between(infos.get(0).getDate(), localDate);

        if (period.getYears()*12+period.getMonths() >= MONTH ){
            return ok;
        }

        double currentPrice = infos.get(infos.size() - 1).getClose();

        for (int i = 0; i < infos.size(); i++) {
            KLineInfo toCheck = infos.get(i);
            if (toCheck.getClose() >= 2 * currentPrice) {
                ok = true;
                format(stock, toCheck, currentPrice);
                break;
            }
        }
        return ok;
    }

    private String format(Stock stock, KLineInfo check, double currentPrice) {
        StringBuilder sb = new StringBuilder();
        sb.append(stock.getCode()).append("  ").append(stock.getName())
                .append("\n");
        return (sb.toString());
    }

    public String getDescription() {
        return descTemplate;
    }
}
