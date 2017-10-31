package MyGugu;
/*
 * 本API完全用Java核心类库实现，其中包含了不少轮子，纯属练手用吧
 * 2017-10-31
 * */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MemobirdAPI	//此方法同时支持GET/POST
{
	private String printContent = "";	//打印内容的队列
	private int theNumberOfContent = 0;	
	
	public String setUserBind()	//用户绑定
	{
		String url = "http://open.memobird.cn/home/setuserbind";
		String timeStamp = new TimeStamp().getTimeStamp();
		String param = "ak=" + MyInfo.ak + 
						"&timestamp=" + timeStamp + 
						"&memobirdID=" + MyInfo.memobirdID +
						"&useridentifying=" + MyInfo.userIdentifying;
		String realUrl = url+"?"+param;
		
		return  Network.getToMemobird(realUrl);		
		//return Network.postToMemobird(url, param);
	}
	
	public String printPaper()
	{
		String result;
		String url = "http://open.memobird.cn/home/printpaper";
		String timeStamp = new TimeStamp().getTimeStamp();
		String param = "ak=" + MyInfo.ak +
						"&timestamp=" + timeStamp + 
						"&printcontent=" + printContent + 
						"&memobirdID=" + MyInfo.memobirdID +
						"&userID=" + MyInfo.userID;
		
		String str = Network.postToMemobird(url, param);
		result = EncodeAndDecode.jsonReader(str, "printcontentid");
		
		//打印成功之后清除打印缓存和计数器（也可以备份到本地哇）
		printContent="";
		theNumberOfContent=0;
		return result;
	}

	public Boolean getPrintStatus(String printContentID)	//获取打印状态，通过唯一的打印任务号码
	{
		String result = "";
		String url = "http://open.memobird.cn/home/getprintstatus";
		String timeStamp = new TimeStamp().getTimeStamp();
		String param = "ak=" + MyInfo.ak +
						"&timestamp=" + timeStamp + 
						"&printcontentid=" + printContentID;
		String realUrl = url+"?"+param;
		
		result = Network.getToMemobird(realUrl);
		result = Network.postToMemobird(url, param);
		
		if(EncodeAndDecode.jsonReader(result, "printflag").equals("1"))
			return true;
		else
			return false;
	}
	
	public int addPrintContent(String text)		//向打印队列增加内容
	{
		if(text.isEmpty())
			return theNumberOfContent;
		if(theNumberOfContent!=0)
			printContent+="|";
		printContent += "T:" + EncodeAndDecode.gbkToBase64(text);
		
		theNumberOfContent++;
		return theNumberOfContent;
	}
	
	public int addPrintContent(File picFile)	//向打印队列增加图片
	{
		if(!picFile.canRead())
			return theNumberOfContent;
		
		if(theNumberOfContent!=0)
			printContent+="|";
		printContent += "P:" + EncodeAndDecode.picToBase64(picFile);	//此函数尚未实现

		theNumberOfContent++;
		return theNumberOfContent;
	}
}

class EncodeAndDecode	//编码及解码
{
	public static String gbkToBase64(String gbk)	//gbk到base64编码
	{
		//自己造一个轮子吧，不是很难，用到位处理的知识
		byte[] ch = gbk.getBytes();
		int quote = ch.length / 3;
		int remain = ch.length % 3;
		byte[] coded;
		if(remain!=0)	//其实就是向上取整，进一法
			coded = new byte[quote*4 + 4];
		else 
			coded = new byte[quote*4];
		
		for(int i = 0; i < quote; i++)	//通过位运算，将3Byte的数据编码成4*6bit的字符，不用乘除法（因为太慢了）
		{
			coded[4*i] = (byte) (ch[3*i]>>2);
			coded[4*i+1] = (byte)(((ch[3*i]&3)<<4) + (ch[3*i+1]>>4));
			coded[4*i+2] = (byte)(((ch[3*i+1]&15)<<2) + (ch[3*i+2]>>6));
			coded[4*i+3] = (byte)(ch[3*i+2]&63);
		}
		switch(remain)	//处理尾巴上的数据
		{
		default:
		case 0:
			break;
		case 1:
			coded[4*quote] = (byte)(ch[3*quote]>>2);
			coded[4*quote+1] = (byte)64;
			coded[4*quote+2] = (byte)64;
			coded[4*quote+3] = (byte)64;
			break;
		case 2:
			coded[4*quote] = (byte)(ch[3*quote]>>2);
			coded[4*quote+1] = (byte)(((ch[3*quote]&3)<<4) + (ch[3*quote+1]>>4));
			coded[4*quote+2] = (byte)((ch[3*quote+1]&15)<<2);
			coded[4*quote+3] = (byte)64;
			break;
		}
		
		for(int i = 0; i < coded.length; i++)	//将base64码映射到ASCII码
		{
			if(coded[i]<26)
				coded[i]+=65;
			else if(coded[i]<52)
				coded[i]+=71;
			else if(coded[i]<62)
				coded[i]-=4;
			else if(coded[i]==62)
				coded[i]=43;
			else if(coded[i]==63)
				coded[i]=47;
			else if(coded[i]==64)
				coded[i]=61;
		}
		String base64 = new String(coded);
		
		return base64;
	}
	
