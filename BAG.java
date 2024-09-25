package sss.am71113363.fakevpn;

import android.view.*;
import android.widget.*;
import android.graphics.drawable.*;

public class BAG
{
	private String appName;
	private String packageName;
	private Drawable image;
	private LinearLayout parent;
	private View child;
	private View frame;
	private Button btn;
	private boolean isBlocked;

	public BAG(String aName,String pName,Drawable icon)
	{
		appName     = aName;
		packageName = pName;
		image       = icon;
	}
	public void child(View l)
	{
		child = l;
	}
	public View child()
	{
		return child;
	}
	public Drawable image()
	{
		return image;
	}
	public void image(Drawable d)
	{
		image = d;
	}
	public String appName()
	{
		return appName;
	}
	public void appName(String s)
	{
		appName = new String(s);
	}
	public String packageName()
	{
		return packageName;
	}
	public void packageName(String s)
	{
		packageName = new String(s);
	}
	public View frame()
	{
		return frame;
	}
	public void frame(View v)
	{
		frame = v;
	}
	public boolean isBlocked()
	{
		return isBlocked;
	}
	public void isBlocked(boolean b)
	{
		isBlocked = b;
	}
	public void parent(LinearLayout l)
	{
		parent = l;
	}
	public LinearLayout parent()
	{
		return parent;
	}
	public void btn(Button b)
	{
		btn = b;
	}
	public Button btn()
	{
		return btn;
	}
}
