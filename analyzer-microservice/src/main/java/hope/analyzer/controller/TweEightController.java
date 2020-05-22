package hope.analyzer.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.io.Resources;
import hope.analyzer.job.*;
import hope.analyzer.job.policy28.BaseTwoEightJob;
import hope.analyzer.model.AccountSummary;
import hope.analyzer.model.Stock;
import hope.analyzer.model.ValueLog;
import hope.analyzer.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;

import static java.lang.System.out;

@RestController
public class TweEightController {
	static String historyURL = "http://quotes.money.163.com/service/chddata.html?code=#{symbol}&start=20140101&&fields=TOPEN;HIGH;LOW;TCLOSE;VOTURNOVER";
	static String currentURL = "http://api.money.126.net/data/feed/#{symbol},money.api";
	@Autowired
	private RestTemplate restTemplate;
	@RequestMapping("/28")
	public String analyze28(
			@RequestParam(value = "name", defaultValue = "World") String name) {
		String result = null;
		DecimalFormat df = new DecimalFormat("######0.00");
		try {
			// 沪深300
			double current300 = getCurrentIndex("0000300");
			double history300 = getIndexOf4WeeksAgo("0000300"); 	

			// 中证500
			double current500 = getCurrentIndex("0000905");
			double history500 = getIndexOf4WeeksAgo("0000905");

			result = "300" + "     "
					+ df.format((current300 / history300 - 1) * 100) + "%";
			result = result + "-------------------------";
			result = result + "500" + "      "
					+ df.format((current500 / history500 - 1) * 100) + "%";

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("163 process error ");
		}
		return result;
	}

	@RequestMapping("/28/{version}")
	public AccountSummary trace28(
			@PathVariable String version,  @RequestParam String startDate) {
		LocalDate fromDate=null;
		try{
			fromDate=LocalDate.parse(startDate);
		}catch (Exception e){
			//300 index starts from 2002 01
			//500 index starts from here
			fromDate=LocalDate.parse("2005-02-04");
		}

		BaseTwoEightJob job= JobFacotry.getTwoEightJob(version);
		job.setFromDate(fromDate);

		Stock stock=new Stock();
		stock.setCode("i000300");
		stock=getStock(stock);
		job.setIndex300List(stock.getkLineInfos());

		stock=new Stock();
		stock.setCode("i000905");
		stock=getStock(stock);
		job.setIndex500List(stock.getkLineInfos());

		job.run();

		return job.accountSummary();
	}


	@RequestMapping("/28/compare")
	public String compare(@RequestParam String startDate) {
		LocalDate fromDate=null;
		try{
			fromDate=LocalDate.parse(startDate);
		}catch (Exception e){
			//300 index starts from 2002 01
			//500 index starts from here
			fromDate=LocalDate.parse("2005-02-04");
		}

		Stock index300=new Stock();
		index300.setCode("i000300");
		index300=getStock(index300);

		Stock index500=new Stock();
		index500.setCode("i000905");
		index500=getStock(index500);


		List<ValueLog> jobR1ValueLogList = getValueLogs("r1",fromDate, index300, index500);
		List<ValueLog> jobR2ValueLogList=getValueLogs("r2",fromDate, index300, index500);
		List<ValueLog> jobBase300ValueLogList=getValueLogs("base300",fromDate, index300, index500);
		List<ValueLog> jobBase500ValueLogList=getValueLogs("base500",fromDate, index300, index500);
		List<ValueLog> job300ValueLogList=getValueLogs("300",fromDate, index300, index500);
		List<ValueLog> job500ValueLogList=getValueLogs("500",fromDate, index300, index500);







		List<String> categoryList=new LinkedList<>();
		List<Double> r1DataList=new LinkedList<>();
		List<Double> r2DataList=new LinkedList<>();
		List<Double> base300DataList=new LinkedList<>();
		List<Double> base500DataList=new LinkedList<>();
		List<Double> index300DataList=new LinkedList<>();
		List<Double> index500DataList=new LinkedList<>();

		//按月分组显示
		int monthIndex=jobR1ValueLogList.get(0).getDate().getMonthValue();
		for (int i=1;i<jobR1ValueLogList.size();i++){
			LocalDate date=jobR1ValueLogList.get(i).getDate();
			int monthValue=date.getMonthValue();
			if(monthIndex!=monthValue){
				ValueLog wanted=jobR1ValueLogList.get(i-1);
				categoryList.add(getCategoryLabel(wanted));
				r1DataList.add(wanted.getTotalValue());
				r2DataList.add(jobR2ValueLogList.get(i-1).getTotalValue());
				base300DataList.add(jobBase300ValueLogList.get(i-1).getTotalValue());
				base500DataList.add(jobBase500ValueLogList.get(i-1).getTotalValue());
				index300DataList.add(job300ValueLogList.get(i-1).getTotalValue());
				index500DataList.add(job500ValueLogList.get(i-1).getTotalValue());
				monthIndex = monthValue;
			}
		}

		Map<String,List> map=new HashMap<String,List>();
        map.put("category",categoryList);
        map.put("r1",r1DataList);
        map.put("r2",r2DataList);
		map.put("base300",base300DataList);
		map.put("base500",base500DataList);
		map.put("index300",index300DataList);
		map.put("index500",index500DataList);

		return JSON.toJSONString(map);
	}

