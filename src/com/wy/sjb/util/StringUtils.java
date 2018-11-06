package com.wy.sjb.util;

/**
 * @author wangyuan
 * @date 2018年8月30日
 */
public class StringUtils {

	/**
	 * 判断字符串是否为空
	 * 
	 * @param str
	 * @return 空返回flse，非空返回true.
	 */
	public static boolean checkStr(String str) {
		if (str == null || str == "" || str.equals("null")) {
			return false;
		} else {
			return true;
		}
	}
	
	public static boolean checkStr(String ... strs) {
		for(String str : strs) {
			if (str == null || str == "" || str.equals("null")) {
				return false;
			}
		}
		
		return true;
	}

}
