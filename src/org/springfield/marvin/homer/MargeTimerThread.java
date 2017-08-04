/* 
* MargeTimerThread.java
* 
* Copyright (c) 2014 Noterik B.V.
* 
* This file is part of Marvin, related to the Noterik Springfield project.
*
* Marvin is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Marvin is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Marvin.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.springfield.marvin.homer;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.dom4j.Node;


public class MargeTimerThread extends Thread {
	
	private static final Logger LOG = Logger.getLogger(MargeTimerThread.class);
	private Map<MargeObserver,String> observers = new HashMap<MargeObserver,String>();
	
	private int counter = 0;
	private boolean running = false;
	
	public MargeTimerThread() {
		super("margetimerthread");
		running = true;
		start();
	}
	
	public void addTimedObserver(String url,int counter,MargeObserver o) {
		observers.put(o,url+","+counter);
	}
	
	public void run() {
		while (running) {
			try {	
				sleep(10000);
				LazyHomer.setLastSeen();
				for(Iterator<MargeObserver> iter = observers.keySet().iterator(); iter.hasNext(); ) {
					MargeObserver obs = (MargeObserver)iter.next();
					String url = observers.get(obs);
					obs.remoteSignal("localhost","GET", url);
				}
				counter++;
			} catch(InterruptedException i) {
				if (!running) break;
			} catch(Exception e) {
					LOG.info("ERROR MargeTimerThread");
					//e.printStackTrace();
			}
		}
	}
	
    /**
     * Shutdown
     */
	public void destroy() {
		running = false;
		this.interrupt(); // signal we should stop;
	}

}
