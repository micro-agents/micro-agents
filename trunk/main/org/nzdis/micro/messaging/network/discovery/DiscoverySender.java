/*******************************************************************************
 * µ² - Micro-Agent Platform, core of the Otago Agent Platform (OPAL),
 * developed at the Information Science Department, 
 * University of Otago, Dunedin, New Zealand.
 * 
 * This file is part of the aforementioned software.
 * 
 * µ² is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * µ² is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Micro-Agents Framework.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.nzdis.micro.messaging.network.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This is the actual thread sending of discovery packets independent of 
 * Multicast or Broadcast choice.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.1 $ $Date: 2013/04/18 00:00:00 $
 * 
 */
public class DiscoverySender extends Thread {

	protected DiscoveryListener server = null;
	protected boolean running = true;
	/* indicates if discovery sender has shutdown (not executing run() loop anymore) */
	private boolean shutdown = false;
	/* sensing thread wait duration in ms */
	protected int sensingFrequency = 3000;
	/* announcement frequency in seconds */
	protected int delayInSeconds = 20;
	
	private boolean debug = false;
	
	protected DatagramPacket packet = null;
	
	protected Enumeration<NetworkInterface> nics = null;
	protected Long time = null;
	
	public DiscoverySender(DiscoveryListener listener, int frequencySec, DatagramPacket packet){
		this.server = listener;
		this.delayInSeconds = frequencySec;
		this.packet = packet;
		this.setName("DiscoverySender");
	}
	
