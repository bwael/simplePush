package cn.xunsci.simplepush.client.appuser;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class Message {
	
	
	/*
	 * 协议格式:[1字节版本号][1字节appid][1字节cmd命令码][16字节UUID][2字节包内容长度][包内容]
	 * 版本号 ：1字节  0-255
	 * appid : 1字节 0-255 255个应用同时使用一个服务器，也可以一个应用有255个分类或渠道        0保留用于实时
	 * cmd   : 1字节 0-255 255种交互命令
	 * uuid  : 唯一识别码
	 * contentLength ：2字节 0-65535 理论支持，为避免丢包，将会在配置文件中设为500
	 * content : 1-500字节 字符数组  为保证效果，实际推送可能100左右为宜
	 * 
	 * 1.通用推送命令:
	 * 	命令码：16（0x10）。格式：[1][1][0x10][0x0000] 
	 *	客户端响应：16（0x10）。格式：[1][1][0x10][uuid][0x0000]
	 *
	 * 2.分类推送命令:
	 * 	命令码：17（0x11）。格式：[1][1][0x11][0x0008][8字节无符号整数] 
	 *	客户端响应：17（0x11）。格式：[1][1][0x11][uuid][0x0008][8字节无符号整数]
	 *  注意：分类信息至多64种类型，且按位叠加操作（|和&）进行确认。
	 * 
	 * 3.自定义信息推送命令:
	 * 	命令码：32（0x20）。格式：[1][1][0x20][0x内容长度][内容] 
	 *	客户端响应：32（0x20）。格式：[1][1][0x20] [uuid] [0x0000]
	 *
	 */
	//消息版本号
	public static int version = 1;
	//服务器消息最小长度
	public static final int SERVER_MESSAGE_MIN_LENGTH = 5;
	//客户端消息最短长度
	public static final int CLIENT_MESSAGE_MIN_LENGTH = 21;
	public static final int CMD_0x00 = 0;//心跳包
	public static final int CMD_0x10 = 16;//通用信息
	public static final int CMD_0x11 = 17;//分类信息
	public static final int CMD_0x20 = 32;//自定义信息
	
	//套接字地址
	protected SocketAddress address;
	//消息数据
	protected byte[] data;
	//用ip套接字和数据创建message对象
	public Message(SocketAddress address, byte[] data){
		this.address = address;
		this.data = data;
	}
	
	//剪去长度信息
	public int getContentLength(){
		return (int)ByteBuffer.wrap(data, SERVER_MESSAGE_MIN_LENGTH - 2, 2).getChar();
	}
	//取出data的cmd 转成 int 并返回
	public int getCmd(){
		byte b = data[2];
		//byte 转成 int  byte只有8位 直接转成int时
        //int是32位 不够的java会自动补位。To deal:补位究竟是怎样补得？？？
        //所以需要将byte转成int的高于byte原本的8位的全部置零
		//eg. number如果为 0xabcd， 那么number & 0xff = number & 0x00ff = 0x00cd = 0xcd
		return b & 0xff;
	}
	//检测当前Message数据包的格式是否正确
	public boolean checkFormat(){
		if(address == null || data == null || data.length < Message.SERVER_MESSAGE_MIN_LENGTH){
			return false;
		}
		int cmd = getCmd();
		if(cmd != CMD_0x00
				&& cmd != CMD_0x10
				&& cmd != CMD_0x11
				&& cmd != CMD_0x20){
			return false;
		}
		int dataLen = getContentLength();
		if(data.length != dataLen + SERVER_MESSAGE_MIN_LENGTH){
			return false;
		}
		if(cmd ==  CMD_0x10 && dataLen != 0){
			return false;
		}
		
		if(cmd ==  CMD_0x11 && dataLen != 8){
			return false;
		}
		
		if(cmd ==  CMD_0x20 && dataLen < 1){//must has content
			return false;
		}
		return true;
	}
	
	public void setData(byte[] data){
		this.data = data;
	}
	
	public byte[] getData(){
		return this.data;
	}
	
	public void setSocketAddress(SocketAddress address){
		this.address = address;
	}
	
	public SocketAddress getSocketAddress(){
		return this.address;
	}
	
	public static void setVersion(int v){
		if(v < 1 || v > 255){
			return;
		}
		version = v;
	}
	
	public static int getVersion(){
		return version;
	}

}
