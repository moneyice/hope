package hope.analyzer.job.fixedinvestiment;

import hope.analyzer.model.ETradeType;
import hope.analyzer.model.KLineInfo;

import java.util.List;

public class WeeklyPolicy extends BaseFixedInvestmentPolicy {


    @Override
    public ETradeType check(int index, List<KLineInfo> kLineInfos) {
        Integer dayOfWeek=kLineInfos.get(index).getDate().getDayOfWeek().getValue();
        if(getDaysSet().contains(dayOfWeek)){
            return  ETradeType.Buy;
        }
        return ETradeType.NoAction;
    }
}
