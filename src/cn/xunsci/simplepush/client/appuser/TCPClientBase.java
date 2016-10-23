package cn.xunsci.simplepush.client.appuser;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/*
 * TCP客户端基类
 */
public abstract class TCPClientBase implements Runnable{
	
	//连接超时    10s
	protected static int connectTimeout = 10;
	//套接字缓冲
	protected SocketChannel channel;
	//最后一次发送时间
	protected long lastSent = 0;
	//远程服务器端口
	protected int remotePort = 9966;
	//appid
	protected int appid = 1;
	//uuid
	protected byte[] uuid;
	//远程服务器地址
	protected String remoteAddress = null;
	//并发消息队列
	protected ConcurrentLinkedQueue<Message> mq = new ConcurrentLinkedQueue<Message>();
	
	//消息队列接收消息计数
	protected AtomicLong queueIn = new AtomicLong(0);
	//消息队列发送消息计数
	protected AtomicLong queueOut = new AtomicLong(0);

	//缓冲区大小
	protected int bufferSize = 1024;
	//心跳包间隔     50s
	protected int heartbeatInterval = 50;
	
	//用于存放消息数据
	protected byte[] bufferArray;
	//用于处理消息数据的缓冲区
	protected ByteBuffer buffer;
	//是否重置连接
	protected boolean needReset = true;
	
	//启动状态标识
	protected boolean started = false;
	//停止状态标识
	protected boolean stoped = false;
	
	//当前TCPClientBase线程
	protected Thread receiverT;
	//工作线程类，用于消息处理
	protected Worker worker;
	//工作线程
	protected Thread workerT;
	
	//发包数，收包数
	private long sentPackets;
	private long receivedPackets;
	
	/*
	 * TCP客户端初始化
	 */
	public TCPClientBase(byte[] uuid, int appid, String serverAddr, int serverPort, int connectTimeout) throws Exception{
		if(uuid == null || uuid.length != 16){
			throw new java.lang.IllegalArgumentException("uuid byte array must be not null and length of 16 bytes");
		}
		if(appid < 1 || appid > 255){
			throw new java.lang.IllegalArgumentException("appid must be from 1 to 255");
		}
		if(serverAddr == null || serverAddr.trim().length() == 0){
			throw new java.lang.IllegalArgumentException("server address illegal: "+serverAddr);
		}
		
		this.uuid = uuid;
		this.appid = appid;
		this.remoteAddress = serverAddr;
		this.remotePort = serverPort;
		TCPClientBase.connectTimeout = connectTimeout;
	}
	
	//入队      消息加入消息队列
	protected boolean enqueue(Message message){
		boolean result = mq.add(message);
		if(result == true){
			queueIn.addAndGet(1);
		}
		return result;
	}
	//出队    将消息从消息队列中取出
	protected Message dequeue(){
		Message m = mq.poll();
		if(m != null){
			queueOut.addAndGet(1);
		}
		return m;
	}
	
	//初始化 数据块  缓冲
	private synchronized void init(){
		bufferArray = new byte[bufferSize];
		buffer = ByteBuffer.wrap(bufferArray);
		buffer.limit(Message.SERVER_MESSAGE_MIN_LENGTH);
	}
	
	//socket重置  和服务器的连接中断后重新连接 
	protected synchronized void reset() throws Exception{
		if(needReset == false){
			return;
		}
		//如果socket通道 != null 就把socket通道销毁
		if(channel != null){
			try{
				channel.socket().close();
				}catch(Exception e){
				}
			try{
				channel.close();
				}catch(Exception e){
				}
		}
		//检测网络连接状态 如果网络连接状态正常就开始创建到远程服务器的连接
		if(hasNetworkConnection() == true){
			channel = SocketChannel.open();
			//socket被设置为阻塞
			channel.configureBlocking(true);
			channel.socket().connect(new InetSocketAddress(remoteAddress, remotePort), 1000*connectTimeout);
			channel.socket().setSoTimeout(1000*5);
			needReset = false;
		}else{
			try{Thread.sleep(1000);}catch(Exception e){}
		}
	}
	
