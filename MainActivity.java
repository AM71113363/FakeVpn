package sss.am71113363.fakevpn;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.net.*;
import android.provider.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import java.util.*;
import android.content.pm.*;
import android.graphics.drawable.*;
import android.util.*;
import java.net.*;

public class MainActivity extends Activity 
{
	final static int CODE_LIST_APPS     = 4000;
	final static int CODE_START_VPN     = 4001;
	final static int CODE_CONTAINER_ADD = 4002;
	final static int CODE_CONFIRM_LIST  = 4003;
	final static int CODE_GET_IP        = 4004;
	final static int CODE_CHECK_OVERLAY = 4005;
	
	
	Context ctx=null;
	private static Button opBtn;
	static LinearLayout hBlockedContainer=null;
	static LinearLayout hAllowedContainer=null;

	private static float dpi=2.625f;
	int int150=150;
	int int20=20;
	int int10=10;
	int int170=170;
	int int4=4;
	int int2=2;
	public static int dpi2px(float px)
	{
		Float calc=Float.valueOf(Math.round(dpi*px));
		return calc.intValue();
	}
	public static void serviceExited(boolean isRunning)
	{
		if(isRunning)
		opBtn.setText("STOP <VPN>");
		else
		opBtn.setText("START <VPN>");
	}
	private void loadInts()
	{
	        int2=dpi2px(   0.761f );
		int4=dpi2px(   1.523f );
	        int10=dpi2px(  3.809f );
		int20=dpi2px(  7.619f );
	        int150=dpi2px(57.142f );
		int170=dpi2px(64.761f );
	}
	
