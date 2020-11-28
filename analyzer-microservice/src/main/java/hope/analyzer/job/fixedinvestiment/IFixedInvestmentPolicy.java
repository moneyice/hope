package hope.analyzer.job.fixedinvestiment;

import hope.analyzer.model.ETradeType;
import hope.analyzer.model.KLineInfo;

import java.util.List;

public interface IFixedInvestmentPolicy {
    ETradeType check(int index, List<KLineInfo> kLineInfos);

    void setDays(String days);

    //定投本金
    void setCapital(double capital);
    //定投金额
    void setFixedInvestimentValue(double fixedInvestimentValue);
}