	/*
	 * 启动TCPClient
	 */
	public synchronized void start() throws Exception{
		
		if(this.started == true){
			return;
		}
		this.init();
		
		//当前线程
		receiverT = new Thread(this,"SIMPLEPUSH-TCP-CLIENT-RECEIVER");
		//守护线程
		receiverT.setDaemon(true);
		synchronized(receiverT){
			receiverT.start();
			receiverT.wait();
		}
		
		//工作线程  发送心跳包  以及处理接收到的消息
		worker = new Worker();
		workerT = new Thread(worker,"SIMPLEPUSH-TCP-CLIENT-WORKER");
		//守护线程
		workerT.setDaemon(true);
		synchronized(workerT){
			workerT.start();
			workerT.wait();
		}
		
		//更新状态标记
		this.started = true;
	}
	/*
	 * 停止TCPClientBase
	 */
	public synchronized void stop(){
		stoped = true;
		
		if(channel != null){
			try{channel.socket().close();}catch(Exception e){};
			try{channel.close();}catch(Exception e){};
		}
		channel = null;
		if(receiverT != null){
			try{receiverT.interrupt();}catch(Exception e){}
		}

		if(workerT != null){
			try{workerT.interrupt();}catch(Exception e){}
		}
	}
	/*
	 * TCPClientBase线程体
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run(){
		
		//唤醒当前TCPClientBase的所有处于wait状态的线程
		synchronized(receiverT){
			receiverT.notifyAll();
		}
		
		while(stoped == false){
			try{
				//如果网络状态不正常 无网络连接 的时候。就一直跳出循环。不连接服务器 不处理数据。直到网络连接正常 
				if(hasNetworkConnection() == false){
					try{
						trySystemSleep();
						Thread.sleep(1000);
					}catch(Exception e){}
					continue;
				}
				//重置socket
				reset();
				//接收服务器发来的数据
				receiveData();
			}catch(java.net.SocketTimeoutException e){
				
			}catch(java.nio.channels.ClosedChannelException e){
				this.needReset = true;
			}catch(Exception e){
				e.printStackTrace();
				this.needReset = true;
			}catch(Throwable t){
				t.printStackTrace();
				this.needReset = true;
			}finally{
				if(needReset == true){
					try{
						trySystemSleep();
						Thread.sleep(1000);
					}catch(Exception e){}
				}
				if(mq.isEmpty() == true || hasNetworkConnection() == false){
					try{
						trySystemSleep();
						Thread.sleep(1000);
					}catch(Exception e){}
				}
			}
		}
		if(this.channel != null){
			try{channel.socket().close();}catch(Exception e){}
			try{channel.close();}catch(Exception e){}
			channel = null;
		}
	}
	
	/*
	 * 发送心跳包
	 */
	private void heartbeat() throws Exception{
		//心跳间隔50s
		if(System.currentTimeMillis() - lastSent < heartbeatInterval * 1000){
			return;
		}
		//构造发送心跳包，详见message类
		byte[] buffer = new byte[Message.CLIENT_MESSAGE_MIN_LENGTH];
		ByteBuffer.wrap(buffer).put((byte)Message.version).put((byte)appid).put((byte)Message.CMD_0x00).put(uuid).putChar((char)0);
		send(buffer);
	}
	
	/*
	 * 接收服务器消息
	 */
	private void receiveData() throws Exception{
		while(hasPacket() == false){
			int read = channel.read(buffer);
			if(read < 0){
				throw new Exception("end of stream");
			}
			if(hasPacket() == true){
				break;
			}
			if(mq.isEmpty() == true || hasNetworkConnection() == false){
				try{
					trySystemSleep();
					Thread.sleep(1000);
				}catch(Exception e){}
			}
		}
		
		byte[] data = new byte[buffer.position()];
		
		//将接受到得数据拷贝到一个新的byte[]中以便 
        //当前的buffer能够继续的接受服务器发送来得消息数据
		System.arraycopy(bufferArray, 0, data, 0, buffer.position());
		//根据服务器发送来得消息数据。创建一个Message消息数据包
		Message m = new Message(channel.socket().getRemoteSocketAddress(), data);
		buffer.clear();
		buffer.limit(Message.SERVER_MESSAGE_MIN_LENGTH);
		if(m.checkFormat() == false){
			return;
		}
		this.receivedPackets++;//接受包计数加一
		//向服务器发送确认应答
		this.ackServer(m);
		//如果是心跳包，丢弃
		if(m.getCmd() == Message.CMD_0x00){
			return;
		}
		this.enqueue(m);
		worker.wakeup();
	}
	
