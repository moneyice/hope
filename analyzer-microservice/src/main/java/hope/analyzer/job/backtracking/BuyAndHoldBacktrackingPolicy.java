package hope.analyzer.job.backtracking;

import hope.analyzer.model.ETradeType;
import hope.analyzer.model.KLineInfo;

import java.util.List;

/**
 * 买入并一直持有
 */
public class BuyAndHoldBacktrackingPolicy implements IBacktrackingPolicy{

    boolean ifBought=false;
    @Override
    public ETradeType check(int index, List<KLineInfo> kLineInfos) {
        if(ifBought){
            return ETradeType.NoAction;
        }else{
            ifBought=true;
            return ETradeType.Buy;
        }
    }
}
