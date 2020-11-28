package hope.analyzer.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.io.Resources;
import hope.analyzer.job.JobFacotry;
import hope.analyzer.job.backtracking.BacktrackingJob;
import hope.analyzer.job.backtracking.BuyAndHoldBacktrackingPolicy;
import hope.analyzer.job.backtracking.MacdAndMean30BacktrackingPolicy;
import hope.analyzer.job.policy28.BaseTwoEightJob;
import hope.analyzer.model.AccountSummary;
import hope.analyzer.model.Stock;
import hope.analyzer.model.ValueLog;
import hope.analyzer.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.System.out;

@RestController
public class BacktrackingController {
	@Autowired
	private RestTemplate restTemplate;

	@RequestMapping("/backtracking/{code}")
	public String compare(@PathVariable String code ,@RequestParam String startDate) {
		LocalDate fromDate=null;
		try{
			fromDate=LocalDate.parse(startDate);
		}catch (Exception e){
			//300 index starts from 2002 01
			//500 index starts from here
			fromDate=LocalDate.parse("2005-02-04");
		}

		Stock stock=new Stock();
		stock.setCode(code);
		stock=getStock(stock);


		BacktrackingJob macdAndMean30PolicyJob= new BacktrackingJob(stock,new MacdAndMean30BacktrackingPolicy());
		macdAndMean30PolicyJob.setFromDate(fromDate);
		macdAndMean30PolicyJob.run();
		List<ValueLog> jobR1ValueLogList=macdAndMean30PolicyJob.getAccountSummary().getValueLogList();

		BacktrackingJob buyAndHoldPolicyJob= new BacktrackingJob(stock,new BuyAndHoldBacktrackingPolicy());
		buyAndHoldPolicyJob.setFromDate(fromDate);
		buyAndHoldPolicyJob.run();
		List<ValueLog> jobR2ValueLogList=buyAndHoldPolicyJob.getAccountSummary().getValueLogList();



		List<String> categoryList=new LinkedList<>();
		List<Double> r1DataList=new LinkedList<>();
		List<Double> r2DataList=new LinkedList<>();


		for (int i=0;i<jobR1ValueLogList.size();i++){
				ValueLog wanted=jobR1ValueLogList.get(i);
				categoryList.add(getCategoryLabel(wanted));
				r1DataList.add(wanted.getTotalValue());

				r2DataList.add(jobR2ValueLogList.get(i).getTotalValue());
		}

		Map<String,List> map=new HashMap<String,List>();
        map.put("category",categoryList);
        map.put("r1",r1DataList);
        map.put("r2",r2DataList);



		return JSON.toJSONString(map);
	}

	private String getCategoryLabel(ValueLog wanted){
		return wanted.getDate().getYear()+"-"+wanted.getDate().getMonthValue()+ "-"+wanted.getDate().getDayOfMonth();
	}

	private Stock getStock(Stock stock) {
		//"timerCache is above the warning threshold of 1000 with size XXX"
		//https://my.oschina.net/u/2408085/blog/733900
//        这个告警主要是说创建的timer已经超过默认阈值1000了，可以通过增大配置netflix.metrics.servo.cacheWarningThreshold来解决
		// /data/kline/daily/{code}
		//-Djava.net.preferIPv4Stack=true
		try{
			stock=restTemplate.getForObject("http://stock-microservice/data/kline/daily/"+stock.getCode(),Stock.class);
			if(stock==null||stock.getkLineInfos()==null){
				return null;
			}
			return stock;
		}catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}






}
