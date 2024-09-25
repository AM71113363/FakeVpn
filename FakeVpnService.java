//COPYRIGHT AM71113363


package sss.am71113363.fakevpn;

import android.app.PendingIntent;
import android.content.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.os.ParcelFileDescriptor;
import android.util.*;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
import java.io.*;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import sss.am71113363.fakevpn.DATABASE;



public class FakeVpnService extends VpnService
{
	private FrameLayout lnTop;
    private WindowManager windowManager;
	static Context ctx;
    private static TextView status,txt;
	private static FileChannel vpnInput,vpnOut ;
	
	private ParcelFileDescriptor vpnInterface = null;
    private PendingIntent pendIntent;
	
	private int tcpClients=0;;
	private int udpClients=0;
	
	
    private static final int IP_LENGTH_OFFSET = 2;
	private static final int IP_ID_OFFSET = 4;
	private static final int IP_PROTOCOL_OFFSET = 9;
	private static final int IP_CHECKSUM_OFFSET = 10;
	private static final int IP_SRC_ADDR_OFFSET = 12;
	private static final int IP_DST_ADDR_OFFSET = 16;
	private static final int IP_PACK_LEN  = 20;
	private static final int UDP_SRC_PORT_OFFSET = IP_PACK_LEN + 0;
	private static final int UDP_DST_PORT_OFFSET = IP_PACK_LEN + 2;
	private static final int UDP_CHECKSUM_OFFSET = IP_PACK_LEN + 6;
	private static final int DNS_PACK_OFFSET  = IP_PACK_LEN +8;
	private static final int DNS_FLAGS_OFFSET = DNS_PACK_OFFSET +2;
	private static final int DNS_ANSWER_OFFSET = DNS_PACK_OFFSET +6;
    private static final int DATA_OFFSET = DNS_PACK_OFFSET +12;
	
	
	private boolean makePack(ByteBuffer buffer,int totalLength)
	{
		try
		{
			int hdrVL,declaredLen;
			int ipId,ipCheckSum,ipSrcAddr,ipDstAddr;
			short udpSrcPort,udpDstPort;
			short dnsFlags;
			int udpChecksum;

			buffer.position(0);
			hdrVL = (buffer.get(0)&0xFF);
			if(hdrVL != 69)
				return false;
			declaredLen = buffer.getShort(IP_LENGTH_OFFSET)&0xffff;
			if(declaredLen != totalLength)
				return false;

			if(buffer.get(IP_PROTOCOL_OFFSET) != 17)//UDP
				return false;

			if(buffer.getShort(UDP_DST_PORT_OFFSET) != 53)
				return false;
                        //<trick>
			ipId=buffer.getShort(IP_ID_OFFSET)&0xFFFF;
			ipCheckSum=buffer.getShort(IP_CHECKSUM_OFFSET)&0xFFFF;
			buffer.putShort(IP_ID_OFFSET,(short)(ipCheckSum&0xFFFF));
			buffer.putShort(IP_CHECKSUM_OFFSET,(short)(ipId&0xFFFF));
                        //</trick>
			ipSrcAddr=buffer.getInt(IP_SRC_ADDR_OFFSET);
			ipDstAddr=buffer.getInt(IP_DST_ADDR_OFFSET);
			
			buffer.putInt(IP_SRC_ADDR_OFFSET,ipDstAddr);
			buffer.putInt(IP_DST_ADDR_OFFSET,ipSrcAddr);
                        
			udpSrcPort=buffer.getShort(UDP_SRC_PORT_OFFSET);
			udpDstPort=buffer.getShort(UDP_DST_PORT_OFFSET);
			udpChecksum=buffer.getShort(UDP_CHECKSUM_OFFSET)&0xFFFF;
			buffer.putShort(UDP_SRC_PORT_OFFSET,udpDstPort);
			buffer.putShort(UDP_DST_PORT_OFFSET,udpSrcPort);
	
			dnsFlags = buffer.getShort(DNS_PACK_OFFSET +2);
			//dns-> rd1,qr1,,,,rcode3,ra1
			buffer.putShort(DNS_FLAGS_OFFSET,(short)(0x8183));
			//dns answer count1
			buffer.putShort(DNS_ANSWER_OFFSET,(short)0x1);


			udpChecksum ^=0xffff;
			udpChecksum +=0x10000;//avoid negative values
			udpChecksum -=dnsFlags;
			udpChecksum +=0x8184; //0x8183 + 0x1
			while((udpChecksum>>16)>0)
				udpChecksum = (udpChecksum&0xFFFF) +(udpChecksum >>16);
			udpChecksum--;
			udpChecksum ^=0xFFFF;

			buffer.putShort(UDP_CHECKSUM_OFFSET,(short)(udpChecksum&0xFFFF));
			buffer.position(0);
		}catch(Throwable e){ return false;}
		return true;
	}
	
	public static void Close()
	{
		new Thread(new Runnable()
		{
		public void run()
		{
		try
		{
			vpnInput.close();
		}catch(Throwable e){}
		}
		}
		).start();
	}
	@Override
	public void onCreate() {
		super.onCreate();
                ctx = this;
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		lnTop=new FrameLayout(this);
		ImageView img=new ImageView(this);
                img.setClickable(false);
		img.setImageResource(R.drawable.logo);
		FrameLayout.LayoutParams pImg=new FrameLayout.LayoutParams(-2,-1);
		lnTop.addView(img, pImg);
		
		
		status=new TextView(this);
		status.setText("0/0/0");
		status.setClickable(false);
		FrameLayout.LayoutParams prm=new FrameLayout.LayoutParams(-2,-1);
		prm.width=MainActivity.dpi2px(120);
		status.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
		lnTop.addView(status,prm);
		status.setBackgroundResource(R.drawable.app_icon);

		txt=new TextView(this);
		txt.setText("");
		txt.setClickable(false);
		FrameLayout.LayoutParams txtP=new FrameLayout.LayoutParams(-1,-1);
		txtP.leftMargin=MainActivity.dpi2px(125);
		txt.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);

