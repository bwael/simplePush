package cn.xunsci.simplepush.client.appuser;

import java.util.ArrayList;

import cn.xunsci.simplepush.util.StringUtil;

public class MyTcpClient extends TCPClientBase {

	public MyTcpClient(byte[] uuid, int appid, String serverAddr,
			int serverPort, int connectTimeout) throws Exception {
		super(uuid, appid, serverAddr, serverPort, connectTimeout);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean hasNetworkConnection() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void onPushMessage(Message msg) {
		if(msg == null){
			System.out.println("msg is null");
		}
		if(msg.getData() == null || msg.getData().length == 0){
			System.out.println("msg has no data");
		}
		System.out.println(StringUtil.convert(this.uuid)+"---"+StringUtil.convert(msg.getData()));
	}

	@Override
	public void trySystemSleep() {
		//System.out.println("try sleep");

	}
	
	public static void main(String[] args){
		try{
			ArrayList list = new ArrayList();
			for(int i = 0; i < 1; i++){
				byte[] uuid = StringUtil.md5Byte(""+i);
				System.out.println("uuid is: "+StringUtil.convert(uuid));
				MyTcpClient myTcpClient = new MyTcpClient(uuid, 1, "192.168.1.3", 9966, 5);
				myTcpClient.setHeartbeatInterval(50);
				myTcpClient.start();
				//Thread.sleep(1000);
				//System.out.println(myTcpClient.channel.socket().getLocalAddress().toString());
				//System.out.println("client started...");
				//synchronized(myTcpClient){
				//myTcpClient.wait();
				//}
				list.add(myTcpClient);
				
			}
			synchronized(list){
				list.wait();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
