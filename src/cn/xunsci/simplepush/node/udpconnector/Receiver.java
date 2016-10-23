package cn.xunsci.simplepush.node.udpconnector;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import cn.xunsci.simplepush.node.ClientMessage;

/*
 * UDP 服务器 接受客户端消息的处理类
 */
public class Receiver implements Runnable{
	
	protected DatagramChannel channel;
	
	protected int bufferSize = 1024;
	
	protected boolean stoped = false;
	protected ByteBuffer buffer;
	private SocketAddress address;

	protected AtomicLong queueIn = new AtomicLong(0);
	protected AtomicLong queueOut = new AtomicLong(0);
	protected ConcurrentLinkedQueue<ClientMessage> mq = new ConcurrentLinkedQueue<ClientMessage>();
	
	public Receiver(DatagramChannel channel){
		this.channel = channel;
	}
	
	public void init(){
		buffer = ByteBuffer.allocate(this.bufferSize);
	}
	
	public void stop(){
		this.stoped = true;
	}
	
	public void run(){
		while(!this.stoped){
			try{
				//synchronized(enQueSignal){
					processMessage();
				//	if(mq.isEmpty() == true){
				//		enQueSignal.wait();
				//	}
				//}
			}catch(Exception e){
				e.printStackTrace();
			}catch(Throwable t){
				t.printStackTrace();
			}
		}
	}
	
	protected void processMessage() throws Exception{
		address = null;
		buffer.clear();
		try{
			address = this.channel.receive(buffer);
		}catch(SocketTimeoutException timeout){
			
		}
		if(address == null){
			try{
				Thread.sleep(1);
			}catch(Exception e){
				
			}
			return;
		}
		
		buffer.flip();
		byte[] swap = new byte[buffer.limit() - buffer.position()];
		System.arraycopy(buffer.array(), buffer.position(), swap, 0, swap.length);

		ClientMessage m = new ClientMessage(address,swap);
		
		enqueue(m);
		//System.out.println(DateTimeUtil.getCurDateTime()+" r:"+StringUtil.convert(m.getData())+" from:"+m.getSocketAddress().toString());

	}
	
	protected boolean enqueue(ClientMessage message){
		boolean result = mq.add(message);
		if(result == true){
			queueIn.addAndGet(1);
		}
		return result;
	}
	
	protected ClientMessage dequeue(){
		ClientMessage m = mq.poll();
		if(m != null){
			queueOut.addAndGet(1);
		}
		return m;
	}
	
	public ClientMessage receive() {

		ClientMessage m = null;
		while(true){
			m = dequeue();
			if(m == null){
				return null;
			}
			if(m.checkFormat() == true){//妫?鏌ュ寘鏍煎紡鏄惁鍚堟硶锛屼负浜嗙綉缁滃揩閫熷搷搴旓紝鍦ㄨ繖閲屾鏌ワ紝涓嶅湪鎺ユ敹绾跨▼妫?鏌?
				return m;
			}
		}
	}
}
