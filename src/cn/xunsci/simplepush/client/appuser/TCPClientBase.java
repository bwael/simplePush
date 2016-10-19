package cn.xunsci.simplepush.client.appuser;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/*
 * TCP�ͻ��˻���
 */
public abstract class TCPClientBase implements Runnable{
	
	//���ӳ�ʱ    10s
	protected static int connectTimeout = 10;
	//�׽��ֻ���
	protected SocketChannel channel;
	//���һ�η���ʱ��
	protected long lastSent = 0;
	//Զ�̷������˿�
	protected int remotePort = 9966;
	//appid
	protected int appid = 1;
	//uuid
	protected byte[] uuid;
	//Զ�̷�������ַ
	protected String remoteAddress = null;
	//������Ϣ����
	protected ConcurrentLinkedQueue<Message> mq = new ConcurrentLinkedQueue<Message>();
	
	//��Ϣ���н�����Ϣ����
	protected AtomicLong queueIn = new AtomicLong(0);
	//��Ϣ���з�����Ϣ����
	protected AtomicLong queueOut = new AtomicLong(0);

	//��������С
	protected int bufferSize = 1024;
	//���������     50s
	protected int heartbeatInterval = 50;
	
	//���ڴ����Ϣ����
	protected byte[] bufferArray;
	//���ڴ�����Ϣ���ݵĻ�����
	protected ByteBuffer buffer;
	//�Ƿ���������
	protected boolean needReset = true;
	
	//����״̬��ʶ
	protected boolean started = false;
	//ֹͣ״̬��ʶ
	protected boolean stoped = false;
	
	//��ǰTCPClientBase�߳�
	protected Thread receiverT;
	//�����߳��࣬������Ϣ����
	protected Worker worker;
	//�����߳�
	protected Thread workerT;
	
	//���������հ���
	private long sentPackets;
	private long receivedPackets;
	
	/*
	 * TCP�ͻ��˳�ʼ��
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
	
	//���      ��Ϣ������Ϣ����
	protected boolean enqueue(Message message){
		boolean result = mq.add(message);
		if(result == true){
			queueIn.addAndGet(1);
		}
		return result;
	}
	//����    ����Ϣ����Ϣ������ȡ��
	protected Message dequeue(){
		Message m = mq.poll();
		if(m != null){
			queueOut.addAndGet(1);
		}
		return m;
	}
	
	//��ʼ�� ���ݿ�  ����
	private synchronized void init(){
		bufferArray = new byte[bufferSize];
		buffer = ByteBuffer.wrap(bufferArray);
		buffer.limit(Message.SERVER_MESSAGE_MIN_LENGTH);
	}
	
	//socket����  �ͷ������������жϺ��������� 
	protected synchronized void reset() throws Exception{
		if(needReset == false){
			return;
		}
		//���socketͨ�� != null �Ͱ�socketͨ������
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
		//�����������״̬ �����������״̬�����Ϳ�ʼ������Զ�̷�����������
		if(hasNetworkConnection() == true){
			channel = SocketChannel.open();
			//socket������Ϊ����
			channel.configureBlocking(true);
			channel.socket().connect(new InetSocketAddress(remoteAddress, remotePort), 1000*connectTimeout);
			channel.socket().setSoTimeout(1000*5);
			needReset = false;
		}else{
			try{Thread.sleep(1000);}catch(Exception e){}
		}
	}
	
	/*
	 * ����TCPClient
	 */
	public synchronized void start() throws Exception{
		
		if(this.started == true){
			return;
		}
		this.init();
		
		//��ǰ�߳�
		receiverT = new Thread(this,"SIMPLEPUSH-TCP-CLIENT-RECEIVER");
		//�ػ��߳�
		receiverT.setDaemon(true);
		synchronized(receiverT){
			receiverT.start();
			receiverT.wait();
		}
		
		//�����߳�  ����������  �Լ�������յ�����Ϣ
		worker = new Worker();
		workerT = new Thread(worker,"SIMPLEPUSH-TCP-CLIENT-WORKER");
		//�ػ��߳�
		workerT.setDaemon(true);
		synchronized(workerT){
			workerT.start();
			workerT.wait();
		}
		
		//����״̬���
		this.started = true;
	}
	/*
	 * ֹͣTCPClientBase
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
	 * TCPClientBase�߳���
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run(){
		
		//���ѵ�ǰTCPClientBase�����д���wait״̬���߳�
		synchronized(receiverT){
			receiverT.notifyAll();
		}
		
		while(stoped == false){
			try{
				//�������״̬������ ���������� ��ʱ�򡣾�һֱ����ѭ���������ӷ����� ���������ݡ�ֱ�������������� 
				if(hasNetworkConnection() == false){
					try{
						trySystemSleep();
						Thread.sleep(1000);
					}catch(Exception e){}
					continue;
				}
				//����socket
				reset();
				//���շ���������������
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
	 * ����������
	 */
	private void heartbeat() throws Exception{
		//�������50s
		if(System.currentTimeMillis() - lastSent < heartbeatInterval * 1000){
			return;
		}
		//���췢�������������message��
		byte[] buffer = new byte[Message.CLIENT_MESSAGE_MIN_LENGTH];
		ByteBuffer.wrap(buffer).put((byte)Message.version).put((byte)appid).put((byte)Message.CMD_0x00).put(uuid).putChar((char)0);
		send(buffer);
	}
	
