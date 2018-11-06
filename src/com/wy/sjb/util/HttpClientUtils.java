package com.wy.sjb.util;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HttpClientUtils {
	/**
	 * 连接超时时间
	 */
	public static final int CONNECTION_TIMEOUT_MS = 5000;

	/**
	 * 读取数据超时时间
	 */
	public static final int SO_TIMEOUT_MS = 5000;

	/**
	 * 连接请求超时
	 */
	public static final int CONNECTION_REQ_TIMEOUT_MS = 5000;
	
	public static void main(String[] args) throws Exception {
		String page = "pages/news/ndetails/ndetails?url=180528174642454&f=xcxmsg";
		String ip = "localhost:8080";
		String url = "http://"+ip+"/wxsa_sendMessage/sendMessage?formid=eb0ac1b7a4bfa76fb7f0fbd968d84658&openid="
				+ "o-oH60G5LK6FwcrXESqcjF5TYaLc&topic="
		+URLEncoder.encode(URLEncoder.encode("教你�?个简单动作治好脖子痛、腰痛�?�膝盖痛", "UTF-8"), "UTF-8")
				+"&time=" + URLEncoder.encode(URLEncoder.encode("2018-06-12 07:49:00", "UTF-8"), "UTF-8")
				+ "&desc=" + URLEncoder.encode(URLEncoder.encode("立即查看>>", "UTF-8"), "UTF-8")
				+ "&secret=b82bb7007b7244bf9ab4b9c5a63afb41&page="
				+ URLEncoder.encode(URLEncoder.encode(page, "UTF-8"), "UTF-8");
		System.out.println(url);
		String res = requestGet(url);
		System.out.println(res);
	}

	public static String requestGet(String url) throws Exception {
		CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultRequestConfig(buildRequestConfig()).build();
		HttpGet getRequest = new HttpGet(url);
		getRequest.setHeader("x-forwarded-for", "127.0.0.1");
		getRequest.setHeader("Proxy-Client-IP", "127.0.0.1");
		getRequest.setHeader("WL-Proxy-Client-IP", "127.0.0.1");
		getRequest.setConfig(buildRequestConfig());
		try {
			HttpResponse response = httpclient.execute(getRequest);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String returnStr = EntityUtils.toString(entity, "utf-8");
				return returnStr;
			}
		} finally {
			try {
				httpclient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return "";
	}

	public static String post(String url, String body) throws Exception {
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setSocketTimeout(3 * 1000).setConnectTimeout(3 * 1000)
				.setConnectionRequestTimeout(3 * 1000)
				.setStaleConnectionCheckEnabled(true).build();
		CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultRequestConfig(defaultRequestConfig).build();
		
		HttpPost httpPost = new HttpPost(url);// 创建httpPost
		httpPost.setHeader("Accept", "application/json");
		httpPost.setHeader("Content-Type", "application/json");
		String charSet = "UTF-8";
		StringEntity entity = new StringEntity(body, charSet);
		httpPost.setEntity(entity);
		CloseableHttpResponse response = null;

		try {
			response = httpclient.execute(httpPost);
			StatusLine status = response.getStatusLine();
			int state = status.getStatusCode();
			if (state == HttpStatus.SC_OK) {
				HttpEntity responseEntity = response.getEntity();
				String jsonString = EntityUtils.toString(responseEntity);
				return jsonString;
			}
		} finally {
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				httpclient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static String requestPost(String url, Map<String, String> _paramsmap)
			throws Exception {
		return requestPost(url, _paramsmap, null);
	}

	// post请求
	@SuppressWarnings("finally")
	public static String requestPost(String url, Map<String, String> _paramsmap, Map<String, String> headers)
			throws Exception {
		String content = "";
		// 客户请求超时配置
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setSocketTimeout(3 * 1000).setConnectTimeout(3 * 1000)
				.setConnectionRequestTimeout(3 * 1000)
				.setStaleConnectionCheckEnabled(true).build();
		// 创建默认的httpClient实例.
		CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultRequestConfig(defaultRequestConfig).build();
		// CloseableHttpClient httpclient = HttpClients.createDefault();
		// 创建httppost
		HttpPost httppost = new HttpPost(url);
		httppost.setHeader("x-forwarded-for", "127.0.0.1");
		httppost.setHeader("Proxy-Client-IP", "127.0.0.1");
		httppost.setHeader("WL-Proxy-Client-IP", "127.0.0.1");
		if (headers != null) {
			for(Entry<String, String> entry : headers.entrySet()) {
				httppost.setHeader(entry.getKey(), entry.getValue());
			}
		}
		// 复制客户端超时设
		RequestConfig requestConfig = RequestConfig.copy(defaultRequestConfig)
				.build();
		httppost.setConfig(requestConfig);
		// 创建参数队列
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		if (_paramsmap != null) {
			for (Entry<String, String> paramsentry : _paramsmap.entrySet()) {
				formparams.add(new BasicNameValuePair(paramsentry.getKey(),
						paramsentry.getValue()));
			}
		}

		UrlEncodedFormEntity uefEntity;
		try {
			uefEntity = new UrlEncodedFormEntity(formparams, "UTF-8");
			httppost.setEntity(uefEntity);
			CloseableHttpResponse response = httpclient.execute(httppost);
			try {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					content = EntityUtils.toString(entity, "UTF-8");
				}
			} finally {
				response.close();
			}
		} finally {
			// 关闭连接,释放资源
			try {
				httpclient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return content;
		}
	}

	/**
	 * 设置请求和传输超时时�?
	 */
	public static RequestConfig buildRequestConfig() {
		RequestConfig requestConfig = RequestConfig.custom()
				.setSocketTimeout(SO_TIMEOUT_MS)
				.setConnectTimeout(CONNECTION_TIMEOUT_MS)
				.setConnectionRequestTimeout(CONNECTION_REQ_TIMEOUT_MS)
				.setStaleConnectionCheckEnabled(true).build();
		return requestConfig;
	}
}
