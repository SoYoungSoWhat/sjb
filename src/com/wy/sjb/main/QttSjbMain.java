package com.wy.sjb.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.wy.sjb.config.PropertiesConfig;
import com.wy.sjb.util.HttpClientUtils;
import com.wy.sjb.util.StringUtils;

/**
 * 趣头条刷金币。
 * 
 * @author wangyuan
 * @date 2018年8月30日
 */
public class QttSjbMain {

	public final static Logger info = Logger.getLogger("qttinfo");
	public final static Logger error = Logger.getLogger("qtterror");
	public final static Logger qdataLogger = Logger.getLogger("qttqdata");
	public final static Logger signLogger = Logger.getLogger("sign");// 每日签到奖励。
	public final static Logger boxLogger = Logger.getLogger("box");// 宝箱任务奖励。
	public final static Logger pointrewardLogger = Logger
			.getLogger("pointreward");// 整点奖励。

	public static final CountDownLatch LATCH = new CountDownLatch(4);
	public List<User> USERS = new ArrayList<QttSjbMain.User>();

	private final static String url = "https://api.1sapp.com/readtimer/report?qdata=";
	private final Map<Integer, Integer> errorMap = new HashMap<Integer, Integer>();
	private final Set<String> errorQdataSet = new HashSet<String>();
	//记录处理每个用户调用接口的耗时。
	final Map<Integer, Long> qdataTimeMap = new HashMap<Integer, Long>();

	private static final int THREAD_POOL_SIZE = 20;
	public static ExecutorService service = Executors
			.newFixedThreadPool(THREAD_POOL_SIZE);

	public static void main(String[] args) {
		try {
			info.info("main start...");

			QttSjbMain main = new QttSjbMain();
			main.qdata();
			main.task();

			LATCH.await();
		} catch (Exception e) {
			e.printStackTrace();
			error.error("main error:", e);
		} finally {
			info.info("main exit...");
		}
	}

	private QttSjbMain() throws FileNotFoundException, IOException {
		init();
	}

	/** 初始化用户信息配置。 */
	public void init() throws FileNotFoundException, IOException {
		Properties prop = PropertiesConfig.getInstance().getProp();
		Integer userCount = Integer.valueOf(prop.getProperty("user.count"));
		List<User> users = new ArrayList<QttSjbMain.User>();
		for (int i = 1; i <= userCount; i++) {
			User user = new User();
			user.id = Integer.valueOf(prop.getProperty(String.format(
					"user.id.%s", i)));
			String qdatas = prop.getProperty(String.format("user.qdata.%s", i));
			if (StringUtils.checkStr(qdatas)) {
				user.qdata = Arrays.asList(qdatas.split(","));
				for (String qdata : errorQdataSet) {
					user.qdata.remove(qdata);
				}
			}
			user.token = prop.getProperty(String.format("user.token.%s", i));
			user.tk = prop.getProperty(String.format("user.tk.%s", i));
			user.sign = prop.getProperty(String.format("user.sign.%s", i));
			users.add(user);
		}
		USERS = users;

	}

	protected class User {
		int id;
		List<String> qdata = new ArrayList<String>();
		String token;
		String tk;
		String sign;
	}

	/** 定时获取整点任务奖励 */
	public Timer timer1 = null;
	public Timer timer2 = null;
	public Timer timer3 = null;
	public Timer timer4 = null;