	private void alertWindow(String title,String ms,boolean _isError)
	{

		final AlertDialog.Builder asp=new AlertDialog.Builder(this);
		TextView showText = new TextView(this);
		showText.setText(ms);
		showText.setTextIsSelectable(true);
		showText.setBackgroundColor(Color.WHITE);
		if(_isError==true){ showText.setTextColor(Color.RED);}
		else{ showText.setTextColor(Color.GREEN);}
		asp.setView(showText);
	        asp.setTitle(title);
		asp.setCancelable(true);
		asp.setIcon(R.drawable.logo);
                asp.setPositiveButton("Confirm",new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dg,int jf)
				{
					dg.dismiss();
					Intent vpnIntent = VpnService.prepare(ctx);

					if (vpnIntent != null)
						startActivityForResult(vpnIntent,CODE_START_VPN);
					else
						onActivityResult(CODE_START_VPN, RESULT_OK, null);
				}
			});
		asp.show();

	}
	
	
	public void listApps()
	{
		loadInts();
		List<ApplicationInfo> list=getPackageManager().getInstalledApplications(0);
		if(list!=null)
			displayResults(list);
	}
	
	private Drawable myShape(int width,int color,float radius)
	{
		GradientDrawable shape=new GradientDrawable();
		shape.setColor(Color.TRANSPARENT);
		shape.setCornerRadius(radius);
		shape.setStroke(width,color);

		return shape;
	}
	
	private FrameLayout makeFrame(BAG data,String AppName,String packageName,Drawable image)
	{
		FrameLayout container;
		ImageView imgv;
		Button name;

		container= new FrameLayout(ctx);

		FrameLayout profile=new FrameLayout(ctx);
		FrameLayout.LayoutParams profParam=new FrameLayout.LayoutParams(0,0);
		profParam.width=int150;
		profParam.height=int150;
		profParam.leftMargin=int20;
		profParam.topMargin=int20;
		profParam.rightMargin=int20;
		profParam.bottomMargin=int20;
		profParam.gravity=Gravity.LEFT;
		profile.setBackgroundColor(Color.TRANSPARENT);
		container.addView(profile,profParam);


		imgv= new ImageView(ctx);
		FrameLayout.LayoutParams img1Param=new FrameLayout.LayoutParams(0,0);
		img1Param.width=-1;
		img1Param.height=-1;
		img1Param.topMargin=int10;
		img1Param.rightMargin=int10;
		img1Param.leftMargin=int10;
		img1Param.bottomMargin=int10;
		img1Param.gravity=Gravity.CENTER;
		imgv.setBackgroundColor(Color.TRANSPARENT);
		imgv.setImageDrawable(image);
		profile.addView(imgv,img1Param);

		name=new Button(ctx);

		FrameLayout.LayoutParams btn1Param=new FrameLayout.LayoutParams(0,0);
		btn1Param.width=-1;
		btn1Param.height=int150;
		btn1Param.leftMargin=int10;
		btn1Param.rightMargin=int20;
		name.setBackgroundColor(Color.TRANSPARENT);
		name.setPadding(int170,int20,0,0);
	        name.setGravity(Gravity.LEFT);
                name.setTypeface(null,Typeface.ITALIC);
		container.addView(name,btn1Param);
		container.setBackground(myShape(int4,Color.rgb(0x80,0x80,0x80),70));
     
		name.setText(AppName+"\r\n"+packageName);
		
                name.setTransformationMethod(null);
		name.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View arg0)
				{
					BAG data=  (BAG)((FrameLayout)arg0.getParent()).getTag();
					//BAG data=(BAG)arg0.getTag();
					if(data!=null)
					{
						float blur=data.frame().getAlpha();
						
						if(blur == 0.5f) //is in white list
						{
							data.frame().setAlpha(1f);
							data.isBlocked(true);
							if(data.child() != null)
								hAllowedContainer.removeView(data.child());
							return;
						}
						
						
						BAG cm=new BAG(data.appName(),data.packageName(),data.image());

						View frame=makeFrame(cm,data.appName(),data.packageName(),data.image());
						
						if(data.isBlocked()) //top view,not blur
						{
							cm.isBlocked(false);
							cm.frame(frame);
							cm.child(data.frame());
							Message ms=new Message();
							ms.what=CODE_CONTAINER_ADD;
							ms.obj=cm;
							hnd.sendMessage(ms);
							data.frame().setAlpha(0.5f);
							data.child(frame);
						}
						else
						{
							data.child().setAlpha(1f);
							data.parent().removeView(data.frame());
                        }
					}
				}
			});
			if(data!=null)
			{
		      data.btn(name);
		     data.frame(container);
		}
		return container;
	}

	
	private void displayResults(List<ApplicationInfo> p)
	{
		int i,len;
		len = p.size();
		//len=10;
		boolean both=false;
		View fBlocked=null;
		View fAllowed=null;
		BAG  bagBlocked = null;
		BAG bagAllowed  = null;
	
		for(i=0;i<len;i++)
		{
			ApplicationInfo uno= p.get(i);//if((uno.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)!=0)
	       
			String PackageName=uno.packageName;
		    if(uno.uid < 10000)
				continue;
			Intent intr=getPackageManager().getLaunchIntentForPackage(PackageName);
			if(intr== null)
				continue;
			if(DATABASE.add(PackageName) == false)
			{
				both=true;
			}else{both=false;}
			String AppName=getPackageManager().getApplicationLabel(uno).toString();
			Drawable icon=uno.loadIcon(getPackageManager());
			
			bagBlocked=new BAG(AppName,PackageName,icon);
		        fBlocked=makeFrame(bagBlocked,AppName,PackageName,icon);
			if(both == true)
			{
				bagAllowed=new BAG(AppName,PackageName,icon);
				fAllowed=makeFrame(bagAllowed,AppName,PackageName,icon);
				bagAllowed.child(fBlocked);
				bagBlocked.child(fAllowed);
				fBlocked.setAlpha(0.5f);
				bagAllowed.isBlocked(false);
			}
			else
			{
				bagBlocked.child(null);
			}
			bagBlocked.isBlocked(true);
			Message ms=new Message();
			ms.what=CODE_CONTAINER_ADD;
			ms.obj=bagBlocked;
			hnd.sendMessage(ms);
		
			if(both == true)
			{
				Message msg=new Message();
				msg.what=CODE_CONTAINER_ADD;
				msg.obj=bagAllowed;
				hnd.sendMessage(msg);
			}
		}
	}
	
	private void savePreferences(Set<String> list)
	{
		SharedPreferences pref= getPreferences(Context.MODE_PRIVATE);
		if(pref!=null)
		{
			SharedPreferences.Editor ed=pref.edit();
			if(list.size() >0)
			{
		       ed.putStringSet("FAV",list);
			}else{  ed.clear(); }
			ed.apply();
		}
	}
	
	private void confirmList(String ip)
	{
		int len=hAllowedContainer.getChildCount();
		boolean err=true;
		DATABASE.reset();
		String s="WARNING: Internet Blocked for All Apps!!";
		if(len>0)
		{
			s="";
			err=false;
                        for(int i=0;i<len;i++)
			{
				FrameLayout frm=(FrameLayout)hAllowedContainer.getChildAt(i);
				if(frm!=null)
				{
					BAG b=(BAG)frm.getTag();
					if(b!=null)
					{
						DATABASE.add(b.packageName());
						s+="      ▶ "+b.appName()+"\r\n";
					}
				}
			}
		}
		savePreferences(DATABASE.get());
		alertWindow("WhiteList["+ip+"]",s,err);
	}

	
	public FrameLayout create()
	{
		FrameLayout frame=new FrameLayout(ctx);
		frame.setBackgroundColor(Color.WHITE);
		opBtn=new Button(ctx);
		FrameLayout.LayoutParams btn1Param=new FrameLayout.LayoutParams(0,0);
		btn1Param.width=-1;
		btn1Param.height=dpi2px(38f);
		btn1Param.topMargin=dpi2px(3.8f);
		btn1Param.rightMargin=dpi2px(3.8f);
		btn1Param.leftMargin=dpi2px(3.8f);
		btn1Param.gravity=Gravity.CENTER_HORIZONTAL|Gravity.TOP;
		opBtn.setTransformationMethod(null);
		frame.addView(opBtn,btn1Param);
		opBtn.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View arg0)
				{
					hnd.sendEmptyMessage(CODE_GET_IP); 
				}
			});
		ScrollView hScrollB=new ScrollView(ctx);
		ScrollView hScrollA=new ScrollView(ctx);
		
		//hScrollB.setBackgroundColor(Color.TRANSPARENT);
		hScrollB.setBackground(myShape(int10,Color.RED,10));
		
		//hScrollA.setBackgroundColor(Color.TRANSPARENT);
		hScrollA.setBackground(myShape(int10,Color.GREEN,10));
		
	        FrameLayout.LayoutParams  hScrollParamsB=new FrameLayout.LayoutParams(-1,-1);
		FrameLayout.LayoutParams  hScrollParamsA=new FrameLayout.LayoutParams(-1,-1);
		int top = dpi2px(40f);
		Display dsp=getWindowManager().getDefaultDisplay();
		DisplayMetrics mtr=new DisplayMetrics();
		dsp.getMetrics(mtr);
		
		int height=mtr.heightPixels;
		TypedValue tv=new TypedValue();
		if(getTheme().resolveAttribute(android.R.attr.actionBarSize,tv,true))
		{
			height-=TypedValue.complexToDimension(tv.data,getResources().getDisplayMetrics());
		}
		int resId=getResources().getIdentifier("status_bar_height","dimen","android");
		if(resId>0)
			height-=getResources().getDimensionPixelSize(resId);
		height-=top;
		int half=height/2;
	        hScrollParamsB.topMargin=top;
		hScrollParamsA.topMargin=top+half;
		
		hScrollParamsB.height= half;
		hScrollParamsA.height= half;
		
		hBlockedContainer=new LinearLayout(this);
		hAllowedContainer=new LinearLayout(this);
		
		hBlockedContainer.setPadding(20,20,20,20);
		hAllowedContainer.setPadding(20,20,20,20);
		
		hBlockedContainer.setOrientation(LinearLayout.VERTICAL);
		hAllowedContainer.setOrientation(LinearLayout.VERTICAL);
		//hAllowedContainer.setBackgroundColor(Color.GREEN);
		
		hScrollB.addView(hBlockedContainer); 
		hScrollA.addView(hAllowedContainer); 
		
		frame.addView(hScrollB,hScrollParamsB);
		frame.addView(hScrollA,hScrollParamsA);
		
		return frame;
	}

	@Override
	protected void onResume()
	{
		hnd.sendEmptyMessage(CODE_CHECK_OVERLAY);
		super.onResume();
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		ctx=this;
		dpi=this.getResources().getDisplayMetrics().density;
		FrameLayout noe=create();
		
        setContentView(noe);
    }
	private void askOverlay()
	{
		if(!Settings.canDrawOverlays(this))
		{
			Intent in=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));
			startActivityForResult(in,CODE_LIST_APPS);
		}
		else
		{
			onActivityResult(CODE_LIST_APPS,RESULT_OK,null);
		}
	}

	@Override
	protected void onActivityResult(int code, int status, Intent data)
	{
		if(status != RESULT_OK)
		{
		    super.onActivityResult(code,status, data);
		  return;
		}
		if(code == CODE_LIST_APPS)
		{
			if(!Settings.canDrawOverlays(this))
			{
				finish();
			}
			else
			{
				hnd.sendEmptyMessage(CODE_LIST_APPS);
			}
		}
		if(code == CODE_START_VPN)
		{
			opBtn.setText("STOP <VPN>");
			DATABASE.doExit(false);
		  startService(new Intent(this, FakeVpnService.class));
		}
	}

	private Handler hnd=new Handler()
	{
		@Override
		public void handleMessage(Message ms)
		{
			switch(ms.what)
			{
				case CODE_CHECK_OVERLAY:
				{
					if(DATABASE.doExit())
					{
						opBtn.setText("START <VPN>");
					}else
					{
						opBtn.setText("STOP <VPN>");
					}
					askOverlay();
					
				}break;
				case CODE_CONTAINER_ADD:
				{
					BAG t=(BAG) ms.obj;
					if(t!=null)
					{
						LinearLayout.LayoutParams Pm=new LinearLayout.LayoutParams(-1,-1);
						Pm.setMargins(0,0,0,int2);
						t.frame().setTag(t);
						if(t.isBlocked())
						{
							t.parent(hBlockedContainer);
							hBlockedContainer.addView(t.frame(),Pm);
						
						}
						else
						{
						    t.parent(hAllowedContainer);
							hAllowedContainer.addView(t.frame(),Pm);
						}
					}
				}
				break;
				
				
				case CODE_LIST_APPS:
				{
					DATABASE.reset();
					SharedPreferences pref= getPreferences(Context.MODE_PRIVATE);
					if(pref!=null)
					{
						Set<String> fav=pref.getStringSet("FAV",null);
					    if(fav!=null)
						{
							DATABASE.add(fav);
						}
					}
					hBlockedContainer.removeAllViews();
					hAllowedContainer.removeAllViews();
					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
							
							listApps();
							}catch(Throwable e)
							{
								opBtn.setText(e.toString());
							}
						}
						}).start();
					}break;
				case CODE_START_VPN:
				{
                    Intent vpn = VpnService.prepare(ctx);
                    if(vpn != null)
                        startActivityForResult(vpn,CODE_START_VPN);
                    else
                        onActivityResult(CODE_START_VPN, RESULT_OK, null);
					
				}break;
				case CODE_CONFIRM_LIST:
				{
                    String ip=DATABASE.IP();
					if(ip.length()<8)
					{
						Toast.makeText(ctx,"Unable to get Local Ip",1).show();
					}
					else
				    {
						confirmList(ip);
					}
				}
				break;
				case CODE_GET_IP:
				{
					
					if(DATABASE.doExit()==false)
					{
						DATABASE.doExit(true);
						FakeVpnService.Close();
						break;
					}
					DATABASE.IP("");
					new Thread(new Runnable()
					{
						public void run()
						{
							try
							{
								String ip="";
								DatagramSocket dg=new DatagramSocket();
								//dg.connect(InetAddress.getByName("8.8.8.8"),10000);
								dg.connect(InetAddress.getByAddress(new byte[]{8,8,8,8}),10000);
								ip=dg.getLocalAddress().getHostAddress();
								dg.close();
							    DATABASE.IP(ip);
								hnd.sendEmptyMessage(CODE_CONFIRM_LIST);
								}catch(Throwable e)
								{
									opBtn.setText("Need Internet to Get Local IP!!!");
									hnd.sendEmptyMessageDelayed(8888,4000);
								}
							}}
					).start();
					
				}break;
				default:
				opBtn.setText("START <VPN>");
				break;
			}
		}
	};
	
}
