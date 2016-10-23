package cn.xunsci.simplepush.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;

public class PropertyUtil {
	//默认配置文件名
    public static final String DEFAULTSET = "simplepush";

    //map集合。用来存放Property
    protected static HashMap<String,Properties> propertiesSets = new HashMap<String, Properties>();

    private PropertyUtil() {}

    //将DEFAULTSET配置文件读取保存到map集合中
    protected static void init() {
        init(DEFAULTSET);
    }
	/*
	 * 初始化
	 * 读取默认的simplePush配置信息，使用utf-8编码
	 * 并将其序列化保存在map方便读取使用
	 */
	protected static void init(String setName) {

		//用配置表文件创建ResourceBundle对象。来解析配置表文件
        ResourceBundle rb = ResourceBundle.getBundle(setName);
        //创建一个空白的Properties对象。用来保存读取的配置表文件的配置数据
        Properties properties = new Properties();
        //遍历配置表，得到 key value,转为utf-8格式
		Enumeration<String> eu = rb.getKeys();
		while(eu.hasMoreElements()){
			String key = eu.nextElement().trim();
			String value = rb.getString(key).trim();
			try{
				value = new String(value.getBytes("ISO8859-1"),"UTF-8");
			}catch(Exception e){
				e.printStackTrace();
			}
			//放入properties对象
			properties.put(key.toUpperCase(), value);
		}
		//放入map集合中
		propertiesSets.put(setName, properties);
		
	}
	/*
	 * 获取配置的静态方法
	 */
	public static String getProperty(String key){
		if(propertiesSets.get(DEFAULTSET) == null){
			init();
		}
		return propertiesSets.get(DEFAULTSET).getProperty(key.toUpperCase());
	}
	//getInt
	public static Integer getPropertyInt(String key){
		int value = 0;
		try{
			value = Integer.parseInt(getProperty(key));
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		return value;
	}
	//getFloat
	public static Float getPropertyFloat(String key){
		float value = 0;
		try{
			value = Float.parseFloat(getProperty(key));
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		return value;
	}
	//get key
	public static String getProperty(String setName, String key){
		if(propertiesSets.get(setName) == null){
			init(setName);
		}
		String value = propertiesSets.get(setName).getProperty(key.toUpperCase());
		if(value == null){
			return "";
		}
		return value;
	}
}