	@Test
	public void task() {
		try {
			timer1 = new Timer(true);
			timer2 = new Timer(true);
			timer3 = new Timer(true);
			timer4 = new Timer(true);
			Date date = new Date();
			timer1.scheduleAtFixedRate(task1, date, 1000 * 60 * 60l);// 每小时一次。
//			timer2.scheduleAtFixedRate(task2, date, 1000 * 60 * 60 * 24l);// 每天执行一次。
			timer3.scheduleAtFixedRate(task3, date, 1000 * 60 * 60 * 4l);// 每四个小时执行一次。
			timer4.scheduleAtFixedRate(task0, date, 1000 * 60 * 30);// 每三十分钟执行一次，重新加载properties。
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void qdata() throws Exception {

		service.execute(new Runnable() {

			@Override
			public void run() {
				while (true) {
					
					for (final User user : USERS) {
						if (!errorMap.containsKey(user.id)) {
							errorMap.put(user.id, 0);
						}

						service.execute(new Runnable() {
							@Override
							public void run() {
								dealQdata(user);
							}
						});
					}

					boolean flag = true;
					for (Integer count : errorMap.values()) {
						if (count < 3) {
							flag = false;
							break;
						}
					}

					// 如果errorMap错误次数都大于等于3，则该任务关闭。
					if (flag) {
						LATCH.countDown();
						info.error("errorMap错误次数都大于等于3，则该阅读新闻奖励关闭");
						break;
					}

					try {
						Thread.sleep(30 * 1000 + 50);
					} catch (Exception e) {
						error.error("qdata sleep error:", e);
					}
				}

			}
		});

	}

	private TimerTask task0 = new TimerTask() {

		@Override
		public void run() {
			try {
				info.info("reLoadProp start...");
				PropertiesConfig.getInstance().reLoadProp();
				init();
				info.info("reLoadProp start...");
			} catch (Exception e) {
				error.error("重新加载properties异常：", e);
			}
		}
	};

	private MyTask task1 = new MyTask() {

		@Override
		public String getTaskName() {
			return "整点领取金币任务";
		}

		@Override
		public Logger getTaskLogger() {
			return pointrewardLogger;
		}

		@Override
		public String doAction(User user) throws Exception {
			if (!checkUser(user)) {
				throw new Exception(getTaskName() + " 必传字段为空！！！");
			}

			Random random = new Random();
			long time = random.nextInt(600) * 1000;
			getTaskLogger().info("休眠时间：" + time);
//			Thread.sleep(time);
			
			String url = "https://api.1sapp.com/mission/intPointReward?dtu=002&lon=121.62868&tk="
					+ user.tk
					+ "&sign=" + user.sign
					+ "&time=1535630552025&guid=b6613a4e306165b7e6d419604a0.20537940&token="
					+ user.token
					+ "&uuid=4e6bfb7da43f447088195ac7f044d62a&env=qukan_prod&versionName=3.2.2.000.0816.1558"
					+ "&OSVersion=7.1.2&network=wifi&distinct_id=f0f36cc35681b2&version=30202000&deviceCode="
					+ "867709035870425&lat=31.205126";
			String respStr = HttpClientUtils.requestGet(url);
			return respStr;
		}
	};

	public MyTask task2 = new MyTask() {

		@Override
		public String getTaskName() {
			return "每天签到任务";
		}

		@Override
		public Logger getTaskLogger() {
			return signLogger;
		}

		@Override
		public String doAction(User user) throws Exception {
			if (!checkUser(user)) {
				throw new Exception(getTaskName() + " 必传字段为空！！！");
			}

			// 随机间隔一段时间，避免刷接口太明显。
			Random random = new Random();
			long time = random.nextInt(12000) * 1000;
			getTaskLogger().info("休眠时间：" + time);
			Thread.sleep(time);
			String url = "http://api.1sapp.com/act/presignin/activeSignInInfoV2?dtu=002&lon=121.62868&tk="
					+ user.tk
					+ "&inviteId=&sign=06eaf616f4ba4385e0259bacc1a5c6a5&time=1535435173325&token="
					+ user.token
					+ "&guid=b6613a4e306165b7e6d419604a0.20537940&xhi=200&uuid=4e6bfb7da43f447088195ac7f044d62a&os=android&versionName=3.2.2.000.0816.1558"
					+ "&OSVersion=7.1.2&network=wifi&distinct_id=f0f36cc35681b2&version=30202000&deviceCode=867709035870425&lat=31.205118";
			String respStr = HttpClientUtils.requestGet(url);
			return respStr;
		}
	};

	private TimerTask task3 = new MyTask() {

		@Override
		public String getTaskName() {
			return "宝箱任务奖励";
		}

		@Override
		public Logger getTaskLogger() {
			return boxLogger;
		}

		@Override
		public String doAction(User user) throws Exception {
			String url = "https://api.1sapp.com/mission/receiveTreasureBox?_="
					+ System.currentTimeMillis();
			Map<String, String> paramMap = new HashMap<String, String>();
			if (!checkUser(user)) {
				throw new Exception(getTaskName() + " 必传字段为空！！！");
			}
			paramMap.put("token", user.token);
			paramMap.put("dtu", "002");
			paramMap.put("xhi", "200");
			paramMap.put("version", "30202000");
			paramMap.put("os", "android");
			paramMap.put("tk", user.tk);
			paramMap.put("distinct_id", "f0f36cc35681b2");
			String respStr = HttpClientUtils.requestPost(url, paramMap);
			return respStr;
		}
	};

	/** 处理每一条qdata，返回接口调用耗时。 */
	private void dealQdata(User user) {
		String qdata = "";
		try {
			int index;
			if (user.qdata != null && user.qdata.size() > 0) {
				Random random = new Random();
				index = random.nextInt(user.qdata.size());
				qdata = user.qdata.get(index);
				if (!StringUtils.checkStr(qdata)) {
					return;
				}
			} else {
				return;
			}
			
			//根据上次接口调用的耗时，进行休眠。
			if (qdataTimeMap.containsKey(user.id)) {
				sleep(qdataTimeMap.get(user.id) - System.currentTimeMillis());
			}
			
			String respStr = HttpClientUtils.requestGet(url + qdata);
			qdataTimeMap.put(user.id, System.currentTimeMillis() + 30010);
			
			JSONObject json = JSONObject.fromObject(respStr);
			qdataLogger.info(String.format("userid[%s],respStr[%s]", user.id, respStr));
			JSONObject data = json.getJSONObject("data");

			// 如果qdata过期，则必须要从配置文件内移除。
			if (json.containsKey("code") && json.getInt("code") != 0) {
				throw new Exception("code != 0");
			}
			int amount = data.getJSONObject("curr_task").getInt("amount");
			int nextAmount = data.getJSONObject("next_task").getInt("amount");

			String info = String.format(
					"id[%s],amount[%s],next_amount[%s],qdata[%s]", user.id,
					amount, nextAmount, qdata.substring(0, 20));
			qdataLogger.info(info);
			errorMap.put(user.id, 0);

		} catch (Exception e) {
			e.printStackTrace();
			errorMap.put(user.id, errorMap.get(user.id) + 1);

			if (errorMap.get(user.id) >= 5) {
				info.info("============新闻阅读奖励 error=4,end==========");

				// 删除掉qdata。
				error.error("无效qdata:" + qdata);
				errorQdataSet.add(qdata);
				user.qdata.remove(qdata);
			}
		}
		
	}

	/** 自定义定时任务。 */
	interface IMyTask {
		List<User> getUsers();

		String getTaskName();

		Integer getErrorCount();

		Logger getTaskLogger();

		boolean checkUser(User user);

		String doAction(User user) throws Exception;
	}

	abstract class MyTask extends TimerTask implements IMyTask {
		final Map<Integer, Integer> countMap = new HashMap<Integer, Integer>();
		private final int POOL_SIZE = 5;
		public final ExecutorService taskService = Executors
				.newFixedThreadPool(POOL_SIZE);

		public void run() {
			List<User> users = getUsers();

			if (users == null || users.size() == 0) {
				LATCH.countDown();
				info.error(getTaskName() + "已结束...");
				return;
			}

			for (final User user : users) {
				if (!countMap.containsKey(user.id)) {
					countMap.put(user.id, 0);
				}

				taskService.execute(new Runnable() {

					@Override
					public void run() {
						if (countMap.get(user.id) >= getErrorCount()) {
							return;
						}

						try {
							if (!checkUser(user)) {
								throw new Exception(getTaskName()
										+ " 必传字段为空！！！");
							}

							String respStr = doAction(user);
							getTaskLogger()
									.info("用户id=" + user.id + "," + getTaskName() + "\t" + respStr);
							JSONObject json = JSONObject.fromObject(respStr);
							if (json.containsKey("code")
									&& json.getInt("code") != 0) {
								error.error(getTaskName() + " 异常,用户id=" + user.id);
								throw new Exception(getTaskName() + " error");
							}

							// 执行成功，则将失败次数置为0.
							countMap.put(user.id, 0);
						} catch (Exception e) {
							// 失败时，则记录错误次数加1.
							countMap.put(user.id, countMap.get(user.id) + 1);
							error.error(getTaskName() + " 异常 count=" + countMap.get(user.id) + ",用户id=" + countMap.get(user.id), e);
							if (countMap.get(user.id) >= 2) {
								error.error(getTaskName() + " 异常 count=" + countMap.get(user.id) + ",用户id=" + countMap.get(user.id), e);
							}
						}
					}
				});
			}

			boolean flag = true;
			for (Integer count : countMap.values()) {
				if (count < 2) {
					flag = false;
					break;
				}
			}

			if (flag) {
				LATCH.countDown();
				error.error(getTaskName() + "  停止！！！");
			}
		}

		@Override
		public Integer getErrorCount() {
			return 3;
		}

		@Override
		public List<User> getUsers() {
			return USERS;
		}

		@Override
		public boolean checkUser(User user) {
			if (StringUtils.checkStr(user.token, user.tk)) {
				return true;
			} else {
				return false;
			}
		}

	}
	
	public void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (Exception e) {
			error.error("sleep error:", e);
		}
	}

}
