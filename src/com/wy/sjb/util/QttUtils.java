package com.wy.sjb.util;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.mysql.jdbc.CallableStatement;
import com.mysql.jdbc.Connection;

/**
 * @author wangyuan
 * @date 2018年8月23日
 */
public class QttUtils {
	
	public final Logger logger = Logger.getLogger("qtt");
	
	public static void main(String[] args) {
		try {
			final QttUtils utils = new QttUtils();
			CountDownLatch latch = new CountDownLatch(1);
			new Thread(new Runnable() {
				public void run() {
					try {
						utils.qdata();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
			
			utils.task();
			latch.await();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void task() {
		try {
			timer1 = new Timer(true);
			timer2 = new Timer(true);
			timer3 = new Timer(true);
			timer1.scheduleAtFixedRate(myTimer, new Date(), 1000 * 60 * 60l);//每小时一次。
			timer2.scheduleAtFixedRate(task2, new Date(), 1000 * 60 * 60 * 24l);//每天执行一次。
			timer3.scheduleAtFixedRate(task3, new Date(), 1000 * 60 * 60 * 4l);//每四个小时执行一次。
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** 定时获取整点任务奖励 */
	private Timer timer1 = null;
	private Timer timer2 = null;
	private Timer timer3 = null;
	private MyTimer myTimer = new MyTimer();
	private TimerTask task2 = new TimerTask() {
		final AtomicInteger ai = new AtomicInteger(0);
		@Override
		public void run() {
			try {
				Random random = new Random();
				Thread.sleep(random.nextInt(12000) * 1000);
				
				String url = "http://api.1sapp.com/act/presignin/activeSignInInfoV2?dtu=002&lon=121.62868&tk=ACHy-IUyohBL3YmxtLvXBnK7KoiPjer93BE0NzUxNDk1MDg5NTIyNQ&inviteId=&sign=06eaf616f4ba4385e0259bacc1a5c6a5&time=1535435173325&token=9216vIrK2VyawnKjvL0eckAqthNoJCVeuxzYkeXTZrKO13r4ZoPR8mml_Ulq_oIryH67Zn_WiFh9D1I4_Q&guid=b6613a4e306165b7e6d419604a0.20537940&xhi=200&uuid=4e6bfb7da43f447088195ac7f044d62a&os=android&versionName=3.2.2.000.0816.1558&OSVersion=7.1.2&network=wifi&distinct_id=f0f36cc35681b2&version=30202000&deviceCode=867709035870425&lat=31.205118";
				String respStr = HttpClientUtils.requestGet(url);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss SSS");
				logger.info(sdf.format(new Date()) + " 每天签到任务 " + respStr);
				JSONObject json = JSONObject.fromObject(respStr);
				if (json.containsKey("code") && json.getInt("code") != 0) {
					throw new Exception("每天签到任务 error");
				}
				ai.set(0);
			} catch (Exception e) {
				e.printStackTrace();
				if (ai.incrementAndGet() >= 2) {
					logger.error("task2 error count > 2");
					timer2.purge();
					timer2.cancel();
					task2.cancel();
				}
			}
		}
	};
	
	private TimerTask task3 = new TimerTask() {
		final AtomicInteger ai = new AtomicInteger(0);
		@Override
		public void run() {
			try {
//				Random random = new Random();
//				Thread.sleep(random.nextInt(600) * 1000);
				String url = "https://api.1sapp.com/mission/receiveTreasureBox?_=" + System.currentTimeMillis();
				Map<String, String> paramMap = new HashMap<String, String>();
				paramMap.put("token", "9216vIrK2VyawnKjvL0eckAqthNoJCVeuxzYkeXTZrKO13r4ZoPR8mml_Ulq_oIryH67Zn_WiFh9D1I4_Q");
				paramMap.put("dtu", "002");
				paramMap.put("xhi", "200");
				paramMap.put("version", "30202000");
				paramMap.put("os", "android");
				paramMap.put("tk", "IUyohBL3YmxtLvXBnK7KoiPjer93BE0NzUxNDk1MDg5NTIyNQ");
				paramMap.put("distinct_id", "f0f36cc35681b2");
				String respStr = HttpClientUtils.requestPost(url, paramMap);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss SSS");
				logger.info(sdf.format(new Date()) + " 宝箱任务奖励  " + respStr);
				JSONObject json = JSONObject.fromObject(respStr);
				if (json.containsKey("code") && json.getInt("code") != 0) {
					throw new Exception("宝箱任务奖励 error");
				}
				ai.set(0);
			} catch (Exception e) {
				e.printStackTrace();
				if (ai.incrementAndGet() >= 2) {
					System.out.println("宝箱任务奖励 error count >= 2");
					logger.error("task2 error count >= 2");
					timer3.purge();
					timer3.cancel();
					task2.cancel();
				}
			}
		}
	};
	
	class MyTimer extends TimerTask {
		final AtomicInteger ai = new AtomicInteger(0);
		
		@Override
		public void run() {
			try {
				Random random = new Random();
				Thread.sleep(random.nextInt(600) * 1000);
				String url = "https://api.1sapp.com/mission/intPointReward?dtu=002&lon=121.62868&tk=ACHy-IUyohBL3YmxtLvXBnK7KoiPjer93BE0NzUxNDk1MDg5NTIyNQ&sign=5d79998f52f4a868105819839bbf4104&time=1535368095962&guid=b6613a4e306165b7e6d419604a0.20537940&token=9216vIrK2VyawnKjvL0eckAqthNoJCVeuxzYkeXTZrKO13r4ZoPR8mml_Ulq_oIryH67Zn_WiFh9D1I4_Q&uuid=4e6bfb7da43f447088195ac7f044d62a&env=qukan_prod&versionName=3.2.2.000.0816.1558&OSVersion=7.1.2&network=wifi&distinct_id=f0f36cc35681b2&version=30202000&deviceCode=867709035870425&lat=31.205126";
				String respStr = HttpClientUtils.requestGet(url);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss SSS");
				logger.info(sdf.format(new Date()) + " 整点领取金币任务  " + respStr);
				JSONObject json = JSONObject.fromObject(respStr);
				if (json.containsKey("code") && json.getInt("code") != 0) {
					throw new Exception("整点领取金币任务 error");
				}
				ai.set(0);
			} catch (Exception e) {
				e.printStackTrace();
				if (ai.incrementAndGet() >= 2) {
					logger.error("error count > 2");
					timer1.purge();
					timer1.cancel();
					myTimer.cancel();
				}
			}
		}
	}
	
	@Test
	public void qdata() throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss SSS");
		
		String url = "https://api.1sapp.com/readtimer/report?qdata=";
		String qdata = "";
		int error = 0;
		while(true) {
			try {
				qdata = queryMysql("SELECT qdata from qtt_qdata where status = 0 ORDER BY RAND() LIMIT 1");
				String respStr = HttpClientUtils.requestGet(url + qdata);
				JSONObject json = JSONObject.fromObject(respStr);
				logger.info(respStr);
				JSONObject data = json.getJSONObject("data");
				if (json.containsKey("code") && json.getInt("code") != 0) {
					throw new Exception("code != 0");
				}
				int amount = data.getJSONObject("curr_task").getInt("amount");
				int nextAmount = data.getJSONObject("next_task").getInt("amount");
				int time = data.getJSONObject("next_task").getInt("time");
				
				logger.info(String.format("time[%s],amount[%s],next_amount[%s],qdata[%s]", sdf.format(new Date()),
						amount, nextAmount, qdata.substring(0, 20)));
				error = 0;
				Thread.sleep(time * 1000 + 50);
			} catch (Exception e) {
				e.printStackTrace();
				error ++;
				if (++error > 3) {
					System.out.println("============error=3,end==========");
					return;
				}
				Thread.sleep(30500);
			}
		}
	}

	public String datasourceUrl = "jdbc:mysql://localhost:3306/test?characterEncoding=utf8&amp;noAccessToProcedureBodies=true&amp;autoReconnect=true";
	private String queryMysql(String sql) {
		ResultSet rs = null;
		CallableStatement cstmt = null;
		Connection con = null;
		String qdata = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = (Connection) DriverManager.getConnection(datasourceUrl, "root", "123456");
			cstmt = (CallableStatement) con
					.prepareCall(sql);
			rs = cstmt.executeQuery();
			while (rs.next()) {
				qdata = rs.getString("qdata");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(con,rs, cstmt);
		}
		return qdata;
	}

	private void close(Connection conn,ResultSet rs, CallableStatement cstmt) {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (cstmt != null) {
			try {
				cstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

}
