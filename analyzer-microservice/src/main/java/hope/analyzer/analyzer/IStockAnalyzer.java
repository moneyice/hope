package hope.analyzer.analyzer;


import hope.analyzer.model.ResultInfo;
import hope.analyzer.model.Stock;

public interface IStockAnalyzer {


    String getDescription();


    void outPutResults();

    boolean analyze(ResultInfo resultInfo, Stock stock);
}