	/*
	 * ���շ�������Ϣ
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
		
		//�����ܵ������ݿ�����һ���µ�byte[]���Ա� 
        //��ǰ��buffer�ܹ������Ľ��ܷ���������������Ϣ����
		System.arraycopy(bufferArray, 0, data, 0, buffer.position());
		//���ݷ���������������Ϣ���ݡ�����һ��Message��Ϣ���ݰ�
		Message m = new Message(channel.socket().getRemoteSocketAddress(), data);
		buffer.clear();
		buffer.limit(Message.SERVER_MESSAGE_MIN_LENGTH);
		if(m.checkFormat() == false){
			return;
		}
		this.receivedPackets++;//���ܰ�������һ
		//�����������ȷ��Ӧ��
		this.ackServer(m);
		//�����������������
		if(m.getCmd() == Message.CMD_0x00){
			return;
		}
		this.enqueue(m);
		worker.wakeup();
	}
	
	/*
	 * �ж��Ƿ��п��Խ��յ�����
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
	 * �Է���������Ӧ��
	 * ˵���Ѿ����յ����Է����������ݰ�
	 * ��˵���ܵ������������ݰ�
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
	//���������������
	private void send(byte[] data) throws Exception{
		if(data == null){
			return;
		}
		if(channel == null || channel.isOpen() == false || channel.isConnected() == false){
			return;
		}
		//��������������
		ByteBuffer bb = ByteBuffer.wrap(data);
		//���״̬��д��
		while(bb.hasRemaining()){
			channel.write(bb);
		}
		//ȡ����flush,˵��ˢ�¿��ܸ����ʣ�flush���Ǵ�cache���Ƴ�ȥ��
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
	
	//�����������
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
	public abstract void onPushMessage(Message message);//�յ���Ϣ�� ��Ϣ���� �ص�
	
	//�����߳���     �����������Լ������յ�����Ϣ��
	class Worker implements Runnable{
		public void run(){
			//���ѹ����߳�
			synchronized(workerT){
				workerT.notifyAll();
			}
			while(stoped == false){
				try{
					//���Է�������
					heartbeat();
					//handle message
					handleEvent();
				}catch(Exception e){
					e.printStackTrace();
				}finally{
					//���ߵȴ�
					waitMsg();
				}
			}
		}
		//����1s
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
		//����Worker�������wait״̬���߳��˳�wait״̬��
		private void wakeup(){
			synchronized(this){
				this.notifyAll();
			}
		}
		 //����Message��Ϣ
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
				//�յ�һ����Ϣ��������ص���Ϣ����ĺ��� 
                //����ǰ��Ϣ���ݸ���Ϣ������
				onPushMessage(m);
			}
			//finish work here, such as release wake lock
		}
		
	}

}