	public static String picToBase64(File file)
	{
		/*
		 *还没想好怎么处理
		 *初步打算支持JPEG和BMP格式图片
		 *第一步是压缩色域，压缩到单通道8bit灰度图
		 *第二步是压缩图片大小，至少有一边不超过120px
		 *第三步是进行base64编码，返回一个字符串
		*/
		return null;
	}
	
	public static String jsonReader(String json, String key)	//json解析，想做成类似于Python中的dictionary那样
	{
		//自己造了一个小轮子，只满足这个API使用
		int i = json.indexOf(key);
		if(i<0)
			return null;
		String result = "";
		while(json.charAt(i+key.length()+2)!=',' && json.charAt(i+key.length()+2)!='}' && i<json.length()-2)
		{
			result+=json.charAt(i+key.length()+2);
			i++;
		}
		return result;
	}
}

class Network	//网络类，封装了GET和POST方法
{
	public static String getToMemobird(String url)
	{
		URL getUrl;
		BufferedReader bufferedReader = null;
		String result = "";
		String line;
		try
		{
			getUrl = new URL(url);
			URLConnection urlConnection;
			urlConnection = getUrl.openConnection();
			//设置http报文头部
			urlConnection.setRequestProperty("Connection","keep-alive");
			urlConnection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.62 Safari/537.36");
			urlConnection.setRequestProperty("Upgrade-Insecure-Requests", "1");
			urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			urlConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
			urlConnection.setRequestProperty("Cookie", MyInfo.cookie);
			
			urlConnection.connect();
			bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
		} catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		try
		{
			while((line = bufferedReader.readLine())!=null)
			{
				result+=line;
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return result;
	}

	public static String postToMemobird(String url, String param)
	{
		URL postUrl;
		PrintWriter printWriter = null;
		BufferedReader bufferedReader = null;
		String result = "";
		String line;
		
		try
		{
			postUrl = new URL(url);
			URLConnection urlConnection = postUrl.openConnection();
			
			urlConnection.setRequestProperty("Connection","keep-alive");
			urlConnection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.62 Safari/537.36");
			urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			urlConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
			urlConnection.setRequestProperty("Cookie", MyInfo.cookie);
			urlConnection.setRequestProperty("Origin", "http://open.memobird.cn");
			urlConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
			urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			
			printWriter = new PrintWriter(urlConnection.getOutputStream());
			
			printWriter.print(param);
			printWriter.flush();
			
			bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			
			while((line = bufferedReader.readLine())!=null)
				result+=line;
			
		} catch (MalformedURLException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
				try
				{
					if(bufferedReader!=null)
						bufferedReader.close();
					if(printWriter!=null)
				printWriter.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
		}
		return result;
	}
	
}

class TimeStamp		//生成时间戳
{
	private SimpleDateFormat dFormat1,dFormat2;	 	//两个对象分别产生日期和时间，只用一个对象无法解决中间的空格编码问题	
	private String value;
	
	public TimeStamp()
	{
		Date date = new Date();
		dFormat1 = new SimpleDateFormat("yyyy-MM-dd");
		dFormat2 = new SimpleDateFormat("HH:mm:ss");
		value = dFormat1.format(date) + "%20" + dFormat2.format(date);	//这里是个坑，URL类没有编码url的功能，需要用%20代替空格
	}
	
	public String getTimeStamp()
	{
		return value;
	}
	
	@Override
	public String toString()
	{
		return value;
	}
}

final class MyInfo		//个人信息
{
	public static String ak = "";	//开发者access key
	public static String memobirdID = "";			//咕咕机的设备编号
	public static String userIdentifying = "";				//咕咕号
	public static String userID = "";							//第一次绑定时获得
	public static String cookie = "";	//本地cookies
}

