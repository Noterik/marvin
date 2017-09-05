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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
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
import org.springfield.fs.FSList;
import org.springfield.fs.FSListManager;
import org.springfield.fs.Fs;
import org.springfield.fs.FsNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class MarvinManager {
	
	private static MarvinManager instance;
	private static Random generator;
	
	private MarvinManager() {
        System.out.println("Marvin Manager started");

	}
	
    public static MarvinManager instance(){
    	if(instance==null) instance = new MarvinManager();
    	return instance;
    }
    
	public void sendOembedData(String name,HttpServletRequest request,HttpServletResponse response) {
		if (generator==null) generator = new Random(System.currentTimeMillis());
		String type = request.getParameter("type");
		String format = request.getParameter("format");
		String url = request.getParameter("url");
		String body="";
		int pos = url.indexOf("/euscreen/");
		if (pos!=-1) {
			url=url.substring(pos+10);
			body = getEUScreenBody(request,name,url,format);
		} else if (type.equals("euscreen")) {
			body = getEUScreenBody(request,name,url,format);
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

	public String getEUScreenBody(HttpServletRequest request,String name,String url,String format) {
		if (format.equals("json")) {
			JSONObject reply= new JSONObject();
			reply.put("version","1.0");
			reply.put("type","video");
			reply.put("provider_name","Noterik");
			//String domainurl = convertEUScreenId(reply,url);
			String domainurl = getCorrectStreamNew(reply,url);
			System.out.println("DOMAINURL2="+domainurl);
			
			String referer=request.getHeader("Referer");
			System.out.println("MARVIN REFER="+request.getHeader("Referer"));
			
			Boolean valid = false;

			if (referer.startsWith("http://oembed.euscreen.eu")) {
				valid = true;
			}

			if (valid) {
				String random = ""+generator.nextInt(999999999);
				String ticket = "mst_marvin_"+random;
				FsNode newticket = new FsNode("ticket",ticket);
				newticket.setProperty("expire",""+(new Date().getTime()/1000)+5);
				newticket.setProperty("url",""+domainurl);
				Fs.insertNode(newticket,"/domain/oembed/service/marvin/");
				String html = "<iframe width=\"640\" height=\"480\" src=\"http://oembed.euscreen.eu/oembed?url="+domainurl+"&ticket="+ticket+"\"></iframe>";
				reply.put("html", html);
			} else {
				String html = "<iframe width=\"640\" height=\"480\" src=\"http://oembed.euscreen.eu/oembed?url="+domainurl+"\"></iframe>";
				reply.put("html", html);
			}
			String body=reply.toString();
			return body;
		}
		return "";
	}
	

	
	
	private String getCorrectStreamNew(JSONObject reply,String url) {
		FSList fslist = FSListManager.get("/domain/euscreenxl/user/*/*"); // get our collection from cache	
		System.out.println("EUSCREEN SIZE="+fslist.size());
		System.out.println("URL="+url);

		List<FsNode> nodes = fslist.getNodesFiltered(url.toLowerCase()); // find the item
		if (nodes!=null && nodes.size()>0) {
			FsNode euscreennode = (FsNode)nodes.get(0);
			System.out.println("FOUND NODE !!!! NODE="+euscreennode.getPath());
			FsNode rawvideo = Fs.getNode(euscreennode.getPath()+"/rawvideo/1");
			if (rawvideo!=null) {
				System.out.println("RAW VIDEO FOUND NODE !!!! NODE="+rawvideo.asXML());
				//return "/"+rawvideo.getProperty("mount")+euscreennode.getPath()+"/rawvideo/1/raw.mp4";
				String mount = rawvideo.getProperty("mount");
				int pos = mount.indexOf(",");
				if (pos!=-1) {
					mount = mount.substring(0,pos);
				}
				if (mount.indexOf("http://")!=-1) {
					String result = mount;
					pos = result.indexOf("/progressive/");
					if (pos!=-1) {
						result = result.substring(pos+12);
						pos = result.indexOf("/rawvideo/");
						if (pos!=-1) {
							return "http://oembed.euscreen.eu/euscreen"+result.substring(0,pos);
						}
					} else {
						return result;
					}
				} else {
					return "http://oembed.euscreen.eu/euscreen/"+mount+euscreennode.getPath();
				}
			} else {
				System.out.println("PROBLEM CAN'T FIND RAW VIDEO FOR THIS ID="+url);
			}
			
		}
		return null;
	}
	
	
}
