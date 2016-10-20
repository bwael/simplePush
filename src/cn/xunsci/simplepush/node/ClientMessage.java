package cn.xunsci.simplepush.node;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import cn.xunsci.simplepush.util.StringUtil;

//�ͻ�����Ϣ����
public final class ClientMessage{

  //socket�׽��ֵ�ַ
  protected SocketAddress address;
  //��Ϣ����
  protected byte[] data;

  //����
  public ClientMessage(SocketAddress address, byte[] data) throws Exception{
      this.address = address;
      this.data = data;
  }

  //���á��޸���Ϣ��������
  public void setData(byte[] data){
      this.data = data;
  }

  //��ȡ��Ϣ���������Ϣ����
  public byte[] getData(){
      return this.data;
  }

  //����׽��ֵ�ַ
  public SocketAddress getSocketAddress(){
      return this.address;
  }

  //������Ϣ�����׽��ֵ�ַ
  public void setSocketAddress(SocketAddress addr){
      this.address = addr;
  }

  //��õ���Ϣ���ݵ�version
  public int getVersionNum(){
      byte b = data[0];
      return b & 0xff;
  }

  //�����Ϣ��������   0 ������16 ͨ����Ϣ  17 ������Ϣ  32 �Զ�����Ϣ
  public int getCmd(){
      byte b = data[2];
      return b & 0xff;
  }

  //��ȡ��Ϣ������Ϣ���ݵĳ���
  public int getDataLength(){
      return (int)ByteBuffer.wrap(data, 19, 2).getChar();
  }

  //��ȡuuid
  public String getUuidHexString(){
      return StringUtil.convert(data, 3, 16);
  }

  //�����Ϣ���ݰ���ʽ�Ƿ���ȷ
  public boolean checkFormat(){
      if(this.data == null){
          return false;
      }
      //��Ϣ�����ȼ��
      if(data.length < Constant.CLIENT_MESSAGE_MIN_LENGTH){
          return false;
      }
      //��Ϣ���汾���
      if(getVersionNum() != Constant.VERSION_NUM){
          return false;
      }
      //��Ϣ���ͼ��
      int cmd = getCmd();
      if(cmd != ClientStatMachine.CMD_0x00
              //&& cmd != ClientStatMachine.CMD_0x01
              && cmd != ClientStatMachine.CMD_0x10
              && cmd != ClientStatMachine.CMD_0x11
              && cmd != ClientStatMachine.CMD_0x20
              && cmd != ClientStatMachine.CMD_0xff){
          return false;
      }
      //��Ϣ���ȼ��
      int dataLen = getDataLength();
      if(data.length != dataLen + Constant.CLIENT_MESSAGE_MIN_LENGTH){
          return false;
      }
      //��Ϣ���ͺ���Ϣ���ݳ��ȸ�ʽ���
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