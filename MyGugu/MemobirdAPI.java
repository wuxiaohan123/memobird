package MyGugu;
/*
 * ��API��ȫ��Java�������ʵ�֣����а����˲������ӣ����������ð�
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

public class MemobirdAPI	//�˷���ͬʱ֧��GET/POST
{
	private String printContent = "";	//��ӡ���ݵĶ���
	private int theNumberOfContent = 0;	
	
	public String setUserBind()	//�û���
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
		
		//��ӡ�ɹ�֮�������ӡ����ͼ�������Ҳ���Ա��ݵ������ۣ�
		printContent="";
		theNumberOfContent=0;
		return result;
	}

	public Boolean getPrintStatus(String printContentID)	//��ȡ��ӡ״̬��ͨ��Ψһ�Ĵ�ӡ�������
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
	
	public int addPrintContent(String text)		//���ӡ������������
	{
		if(text.isEmpty())
			return theNumberOfContent;
		if(theNumberOfContent!=0)
			printContent+="|";
		printContent += "T:" + EncodeAndDecode.gbkToBase64(text);
		
		theNumberOfContent++;
		return theNumberOfContent;
	}
	
	public int addPrintContent(File picFile)	//���ӡ��������ͼƬ
	{
		if(!picFile.canRead())
			return theNumberOfContent;
		
		if(theNumberOfContent!=0)
			printContent+="|";
		printContent += "P:" + EncodeAndDecode.picToBase64(picFile);	//�˺�����δʵ��

		theNumberOfContent++;
		return theNumberOfContent;
	}
}

class EncodeAndDecode	//���뼰����
{
	public static String gbkToBase64(String gbk)	//gbk��base64����
	{
		//�Լ���һ�����Ӱɣ����Ǻ��ѣ��õ�λ�����֪ʶ
		byte[] ch = gbk.getBytes();
		int quote = ch.length / 3;
		int remain = ch.length % 3;
		byte[] coded;
		if(remain!=0)	//��ʵ��������ȡ������һ��
			coded = new byte[quote*4 + 4];
		else 
			coded = new byte[quote*4];
		
		for(int i = 0; i < quote; i++)	//ͨ��λ���㣬��3Byte�����ݱ����4*6bit���ַ������ó˳�������Ϊ̫���ˣ�
		{
			coded[4*i] = (byte) (ch[3*i]>>2);
			coded[4*i+1] = (byte)(((ch[3*i]&3)<<4) + (ch[3*i+1]>>4));
			coded[4*i+2] = (byte)(((ch[3*i+1]&15)<<2) + (ch[3*i+2]>>6));
			coded[4*i+3] = (byte)(ch[3*i+2]&63);
		}
		switch(remain)	//����β���ϵ�����
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
		
		for(int i = 0; i < coded.length; i++)	//��base64��ӳ�䵽ASCII��
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
		 *��û�����ô����
		 *��������֧��JPEG��BMP��ʽͼƬ
		 *��һ����ѹ��ɫ��ѹ������ͨ��8bit�Ҷ�ͼ
		 *�ڶ�����ѹ��ͼƬ��С��������һ�߲�����120px
		 *�������ǽ���base64���룬����һ���ַ���
		*/
		return null;
	}
	
	public static String jsonReader(String json, String key)	//json������������������Python�е�dictionary����
	{
		//�Լ�����һ��С���ӣ�ֻ�������APIʹ��
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

class Network	//�����࣬��װ��GET��POST����
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
			//����http����ͷ��
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

class TimeStamp		//����ʱ���
{
	private SimpleDateFormat dFormat1,dFormat2;	 	//��������ֱ�������ں�ʱ�䣬ֻ��һ�������޷�����м�Ŀո��������	
	private String value;
	
	public TimeStamp()
	{
		Date date = new Date();
		dFormat1 = new SimpleDateFormat("yyyy-MM-dd");
		dFormat2 = new SimpleDateFormat("HH:mm:ss");
		value = dFormat1.format(date) + "%20" + dFormat2.format(date);	//�����Ǹ��ӣ�URL��û�б���url�Ĺ��ܣ���Ҫ��%20����ո�
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

final class MyInfo		//������Ϣ
{
	public static String ak = "";	//������access key
	public static String memobirdID = "";			//���������豸���
	public static String userIdentifying = "";				//������
	public static String userID = "";							//��һ�ΰ�ʱ���
	public static String cookie = "";	//����cookies
}

