package org.springfield.marvin;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.imageio.ImageWriteParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class MarvinManager {
	
	private static MarvinManager instance;
	
	private MarvinManager() {
        System.out.println("Marvin Manager started");

	}
	
    public static MarvinManager instance(){
    	if(instance==null) instance = new MarvinManager();
    	return instance;
    }
    
	public void sendOembedData(String name,HttpServletRequest request,HttpServletResponse response) {
		System.out.println("GOT A MARVIN SERVLET REQUEST ! "+name);
		String type = request.getParameter("type");
		String format = request.getParameter("format");
		String url = request.getParameter("url");
		System.out.println("TYPE="+type);
		System.out.println("FORMAT="+format);
		System.out.println("URL="+url);
		String body="";
		if (type.equals("euscreen")) {
			body = getEUScreenBody(name,url,format);
		}
		try {


			OutputStream out = response.getOutputStream();
			out.write(body.getBytes());
			out.flush();
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public String getEUScreenBody(String name,String url,String format) {
		if (format.equals("json")) {
			JSONObject reply= new JSONObject();
			reply.put("version","1.0");
			reply.put("type","video");
			reply.put("provider_name","Noterik");
			String domainurl = convertEUScreenId(url);
			System.out.println("DOMAINURL="+domainurl);
			reply.put("url",domainurl);
			
			String html = "<iframe src=\"http://oembed.noterik.com/oembed?url="+domainurl+"\"></iframe>";
			reply.put("html", html);
			String body=reply.toString();
			return body;
		}
		return "";
	}
	
	private String convertEUScreenId(String eus) {
		StringBuilder result = new StringBuilder();
		try {
			URL serverUrl = new URL("http://www.euscreen.eu/item.html?id="+eus);
			HttpURLConnection urlConnection = (HttpURLConnection)serverUrl.openConnection();
	
			urlConnection.setDoOutput(true);
			urlConnection.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			//System.out.println("RESULT="+result);
			rd.close();
			int pos = result.indexOf("images3.noterik.com/domain/euscreenxl");
			if (pos!=-1) {
				String domainurl = result.substring(pos+19);
				int pos2=domainurl.indexOf("/shots/");
				if (pos2!=-1) {
					domainurl=domainurl.substring(0,pos2);
					
					// now find the correct url !
					
					domainurl=getCorrectStream(domainurl);
					return domainurl;
				}
			}
			return null;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private String getCorrectStream(String url) {
		StringBuilder result = new StringBuilder();
		for (int i=4;i<21;i++) {
			try {
			URL serverUrl = new URL("http://stream.noterik.com/progressive/stream"+i+"/"+url+"/rawvideo/1/raw.mp4");
			HttpURLConnection urlConnection = (HttpURLConnection)serverUrl.openConnection();
	
			urlConnection.setDoOutput(true);
			urlConnection.setRequestMethod("GET");
			int code = urlConnection.getResponseCode();
			//System.out.println("CODE="+code);
			if (code==403) {
				return "/stream"+i+url;
			}
			} catch(Exception e) {
				//e.printStackTrace();
			}
		}
		return null;
	}

	
}
