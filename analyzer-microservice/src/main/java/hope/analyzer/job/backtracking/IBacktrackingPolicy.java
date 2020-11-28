package hope.analyzer.job.backtracking;

import hope.analyzer.model.ETradeType;
import hope.analyzer.model.KLineInfo;

import java.util.List;

public interface IBacktrackingPolicy {
    ETradeType check(int index, List<KLineInfo> kLineInfos);
}