	public void run(){
		time = System.currentTimeMillis();
		if(debug){
			System.out.println(DiscoveryService.discoveryServicePrefix + "Network announcement started - every " + delayInSeconds + " seconds.");
		}
		
		//broadcast address data structure
		HashSet<InetAddress> broadcastAdresses = null;
		HashSet<NetworkInterface> multicastNics = null;
		
		//prepare NIC for multicast
		boolean multicast = false;
		if(server.getSocket().getClass().equals(MulticastSocket.class)){
			multicast = true;
		}

		if(multicast){
			multicastNics = getNicsForValidIpAddresses();
			if(multicastNics.size() == 0){
				DiscoveryService.stopDiscovery();
				throw new RuntimeException(new StringBuffer(DiscoveryService.discoveryServicePrefix)
					.append("No network interface for Multicast discovery found.\n")
					.append("Discovery will be stopped.\n")
					.append("Switch to broadcast-based discovery and restart service.").toString());
			}
		} else {
			//broadcast preparation
			broadcastAdresses = getBroadcastIps();
			if(broadcastAdresses.size() == 0){
				DiscoveryService.stopDiscovery();
				throw new RuntimeException(new StringBuffer(DiscoveryService.discoveryServicePrefix)
					.append("No network interface for Broadcast discovery found.\n")
					.append("Discovery will be stopped.").toString());
			}
		}
		
		while(running){
			if((System.currentTimeMillis()-delayInSeconds*1000) > time){
				if(packet == null){
					throw new RuntimeException(DiscoveryService.discoveryServicePrefix + "Discovery packet is null! Discovery service not initialized properly!");
				} else {
					try {
						//Multicast discovery packet sending
						if(multicast){
							for(NetworkInterface nic: multicastNics){
								try {
									((MulticastSocket)server.getSocket()).setNetworkInterface(nic);
								} catch (SocketException e) {
									System.err.println(DiscoveryService.discoveryServicePrefix + "It seems the interface " + nic + " cannot be assigned.");
									e.printStackTrace();
								}
								server.getSocket().send(packet);
								if(debug){
									System.out.println(DiscoveryService.discoveryServicePrefix + "Sent multicast announcement via " + nic.getDisplayName());
								}
							}
						} else {
							//if not multicast -> send as Broadcast packet
							if(server.getSocket() != null){
								if(debug){
									System.out.println(DiscoveryService.discoveryServicePrefix + "Broadcast enabled on socket " + server.getSocket().getLocalAddress());
								}
								
								//byte[] broadcastAddress = getBroadcastIpBytes();
								
								for(InetAddress broadcast: broadcastAdresses){
									
								
								
								
								//if(broadcastAddress != null){
									//InetAddress bcastAddr = InetAddress.getByAddress(getBroadcastIpBytes());
									if(debug){
										System.out.println(DiscoveryService.discoveryServicePrefix + "Sent announcement to broadcast address: " + broadcast);
									}
									
									packet.setAddress(broadcast);
									server.getSocket().send(packet);
									/*if(debug){
										System.out.println(DiscoveryService.discoveryServicePrefix + "Sent broadcast announcement via " + broadcast);
									}*/
								/*} else {
									if(debug){
										System.out.println(DiscoveryService.discoveryServicePrefix + "No broadcast sent as not connected to network.");
									}*/
								}
							} else {
								System.err.println(DiscoveryService.discoveryServicePrefix + "Socket for discovery broadcast not initialized!");
							}
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				//System.out.println("Sent package to group " + server.getMulticastGroup());
				time = System.currentTimeMillis();
			} else {
				if(!running){
					shutdown = true;
				} else {
					try {
						Thread.sleep(sensingFrequency);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			if(!running){
				shutdown = true;
			}
			if(shutdown){
				if(debug){
					System.out.println(DiscoveryService.discoveryServicePrefix + "Network discovery sender has shut down.");
				}
			}
		}
	}
	
	/*
	private String getLocalIpAddress() {
		boolean validAddress = true;
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface
	                .getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf
	                    .getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                //testing if is IPv4 address - will cause exception if not
	                try{
	                	Inet4Address address = (Inet4Address)inetAddress;
	                } catch (ClassCastException e){
	                	validAddress = false;
	                }
	                if (!inetAddress.isLoopbackAddress() && validAddress) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	            validAddress = true;
	        }
	    } catch (SocketException ex) {}
	    return null;
	}*/
	
	private HashSet<NetworkInterface> getNicsForValidIpAddresses() {
		HashSet<NetworkInterface> listOfNics = new HashSet<NetworkInterface>();
		try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface
	                .getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf
	                    .getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    listOfNics.add(intf);
	                }
	            }
	        }
	        return listOfNics;
	    } catch (SocketException ex) {}
	    return null;
	}

	/**
	 * Returns the broadcast bytes of the connected network subnet.
	 * Returns null if no network can be detected.
	 */
	/*
	private byte[] getBroadcastIpBytes(){
	    String myIP = getLocalIpAddress();
	    if(myIP != null){
		    StringTokenizer tokens = new StringTokenizer(myIP, ".");
		    byte[] mask = new byte[4];
			mask[3] = (byte)255;
		    int count = 0;
		    while (count < 3) {
		    	mask[count] = (byte)Integer.parseInt(tokens.nextToken());
		        //broadcast += tokens.nextToken() + ".";
		        count++;
		    }
		    return mask;
	    } else {
	    	return null;
	    }
	}*/

	/**
	 * Returns a set of broadcast addresses for connected networks.
	 * @return HashSet<InetAddress>
	 */
	private HashSet<InetAddress> getBroadcastIps(){
		HashSet<InetAddress> listOfBroadcasts = new HashSet<InetAddress>();
	    Enumeration<NetworkInterface> list;
	    try {
	        list = NetworkInterface.getNetworkInterfaces();

	        while(list.hasMoreElements()) {
	            NetworkInterface iface = list.nextElement();

	            if(iface == null){
	            	continue;
	            }

	            if(!iface.isLoopback() && iface.isUp()) {
	                //System.out.println("Found non-loopback, up interface:" + iface);

	                Iterator<InterfaceAddress> it = iface.getInterfaceAddresses().iterator();
	                while (it.hasNext()) {
	                    InterfaceAddress address = it.next();
	                    //System.out.println("Found address: " + address);
	                    if(address == null){
	                    	continue;
	                    }
	                    InetAddress broadcast = address.getBroadcast();
	                    if(broadcast != null){
	                    	listOfBroadcasts.add(broadcast);
	                    }
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        System.err.println(new StringBuffer(DiscoveryService.discoveryServicePrefix)
	        	.append("Error while getting broadcast network interfaces").toString());
	        ex.printStackTrace();
	    }
	    return listOfBroadcasts;
	}
	
	public synchronized void pauseAnnouncements(){
		if(debug){
			System.out.println(DiscoveryService.discoveryServicePrefix + "Network announcement paused.");
		}
		try {
			this.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void resumeAnnouncements(){
		if(debug){
			System.out.println(DiscoveryService.discoveryServicePrefix + "Network announcement resumed.");
		}
		this.notify();
	}
	
	public synchronized void shutdown(){
		running = false;
		if(time != null){
			while(!shutdown){
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
