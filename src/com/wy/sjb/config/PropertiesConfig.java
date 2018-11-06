package com.wy.sjb.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * @author wangyuan
 * @date 2018年8月30日
 */
public class PropertiesConfig {
	
	private Properties prop = new Properties();
	
	private static PropertiesConfig config;
	
	private PropertiesConfig() throws FileNotFoundException, IOException {
		init();
	}
	
	public static void main(String[] args) throws Exception {
		PropertiesConfig.getInstance();
	}
	
	private void init() throws FileNotFoundException, IOException {
//		String path = PropertiesConfig.class.getResource("/").getPath() + "/sjb.properties";
//		File file = new File(path);
//		if (!file.exists()) {
//			prop.load(new FileInputStream(new File(path)));
//		} else {
//		}
		prop.load(PropertiesConfig.class.getClassLoader().getResourceAsStream("sjb.properties"));
	}

	public static PropertiesConfig getInstance() throws FileNotFoundException, IOException {
		if (config == null) {
			synchronized (PropertiesConfig.class) {
				if (config == null) {
					config = new PropertiesConfig();
				}
			}
		}
		return config;
	}
	
	public Properties getProp() {
		return prop;
	}
	
	/** 重新加载properties文件 */
	public Properties reLoadProp() throws FileNotFoundException, IOException {
		init();
		return prop;
	}
	
}
