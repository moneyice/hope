package hope.analyzer.job.fixedinvestiment;

import hope.analyzer.model.ETradeType;
import hope.analyzer.model.KLineInfo;

import java.util.List;

public class MonthlyPolicy extends BaseFixedInvestmentPolicy {
    @Override
    public ETradeType check(int index, List<KLineInfo> kLineInfos) {
        Integer dayOfMonth=kLineInfos.get(index).getDate().getDayOfMonth();
        if(getDaysSet().contains(dayOfMonth))
        {
           return  ETradeType.Buy;
        }
        return ETradeType.NoAction;
    }


}