	/*
	 * 判断是否有可以接收的数据
	 */
	private boolean hasPacket(){
		if(buffer.limit() == Message.SERVER_MESSAGE_MIN_LENGTH){
			if(buffer.hasRemaining() == true){
				return false;
			}else{
				int dataLen = (int)ByteBuffer.wrap(bufferArray, Message.SERVER_MESSAGE_MIN_LENGTH-2, 2).getChar();
				if(dataLen == 0){
					return true;
				}else{
					buffer.limit(Message.SERVER_MESSAGE_MIN_LENGTH+dataLen);
					return false;
				}
			}
		}else{
			if(buffer.hasRemaining() == true){
				return false;
			}else{
				return true;
			}
		}
	}
	/*
	 * 对服务器做出应答
	 * 说明已经接收到来自服务器的数据包
	 * 并说明受到了怎样的数据包
	 */
	private void ackServer(Message m) throws Exception{
		if(m.getCmd() == Message.CMD_0x10){
			byte[] buffer = new byte[Message.CLIENT_MESSAGE_MIN_LENGTH];
			ByteBuffer.wrap(buffer).put((byte)Message.version).put((byte)appid).put((byte)Message.CMD_0x10).put(uuid).putChar((char)0);
			send(buffer);
		}
		if(m.getCmd() == Message.CMD_0x11){
			byte[] buffer = new byte[Message.CLIENT_MESSAGE_MIN_LENGTH + 8];
			byte[] data = m.getData();
			ByteBuffer.wrap(buffer).put((byte)Message.version).put((byte)appid).put((byte)Message.CMD_0x11).put(uuid).putChar((char)8).put(data, Message.SERVER_MESSAGE_MIN_LENGTH, 8);
			send(buffer);
		}
		if(m.getCmd() == Message.CMD_0x20){
			byte[] buffer = new byte[Message.CLIENT_MESSAGE_MIN_LENGTH];
			ByteBuffer.wrap(buffer).put((byte)Message.version).put((byte)appid).put((byte)Message.CMD_0x20).put(uuid).putChar((char)0);
			send(buffer);
		}
	}
	//向服务器发送数据
	private void send(byte[] data) throws Exception{
		if(data == null){
			return;
		}
		if(channel == null || channel.isOpen() == false || channel.isConnected() == false){
			return;
		}
		//缓冲区包裹数据
		ByteBuffer bb = ByteBuffer.wrap(data);
		//检测状态，写入
		while(bb.hasRemaining()){
			channel.write(bb);
		}
		//取出，flush,说是刷新可能更合适，flush就是从cache里推出去了
		channel.socket().getOutputStream().flush();
		lastSent = System.currentTimeMillis();
		this.sentPackets++;
	}
	
	public long getSentPackets(){
		return this.sentPackets;
	}
	
	public long getReceivedPackets(){
		return this.receivedPackets;
	}
	
	public long getLastHeartbeatTime(){
		return lastSent;
	}
	
	//设置心跳间隔
	public void setHeartbeatInterval(int second){
		if(second <= 0){
			return;
		}
		this.heartbeatInterval = second;
	}
	
	public int getHeartbeatInterval(){
		return this.heartbeatInterval;
	}
	
	public abstract boolean hasNetworkConnection();
	public abstract void trySystemSleep();
	public abstract void onPushMessage(Message message);//收到消息的 消息处理 回调
	
	//工作线程类     发送心跳包以及处理收到的消息包
	class Worker implements Runnable{
		public void run(){
			//唤醒工作线程
			synchronized(workerT){
				workerT.notifyAll();
			}
			while(stoped == false){
				try{
					//尝试发送心跳
					heartbeat();
					//handle message
					handleEvent();
				}catch(Exception e){
					e.printStackTrace();
				}finally{
					//休眠等待
					waitMsg();
				}
			}
		}
		//休眠1s
		private void waitMsg(){
			synchronized(this){
				try{
					this.wait(1000);
				}catch(java.lang.InterruptedException e){
					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		//唤醒Worker里的所有wait状态的线程退出wait状态。
		private void wakeup(){
			synchronized(this){
				this.notifyAll();
			}
		}
		 //处理Message消息
		private void handleEvent() throws Exception{
			Message m = null;
			while(true){
				m = dequeue();
				if(m == null){
					return;
				}
				if(m.checkFormat() == false){
					continue;
				}

				//real work here
				//收到一个消息。在这里回调消息处理的函数 
                //将当前消息传递给消息处理函数
				onPushMessage(m);
			}
			//finish work here, such as release wake lock
		}
		
	}

}
