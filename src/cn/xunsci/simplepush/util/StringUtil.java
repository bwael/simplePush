package cn.xunsci.simplepush.util;

import java.security.MessageDigest;
import java.util.UUID;


public class StringUtil {
		
	/**
     * 检测传入的String类型参数是否为 null
     * 如果传入的参数为null就返回 ""（空字符串）
     * 否则就返回参数本身
     * 作用：防止字符串出现 空指针 异常
     * */
		public static String checkBlankString(String param) {
			if (param == null) {
				return "";
			}
			return param;
		}
		//MD5加密
		public static String md5(String encryptStr) throws Exception {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(encryptStr.getBytes("UTF-8"));
			byte[] digest = md.digest();
			StringBuffer md5 = new StringBuffer();
			for (int i = 0; i < digest.length; i++) {
				md5.append(Character.forDigit((digest[i] & 0xF0) >> 4, 16));
				md5.append(Character.forDigit((digest[i] & 0xF), 16));
			}

			encryptStr = md5.toString();
			return encryptStr;
		}
		//sha1加密
		public static String sha1(String encryptStr) throws Exception {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(encryptStr.getBytes("UTF-8"));
			byte[] digest = md.digest();
			StringBuffer sha1 = new StringBuffer();
			for (int i = 0; i < digest.length; i++) {
				sha1.append(Character.forDigit((digest[i] & 0xF0) >> 4, 16));
				sha1.append(Character.forDigit((digest[i] & 0xF), 16));
			}

			encryptStr = sha1.toString();
			return encryptStr;
		}
		//md5加密字符串。返回加密后的byte[] 
		public static byte[] md5Byte(String encryptStr) throws Exception {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(encryptStr.getBytes("UTF-8"));
			return md.digest();
		}
		//sha1加密字符串。返回加密后的byte[] 
		public static byte[] sha1Byte(String encryptStr) throws Exception {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(encryptStr.getBytes("UTF-8"));
			return md.digest();
		}
		//获得UUID(使用JDK生成的UIID) 并将系统生成的默认的UUID中间的 “-” 去掉 并返回这个处理过的UUID
		public static String genUUIDHexString(){
			return UUID.randomUUID().toString().replaceAll("-", "");
		}
		//将16进制字符串解析成UUID并返回
		public static UUID parseUUIDFromHexString(String hexUUID) throws Exception{
			byte[] data = hexStringToByteArray(hexUUID);
			long msb = 0;
	        long lsb = 0;

	        for (int i=0; i<8; i++)
	            msb = (msb << 8) | (data[i] & 0xff);
	        for (int i=8; i<16; i++)
	            lsb = (lsb << 8) | (data[i] & 0xff);
	        
	        return new java.util.UUID(msb,lsb);
		}
		//将传入的值转换成 16 进制数字 并返回
	    private static char convertDigit(int value) {

	        value &= 0x0f;
	        if (value >= 10)
	            return ((char) (value - 10 + 'a'));
	        else
	            return ((char) (value + '0'));

	    }
	    //将一个16进制字符串转换成字节数组 并返回
	    public static byte[] hexStringToByteArray(String s) {
	        int len = s.length();
	        byte[] data = new byte[len / 2];
	        for (int i = 0; i < len; i += 2) {
	            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                                 + Character.digit(s.charAt(i+1), 16));
	        }
	        return data;
	    }
		//将字节数组转换成一个可打印的格式包含一个十六进制数字字符的字符串(两个字节)
	    public static String convert(final byte bytes[]) {

	        StringBuffer sb = new StringBuffer(bytes.length * 2);
	        for (int i = 0; i < bytes.length; i++) {
	            sb.append(convertDigit((int) (bytes[i] >> 4)));
	            sb.append(convertDigit((int) (bytes[i] & 0x0f)));
	        }
	        return (sb.toString());

	    }
	    //将字节数组的 一部分 转换成一个可打印的格式包含一个十六进制数字字符的字符串(两个字节)
	    public static String convert(final byte bytes[],int pos, int len) {

	        StringBuffer sb = new StringBuffer(len * 2);
	        for (int i = pos; i < pos+len; i++) {
	            sb.append(convertDigit((int) (bytes[i] >> 4)));
	            sb.append(convertDigit((int) (bytes[i] & 0x0f)));
	        }
	        return (sb.toString());

	    }
}
