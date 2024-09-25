package sss.am71113363.fakevpn;

import java.util.*;

public abstract class DATABASE
{
    private static String IP="";
	private static boolean doExit=true;
	private static Set<String> list=new TreeSet<String>();

	public static String IP()
	{
		return IP;
	}
    public static void IP(String ip)
	{
		IP=ip;
	}
	public static void reset()
	{
		list.clear();
	}
	public static void add(Set<String> l)
	{
		list.addAll(l);
	}
	public static boolean add(String s)
	{ 
	    return list.add(s); 
	}
	public static int getLen()
	{ 
	    return list.size(); 
	}
	public static Set<String> get()
	{ 
	    return list;
	}
	
	public static void doExit(boolean b)
	{
		doExit=b;
	}
	public static boolean doExit()
	{
		return doExit;
	}
	
}