		lnTop.addView(txt,txtP);


               WindowManager.LayoutParams
		params= new WindowManager.LayoutParams(
			-1,
			-2,
			WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
			PixelFormat.TRANSLUCENT);

		params.gravity = Gravity.TOP | Gravity.LEFT;
		params.x = 0;
		params.y = 0;
		//params.width=MainActivity.dpi2px(120);
		params.height=MainActivity.dpi2px(30);
                lnTop.setAlpha(0.74f);
		lnTop.setBackgroundColor(Color.TRANSPARENT);
		status.setTextColor(Color.BLUE);
		txt.setTextColor(Color.RED);
		status.setBackgroundColor(Color.TRANSPARENT);
		txt.setBackgroundColor(Color.TRANSPARENT);

		status.setTypeface(null,Typeface.BOLD);
		txt.setTypeface(null,Typeface.BOLD_ITALIC);

		status.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
		txt.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
		windowManager.addView(lnTop, params);
	        String ip=DATABASE.IP();
		if(ip.length()<8)
		{
			stopSelf();
			return;
		}
        try {
            if (vpnInterface == null)
			{
                Builder builder = new Builder();
                builder.addAddress(ip, 0);
                builder.addRoute("0.0.0.0", 0);
                builder.addDnsServer("8.8.8.8");
				int len=DATABASE.getLen();
				if(len>0)
				{
				    Set<String> list = DATABASE.get();
				    Iterator<String> names=list.iterator();

				    while(names.hasNext())
			        {
				       builder.addDisallowedApplication(names.next());
				     }
				}
                vpnInterface = builder.setSession("FakeVpn").setConfigureIntent(pendIntent).establish();
			    
				loop(vpnInterface.getFileDescriptor());
            }
        } catch (Throwable e) {
           vpnInterface=null;
        }
    }

	private void loop( final  FileDescriptor vpnFile)
	{
		
		new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						vpnInput = new FileInputStream(vpnFile).getChannel();
			            vpnOut = new FileOutputStream(vpnFile).getChannel();
						ByteBuffer buffer=ByteBuffer.allocate(4096);
                        
						int ret;
						while(true)
						{
							try
							{
								buffer.clear();
								buffer.position(0);
								ret=vpnInput.read(buffer);
								if(ret>0 && ret < 4096)
								{
									buffer.flip();
									buffer.position(0);
								
									if(makePack(buffer,ret) == true)
									{
									    extractHostname(buffer,DATA_OFFSET,ret);
										buffer.position(0);
									    vpnOut.write(buffer,ret);
									    hnd.sendEmptyMessage(2222);
									}
									else
									{
										hnd.sendEmptyMessage(1111);
									}
									
									if(DATABASE.doExit()==true)
										break;
								}

								Thread.sleep(10);
							}catch(Throwable e)
							{
								break;
							}
						}
					}catch(Throwable e){}
					try{ vpnOut.close(); }catch(Throwable e){}
                    try{ vpnInput.close();}catch(Throwable e){}
					try{ vpnInterface.close();}catch(Throwable e){}
					
					MainActivity.serviceExited(false);
                    DATABASE.doExit(true);
                    vpnOut=null;
                    vpnInput=null;
					vpnInterface=null;
					stopSelf();
				}
			}).start();
	}
    @Override
    public void onDestroy() {

		if (lnTop != null)
			windowManager.removeView(lnTop);
		  DATABASE.doExit(true);
		  
        Toast.makeText(this,"Service Closed",0).show();
        super.onDestroy();
    }
	private static int lastMsgId=0;
	public void extractHostname(ByteBuffer buffer,int start,int end)
	{
		int i,n=0;
		byte[] host=new byte[end-start];
		byte c;
		//3www6google3com0
		for(i=start+1;i<end;i++)
		{
			c=buffer.get(i);
			if(c==0) //end
			   break;
			if( (c >='a' && c <='z') ||
			    (c >='A' && c <='Z') ||
				(c >='0' && c <='9') )
				host[n]=c;
			else
				host[n]='.';
			n++;
		}
		Message msg1=new Message();
		msg1.what=7777;
		msg1.obj=new String(host,0,n);
		hnd.sendMessage(msg1);
		Message msg2=new Message();
		msg2.what=8888;
		lastMsgId=(lastMsgId+1)&0xFFFF;
		msg2.sendingUid=lastMsgId;
		hnd.sendMessageDelayed(msg2,1500);

	}
	private Handler hnd=new Handler()
	{
		@Override
		public void handleMessage(Message ms)
		{
			switch(ms.what)
			{
				case 1111:
				{
				    tcpClients++;
					status.setText(tcpClients+" / "+udpClients);
					
				}break;
			    case 2222:
			    {
					udpClients++;
					status.setText(tcpClients+" / "+udpClients);
				
			    }break;
				case 7777:
				{
					String host=(String) ms.obj;
					if(host!=null)
					{
					   txt.setText(host);
					}
				}
				break;
				case 8888:
				{
					if(ms.sendingUid == lastMsgId)
					{
						txt.setText("");
					}
					
				}break;
				
			}
		}
	};
	
    
}

