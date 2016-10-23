package cn.xunsci.simplepush.node;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import cn.xunsci.simplepush.util.StringUtil;

//客户端消息包类
public final class ClientMessage{

  //socket套接字地址
  protected SocketAddress address;
  //消息数据
  protected byte[] data;

  //构造
  public ClientMessage(SocketAddress address, byte[] data) throws Exception{
      this.address = address;
      this.data = data;
  }

  //设置、修改消息包数据体
  public void setData(byte[] data){
      this.data = data;
  }

  //获取消息包具体的消息数据
  public byte[] getData(){
      return this.data;
  }

  //获得套接字地址
  public SocketAddress getSocketAddress(){
      return this.address;
  }

  //设置消息包的套接字地址
  public void setSocketAddress(SocketAddress addr){
      this.address = addr;
  }

  //获得到消息数据的version
  public int getVersionNum(){
      byte b = data[0];
      return b & 0xff;
  }

  //获得消息包得类型   0 心跳，16 通用消息  17 分类消息  32 自定义消息
  public int getCmd(){
      byte b = data[2];
      return b & 0xff;
  }

  //获取消息包中消息数据的长度
  public int getDataLength(){
      return (int)ByteBuffer.wrap(data, 19, 2).getChar();
  }

  //获取uuid
  public String getUuidHexString(){
      return StringUtil.convert(data, 3, 16);
  }

  //检测消息数据包格式是否正确
  public boolean checkFormat(){
      if(this.data == null){
          return false;
      }
      //消息包长度检测
      if(data.length < Constant.CLIENT_MESSAGE_MIN_LENGTH){
          return false;
      }
      //消息包版本检测
      if(getVersionNum() != Constant.VERSION_NUM){
          return false;
      }
      //消息类型检测
      int cmd = getCmd();
      if(cmd != ClientStatMachine.CMD_0x00
              //&& cmd != ClientStatMachine.CMD_0x01
              && cmd != ClientStatMachine.CMD_0x10
              && cmd != ClientStatMachine.CMD_0x11
              && cmd != ClientStatMachine.CMD_0x20
              && cmd != ClientStatMachine.CMD_0xff){
          return false;
      }
      //消息长度检测
      int dataLen = getDataLength();
      if(data.length != dataLen + Constant.CLIENT_MESSAGE_MIN_LENGTH){
          return false;
      }
      //消息类型和消息内容长度格式检测
      if(cmd ==  ClientStatMachine.CMD_0x00 && dataLen != 0){
          return false;
      }

      if(cmd ==  ClientStatMachine.CMD_0x10 && dataLen != 0){
          return false;
      }

      if(cmd ==  ClientStatMachine.CMD_0x11 && dataLen != 8){
          return false;
      }

      if(cmd ==  ClientStatMachine.CMD_0x20 && dataLen != 0){
          return false;
      }

      return true;
  }

}