package cn.xunsci.simplepush.util;

import java.security.MessageDigest;
import java.util.UUID;


public class StringUtil {
		
	/**
     * ��⴫���String���Ͳ����Ƿ�Ϊ null
     * �������Ĳ���Ϊnull�ͷ��� ""�����ַ�����
     * ����ͷ��ز�������
     * ���ã���ֹ�ַ������� ��ָ�� �쳣
     * */
		public static String checkBlankString(String param) {
			if (param == null) {
				return "";
			}
			return param;
		}
		//MD5����
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
		//sha1����
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
		//md5�����ַ��������ؼ��ܺ��byte[] 
		public static byte[] md5Byte(String encryptStr) throws Exception {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(encryptStr.getBytes("UTF-8"));
			return md.digest();
		}
		//sha1�����ַ��������ؼ��ܺ��byte[] 
		public static byte[] sha1Byte(String encryptStr) throws Exception {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(encryptStr.getBytes("UTF-8"));
			return md.digest();
		}
		//���UUID(ʹ��JDK���ɵ�UIID) ����ϵͳ���ɵ�Ĭ�ϵ�UUID�м�� ��-�� ȥ�� ����������������UUID
		public static String genUUIDHexString(){
			return UUID.randomUUID().toString().replaceAll("-", "");
		}
		//��16�����ַ���������UUID������
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
		//�������ֵת���� 16 �������� ������
	    private static char convertDigit(int value) {

	        value &= 0x0f;
	        if (value >= 10)
	            return ((char) (value - 10 + 'a'));
	        else
	            return ((char) (value + '0'));

	    }
	    //��һ��16�����ַ���ת�����ֽ����� ������
	    public static byte[] hexStringToByteArray(String s) {
	        int len = s.length();
	        byte[] data = new byte[len / 2];
	        for (int i = 0; i < len; i += 2) {
	            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                                 + Character.digit(s.charAt(i+1), 16));
	        }
	        return data;
	    }
		//���ֽ�����ת����һ���ɴ�ӡ�ĸ�ʽ����һ��ʮ�����������ַ����ַ���(�����ֽ�)
	    public static String convert(final byte bytes[]) {

	        StringBuffer sb = new StringBuffer(bytes.length * 2);
	        for (int i = 0; i < bytes.length; i++) {
	            sb.append(convertDigit((int) (bytes[i] >> 4)));
	            sb.append(convertDigit((int) (bytes[i] & 0x0f)));
	        }
	        return (sb.toString());

	    }
	    //���ֽ������ һ���� ת����һ���ɴ�ӡ�ĸ�ʽ����һ��ʮ�����������ַ����ַ���(�����ֽ�)
	    public static String convert(final byte bytes[],int pos, int len) {

	        StringBuffer sb = new StringBuffer(len * 2);
	        for (int i = pos; i < pos+len; i++) {
	            sb.append(convertDigit((int) (bytes[i] >> 4)));
	            sb.append(convertDigit((int) (bytes[i] & 0x0f)));
	        }
	        return (sb.toString());

	    }
}
