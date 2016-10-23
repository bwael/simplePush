package cn.xunsci.simplepush.node;

import java.util.ArrayList;

import cn.xunsci.simplepush.node.udpconnector.UdpConnector;

public class Messenger implements Runnable {
	
	private UdpConnector connector;
	private NodeStatus nodeStat;//this is very large and dynamic
	private Thread hostThread;
	
	boolean started = false;
	boolean stoped = false;
	
	public Messenger(UdpConnector connector, NodeStatus nodeStat){
		this.connector = connector;
		this.nodeStat = nodeStat;
	}

	@Override
	public void run() {
		this.started = true;
		
		while(stoped == false){
			try{
				procMessage();
			}catch(Exception e){
				e.printStackTrace();
			}catch(Throwable t){
				t.printStackTrace();
			}
		}

	}
	
	public void stop(){
		this.stoped = true;
	}
	
	private void procMessage() throws Exception{
		ClientMessage m = this.obtainMessage();
		if(m == null){
			try{
				Thread.sleep(5);
			}catch(Exception e){
				;
			}
			return;
		}
		
		this.deliverMessage(m);
		
	}
	
	private void deliverMessage(ClientMessage m) throws Exception{
		//System.out.println(this.hostThread.getName()+" receive:"+StringUtil.convert(m.getData()));
		//System.out.println(m.getSocketAddress().getClass().getName());
		String uuid = m.getUuidHexString();
		//ClientStatMachine csm = NodeStatus.getInstance().getClientStat(uuid);
		ClientStatMachine csm = nodeStat.getClientStat(uuid);
		if(csm == null){//
			csm = ClientStatMachine.newByClientTick(m);
			if(csm == null){
				return;
			}
			nodeStat.putClientStat(uuid, csm);
		}
		ArrayList<ServerMessage> smList = csm.onClientMessage(m);
		if(smList == null){
			return;
		}
		for(int i = 0; i < smList.size(); i++){
			ServerMessage sm = smList.get(i);
			if(sm.getSocketAddress() == null)continue;
			this.connector.send(sm);
		}
		
	}
	
	private ClientMessage obtainMessage() throws Exception{
		return connector.receive();
	}
	
	public void setHostThread(Thread t){
		this.hostThread = t;
	}
	
	public Thread getHostThread(){
		return this.hostThread;
	}

}