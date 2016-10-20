package cn.xunsci.simplepush.node;

import java.nio.ByteBuffer;

import cn.xunsci.simplepush.util.StringUtil;


public class PushMessage {
protected byte[] data;
	
	public PushMessage(byte[] data) throws Exception{
		if(data == null){
			throw new NullPointerException("data array is null");
		}
		this.data = data;
		if(checkFormat() == false){
			throw new java.lang.IllegalArgumentException("data format error");
		}
	}
	
	public void setData(byte[] data){
		this.data = data;
	}
	
	public byte[] getData(){
		return this.data;
	}
	
	public int getVersionNum(){
		byte b = data[0];
		return b & 0xff;
	}
	
	public int getCmd(){
		byte b = data[2];
		return b & 0xff;
	}
	
	public int getContentLength(){
		return (int)ByteBuffer.wrap(data, 19, 2).getChar();
	}
	
	public String getUuidHexString(){
		return StringUtil.convert(data, 3, 16);
	}
	
	public boolean checkFormat(){
		if(data.length < Constant.CLIENT_MESSAGE_MIN_LENGTH){
			return false;
		}
		if(getVersionNum() != Constant.VERSION_NUM){
			return false;
		}

		int cmd = getCmd();
		if(cmd != ClientStatMachine.CMD_0x10
				&& cmd != ClientStatMachine.CMD_0x11
				&& cmd != ClientStatMachine.CMD_0x20){
			return false;
		}
		int dataLen = getContentLength();
		if(data.length != dataLen + Constant.CLIENT_MESSAGE_MIN_LENGTH){
			return false;
		}
		
		if(cmd ==  ClientStatMachine.CMD_0x10 && dataLen != 0){
			return false;
		}
		
		if(cmd ==  ClientStatMachine.CMD_0x11 && dataLen != 8){
			return false;
		}
		
		if(cmd ==  ClientStatMachine.CMD_0x20 && dataLen < 1){//must has content
			return false;
		}
		
		return true;
	}
}
