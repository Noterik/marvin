package org.springfield.marvin;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.*;

import org.springfield.fs.FSList;
import org.springfield.fs.FSListManager;
import org.springfield.fs.Fs;
import org.springfield.fs.FsNode;

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
		int pos = url.indexOf("http://www.euscreen.eu/item.html");
		if (pos!=-1) {
			url=url.substring(pos+36);
			body = getEUScreenBody(request,name,url,format, response);
		} else {
			pos = url.indexOf("/euscreen/");
			if (pos!=-1) {
				url=url.substring(pos+10);
				body = getEUScreenBody(request,name,url,format, response);
			} else if (type != null && type.equals("euscreen")) {
				body = getEUScreenBody(request,name,url,format, response);
			} else {
			    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			    return;
			}
		}
		
		if (body == null) {
		    response.setStatus(HttpServletResponse.SC_NOT_FOUND);		    
		    return;
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

	public String getEUScreenBody(HttpServletRequest request,String name,String url,String format, HttpServletResponse response) {
	    if (format.equals("json")) {
		FsNode euscreenItem = getFsNode(url);
		    
		if (euscreenItem == null) {
		    return null;
		}	
		
		String type = "video";
		int width = 640;
		int height = 480;
		
		if (euscreenItem.getPath().contains("/audio/")) {
		    type = "audio";
		    width = 0;
		    height = 0;
		}
		    
		String domainurl = getCorrectStreamNew(euscreenItem, type);
		System.out.println("DOMAINURL2="+domainurl);
		    
		if (domainurl == null) {
		    return null;
		}
		
		JSONObject reply= new JSONObject();
		reply.put("version","1.0");
		reply.put("type", type);
		reply.put("provider_name", euscreenItem.getProperty("provider"));
		reply.put("title", euscreenItem.getProperty("TitleSet_TitleSetInEnglish_title"));
		reply.put("width", width);
		reply.put("height", height);
			
		String referer=request.getHeader("Referer");
		System.out.println("MARVIN REFER="+request.getHeader("Referer"));
			
		Boolean valid = false;

		/*
		if (referer.startsWith("https://oembed.euscreen.eu")) {
		}
		*/
		valid = true;
	
		if (valid) {
		    String random = ""+generator.nextInt(999999999);
		    String ticket = "mst_marvin_"+random;
		    FsNode newticket = new FsNode("ticket",ticket);
		    newticket.setProperty("expire",""+(new Date().getTime()/1000)+5);
		    newticket.setProperty("url",""+domainurl);
		    Fs.insertNode(newticket,"/domain/oembed/service/marvin/");
		    String html = "<iframe width=\"640\" height=\"480\" src=\"https://oembed.euscreen.eu/oembed?url="+domainurl+"&ticket="+ticket+"\"></iframe>";
		    reply.put("html", html);
		} else {
		    String html = "<iframe width=\"640\" height=\"480\" src=\"https://oembed.euscreen.eu/oembed?url="+domainurl+"\"></iframe>";
		    reply.put("html", html);
		}
		String body=reply.toString();
		return body;
	    }
	    return null;
	}
	
	private FsNode getFsNode(String url) {
	    FSList fslist = FSListManager.get("/domain/euscreenxl/user/*/*"); // get our collection from cache	
	    System.out.println("EUSCREEN SIZE="+fslist.size());
	    System.out.println("URL="+url);
	    
	    List<FsNode> nodes = fslist.getNodesFiltered(url.toLowerCase()); // find the item
	    if (nodes!=null && nodes.size()>0) {
		FsNode euscreennode = (FsNode)nodes.get(0);
		System.out.println("FOUND NODE !!!! NODE="+euscreennode.getPath());
			
		return euscreennode;
		
	    } else {
		return null;
	    }
	}

	private String getCorrectStreamNew(FsNode euscreenItem, String type) {
	    FsNode rawnode = Fs.getNode(euscreenItem.getPath()+"/raw"+type+"/1");
	    
	    if (rawnode!=null) {
		System.out.println("RAW FOUND NODE !!!! NODE="+rawnode.asXML());

		String mount = rawnode.getProperty("mount");
		int pos = mount.indexOf(",");
		if (pos!=-1) {
		    mount = mount.substring(0,pos);
		}
		if (mount.indexOf("http://")!=-1) {
		    String result = mount;
		    pos = result.indexOf("/progressive/");
		    if (pos!=-1) {
			result = result.substring(pos+12);
			pos = result.indexOf("/raw"+type+"/");
			if (pos!=-1) {
			    return "https://oembed.euscreen.eu/euscreen"+result.substring(0,pos);
			}
		    } else {
			return result;
		    }
		} else {
		    return "https://oembed.euscreen.eu/euscreen/"+mount+euscreenItem.getPath();
		}
	    } else {
		System.out.println("PROBLEM CAN'T FIND RAW FOR THIS ID="+euscreenItem.getPath());
	    }		    
	    return null;
	}	
}
