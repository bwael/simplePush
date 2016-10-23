package cn.xunsci.simplepush.node;

import cn.xunsci.simplepush.util.PropertyUtil;

public class ClientStatMachineCleaner implements Runnable {
	
	private boolean stoped = false;
	private long lastCleanTime = 0;
	
	private int expiredHours = PropertyUtil.getPropertyInt("CLEANER_DEFAULT_EXPIRED_HOURS");;
	
	@Override
	public void run() {
		while(!stoped){
			try{
				synchronized(this){
					this.wait();
					if(stoped == true){
						return;
					}
					doClean();
				}
			}catch(InterruptedException e){
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		System.out.println("cleaner quit");

	}
	
	public void wakeup(){
		synchronized(this){
			this.notifyAll();
		}
	}
	
	private void doClean(){
		lastCleanTime = System.currentTimeMillis();
		System.out.println("clearn stat of expired hours of "+expiredHours+"....");
		System.out.println("max   mem: "+Runtime.getRuntime().maxMemory());
		System.out.println("total mem: "+Runtime.getRuntime().totalMemory());
		System.out.println("free  mem: "+Runtime.getRuntime().freeMemory());
		System.gc();
		try{
			int removed = NodeStatus.getInstance().cleanStatus(expiredHours);
			System.out.println("clean "+removed +" expired stat machines of expired hours of "+expiredHours);
			lastCleanTime = System.currentTimeMillis();
			System.gc();
			System.out.println("gc committed");
			System.out.println("max   mem: "+Runtime.getRuntime().maxMemory());
			System.out.println("total mem: "+Runtime.getRuntime().totalMemory());
			System.out.println("free  mem: "+Runtime.getRuntime().freeMemory());
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void stop(){
		stoped = true;
	}
	
	public void setExpiredHours(int expiredHours){
		this.expiredHours = expiredHours;
	}
	
	public long getLastCleanTime(){
		return this.lastCleanTime;
	}

}