	private List<ValueLog> getValueLogs(String jobVersion,LocalDate fromDate, Stock index300, Stock index500) {
		BaseTwoEightJob jobR1= JobFacotry.getTwoEightJob(jobVersion);
		jobR1.setFromDate(fromDate);
		jobR1.setIndex300List(index300.getkLineInfos());
		jobR1.setIndex500List(index500.getkLineInfos());
		jobR1.run();
		return jobR1.accountSummary().getValueLogList();
	}

	private String getCategoryLabel(ValueLog wanted){
		return wanted.getDate().getYear()+"-"+wanted.getDate().getMonthValue();
	}

	private Stock getStock(Stock stock) {
		//"timerCache is above the warning threshold of 1000 with size XXX"
		//https://my.oschina.net/u/2408085/blog/733900
//        这个告警主要是说创建的timer已经超过默认阈值1000了，可以通过增大配置netflix.metrics.servo.cacheWarningThreshold来解决
		// /data/kline/daily/{code}
		//-Djava.net.preferIPv4Stack=true
		try{
			stock=restTemplate.getForObject("http://stock-microservice/data/kline/weekly/"+stock.getCode(),Stock.class);
			if(stock==null||stock.getkLineInfos()==null){
				return null;
			}
			return stock;
		}catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}




	private double getCurrentIndex(String symbol) throws IOException {
		String url = currentURL.replace("#{symbol}", symbol);
		String json = Resources
				.toString(new URL(url), Charset.forName("UTF-8"));
		json = json.replaceAll("_ntes_quote_callback\\(", "").replaceAll(
				"\\);", "");

		out.println(json);
		JSONObject jsonObj = com.alibaba.fastjson.JSON.parseObject(json);
		jsonObj = jsonObj.getJSONObject(symbol);

		out.println(jsonObj);
		double index = jsonObj.getDouble("price");
		return index;
	}

	private double getIndexOf4WeeksAgo(String symbol) throws IOException {
		String url = historyURL.replace("#{symbol}", symbol);
		List<String> list = Resources.readLines(new URL(url),
				Charset.forName("UTF-8"));

		String info = list.get(20);
		String[] result = info.split(",");
		double close = Utils.handleDouble(result[3]);
		return close;
	}

	@RequestMapping("/testException")
	public String exception() throws Exception {
		throw new Exception("发生错误");
	}
	@RequestMapping("/testException1")
	public String exception1() {
		throw new IndexOutOfBoundsException("发生Runtime错误");
	}
}
