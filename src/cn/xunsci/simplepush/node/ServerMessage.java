package cn.xunsci.simplepush.node;

import java.net.SocketAddress;

public final class ServerMessage {

	protected SocketAddress address;
	protected byte[] data;
	
	public ServerMessage(SocketAddress address, byte[] data) throws Exception{
		this.address = address;
		this.data = data;
	}
	
//	public static org.ddpush.im.node.Message getNewInstance(){
//		return null;
//	}
	
	public void setData(byte[] data){
		this.data = data;
	}
	
	public byte[] getData(){
		return this.data;
	}
	
	public SocketAddress getSocketAddress(){
		return this.address;
	}
	
	public void setSocketAddress(SocketAddress addr){
		this.address = addr;
	}

}
