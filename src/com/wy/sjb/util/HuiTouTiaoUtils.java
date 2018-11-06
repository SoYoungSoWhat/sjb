package com.wy.sjb.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class HuiTouTiaoUtils {
	
//	Content-Type:     application/encrypted-json                                                                                                
//	Accept:           application/encrypted-json
	
	@Test
	public void newReadReward() throws Exception {
		try {
			
			String url = "http://api.cashtoutiao.com/frontend/collection/check";
			Map<String, String> headMap = new HashMap<String, String>();
			headMap.put("Content-Type", "application/encrypted-json");
			headMap.put("Accept", "application/encrypted-json");
			headMap.put("Host", "api.cashtoutiao.com");
			headMap.put("Connection", "Keep-Alive");
			headMap.put("Accept-Encoding", "gzip");
//			headMap.put("Content-Length", "272");
			headMap.put("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 7.1.2; MI 5X MIUI/V9.2.2.0.NDBCNEK) hsp");
			
			Map<String, String> paramMap = new HashMap<String, String>();
			paramMap.put("userId", "121128772");
			paramMap.put("loginId", "e002237ce0694b409e9198bfd4f95476");
			String respStr = HttpClientUtils.requestPost(url, paramMap, headMap);
			System.out.println(respStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
