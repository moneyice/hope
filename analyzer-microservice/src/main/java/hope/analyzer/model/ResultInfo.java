package hope.analyzer.model;

import org.springframework.util.StringUtils;

public class ResultInfo {
    //Stock stock;

    String msg = "";

//    public Stock getStock() {
//        return stock;
//    }
//
//    public void setStock(Stock stock) {
//        this.stock = stock;
//    }

    public String getMsg() {
        return msg;
    }

    public void appendMessage(String msg) {
        if (StringUtils.isEmpty(this.msg)) {
            this.msg = msg;
        } else {
            this.msg = this.msg + "|" + msg;
        }
    }
}
