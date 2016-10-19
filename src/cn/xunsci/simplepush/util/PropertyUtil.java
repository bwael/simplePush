package cn.xunsci.simplepush.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;

public class PropertyUtil {
	//Ĭ�������ļ���
    public static final String DEFAULTSET = "simplepush";

    //map���ϡ��������Property
    protected static HashMap<String,Properties> propertiesSets = new HashMap<String, Properties>();

    private PropertyUtil() {}

    //��DEFAULTSET�����ļ���ȡ���浽map������
    protected static void init() {
        init(DEFAULTSET);
    }
	/*
	 * ��ʼ��
	 * ��ȡĬ�ϵ�simplePush������Ϣ��ʹ��utf-8����
	 * ���������л�������map�����ȡʹ��
	 */
	protected static void init(String setName) {

		//�����ñ��ļ�����ResourceBundle�������������ñ��ļ�
        ResourceBundle rb = ResourceBundle.getBundle(setName);
        //����һ���հ׵�Properties�������������ȡ�����ñ��ļ�����������
        Properties properties = new Properties();
        //�������ñ��õ� key value,תΪutf-8��ʽ
		Enumeration<String> eu = rb.getKeys();
		while(eu.hasMoreElements()){
			String key = eu.nextElement().trim();
			String value = rb.getString(key).trim();
			try{
				value = new String(value.getBytes("ISO8859-1"),"UTF-8");
			}catch(Exception e){
				e.printStackTrace();
			}
			//����properties����
			properties.put(key.toUpperCase(), value);
		}
		//����map������
		propertiesSets.put(setName, properties);
		
	}
	/*
	 * ��ȡ���õľ�̬����
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
