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
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.nzdis.micro.messaging.MTRuntime;
import org.nzdis.micro.messaging.SocketAddress;

/**
 * DiscoveryListener is the abstract listener implementation of the
 * network discovery mechanism. Processing actual incoming packets 
 * independent of Multicast or Broadcast operation.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.1 $ $Date: 2013/04/18 00:00:00 $
 * 
 */
public abstract class DiscoveryListener extends Thread {

	protected String address = "225.0.0.1";
	protected InetAddress targetAddress = null;
	protected int port = 4444;
	protected boolean running = true;
	/* indicates if discovery listener has shutdown (not executing run() loop anymore) */
	protected boolean shutdown = false;
	protected HashSet<String> localIps = new HashSet<String>();
	protected DatagramSocket socket = null;
	private DatagramPacket packet = null;
	private byte[] buf = null;
	private final boolean debug = false;
	protected final int socketTimeout = 4000;

	protected void readLocalAddresses(){
		Enumeration<NetworkInterface> nics = null;
		try {
			nics = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		NetworkInterface tempNic = null;
		while(nics.hasMoreElements()){
			tempNic = nics.nextElement();
			Enumeration<InetAddress> tempAddr = tempNic.getInetAddresses();
			while(tempAddr.hasMoreElements()){
				InetAddress tempAddress = tempAddr.nextElement();
				String finalAddress = tempAddress.getHostAddress().toString();
				int percMarker = tempAddress.getHostAddress().toString().indexOf("%");
				//System.out.println("Testing local address on percent: " + tempAddress);
				if(percMarker != -1){
					finalAddress = tempAddress.getHostAddress().toString().substring(0, percMarker);
					//System.out.println("Found percent, new address: " + finalAddress);
				}
				//System.out.println("Final address: " + finalAddress);
				localIps.add(finalAddress);
			}
		}
	}
	
	public InetAddress getDiscoveryTargetAddress() {
		return targetAddress;
	}
	
	public int getTargetPort(){
		return port;
	}
	
	void actionOnRemotePackage(DatagramPacket packet, String port, String platformID) {
		SocketAddress initTarget = new SocketAddress(packet.getAddress().getHostAddress(), Integer.parseInt(port));
		//only propagate if discovery or propagation not only in process and platform unknown
		if(!MTRuntime.getPropagatedNodes().keySet().contains(initTarget.toString())
				&& !MTRuntime.getPropagatedNodeIDs().containsValue(platformID)){
			MTRuntime.addPropagatedNodeID(initTarget.toString(), platformID);
			MTRuntime.initiatePropagationWithNode(packet.getAddress().getHostAddress(), Integer.parseInt(port), 20000);
		}
	}


	void actionOnLocalPackage(DatagramPacket packet, String port) {
		SocketAddress sAddress = new SocketAddress(packet.getAddress().getHostAddress(), Integer.parseInt(port));
		SocketAddress tgtAddress = sAddress;
		Iterator<String> it = localIps.iterator();
		boolean success = false;
		while(it.hasNext()){
			String tempTarget = it.next();
			tgtAddress.setHostAddress(tempTarget);
			if(MTRuntime.getPropagatedNodes().keySet().contains(tgtAddress.toString())){
				//System.out.print(MTRuntime.getPlatformPrefix() + tgtAddress.toString() + " is included in " + MTRuntime.getPropagatedNodes().toString());
				success = true;
			}
		}
		if(!success){
			//System.out.println(MTRuntime.getPlatformPrefix() + tgtAddress.toString() + " not contained in " + MTRuntime.getPropagatedNodes().toString());
			MTRuntime.initiatePropagationWithNode(packet.getAddress().getHostAddress(), Integer.parseInt(port), 20000);
		}
		
	}
	
	public void pauseDiscovery(){
		if(debug){
			System.out.println(DiscoveryService.discoveryServicePrefix + "Network announcement paused.");
		}
		try {
			this.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void resumeDiscovery(){
		if(debug){
			System.out.println(DiscoveryService.discoveryServicePrefix + "Network announcement resumed.");
		}
		this.notify();
	}
	
	public void shutdown(){
		running = false;
		while(!shutdown){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		socket.close();
	}

	public DatagramSocket getSocket(){
		return socket;
	}
	
	public void run(){
		boolean timeOut = false;
		while(running){
			buf = new byte[DiscoveryService.packetSize];
			packet = new DatagramPacket(buf, buf.length);

			try {
				socket.receive(packet);
			} catch (IOException e) {
				if(e.getClass().equals(SocketTimeoutException.class)){
					timeOut = true;
				} else {
					e.printStackTrace();
				}
			}
			
			//if(debug){
				//System.out.println("Discovery: Timeout: " + timeOut);
				//System.out.println("Discovery: Packet: " + packet.toString());
			//}
			
			//if socket receive has not timed out (and received packet instead), process the packet
			if(!timeOut){
				String targetPort = null;
				String packetPlatformID = null;
				String serializationType = null;
				boolean process = false;
				StringTokenizer tok = new StringTokenizer(new String(packet.getData()), DiscoveryService.delimiter);
				if(tok.hasMoreTokens()){
					//check on packet prefix (must be equal, else other application (presumably)
					if(tok.nextToken().equals(DiscoveryService.packetPrefix)){
						//check other tokens to figure out if different or same platform
						if(tok.hasMoreTokens()){
							//save target port
							targetPort = tok.nextToken();
							//save platformID
							packetPlatformID = tok.nextToken();
							//save serialization
							serializationType = tok.nextToken();
							
							//System.out.println("Local platform ID:    " + MTRuntime.getPlatformID());
							//System.out.println("Received platform ID: " + packetPlatformID);
							
							//check if not own platform ID --> if own, ignore packet
							if(packetPlatformID != null && !packetPlatformID.equals(MTRuntime.getPlatformID())){
								if(debug){
									System.out.println(new StringBuffer(DiscoveryService.discoveryServicePrefix)
										.append("Received packet from OTHER platform (").append("ID: ")
										.append(packetPlatformID).append(", IP: ").append(packet.getAddress().getHostAddress())
										.append(", Port: ").append(targetPort).append(")").toString());
								}
								if(serializationType.equals(MTRuntime.getSerializationType())){
									process = true;
								} else {
									//different platform but also different serialization - no connection establishment
									if(debug){
										//only short output, because we just had discovery output
										System.out.println(new StringBuffer(DiscoveryService.discoveryServicePrefix)
											.append("Other platform ").append(packetPlatformID)
											.append(" is running different serialization: ").append(serializationType)
											.append(" - my serialization: ").append(MTRuntime.getSerializationType()));
									} else {
										//long output if debug is not enabled to give user more context
										System.out.println(new StringBuffer(DiscoveryService.discoveryServicePrefix)
												.append("Received packet from OTHER platform (").append("ID: ")
												.append(packetPlatformID).append(", IP: ").append(packet.getAddress().getHostAddress())
												.append(", Port: ").append(targetPort).append(")").append(MTRuntime.LINE_DELIMITER)
												.append(" == but different serialization: ")
												.append(serializationType).append(" - my serialization: ").append(MTRuntime.getSerializationType()));
									}
								}
							} else if(packetPlatformID != null && packetPlatformID.equals(MTRuntime.getPlatformID())){
								if(debug){
									System.out.println(new StringBuffer(DiscoveryService.discoveryServicePrefix)
										.append("Received packet from OWN platform (").append("ID: ")
										.append(packetPlatformID).append(", IP: ").append(packet.getAddress().getHostAddress())
										.append(", Port: ").append(targetPort).append(")").toString());
								}
								process = false;
							}
						}
					}
				}
				if(debug){
					//System.out.println(DiscoveryService.discoveryServicePrefix + "Received packet: " + new String(packet.getData()) + " from " + packet.getAddress().getHostAddress());
				}
				
				//System.out.println("Local IPs: " + localIps.toString());
				//System.out.println("Received packet's host: " + packet.getAddress().getHostAddress());
				
				if(localIps.contains(packet.getAddress().getHostAddress())){
					if(process){
						if(debug){
							System.out.println(DiscoveryService.discoveryServicePrefix + "Received discovery packet from own host but different port - " + targetPort);
						}
						actionOnLocalPackage(packet, targetPort);
					}
					//System.out.println("Own address: " + packet.getAddress());
				} else {
					if(process){
						if(debug){
							System.out.println(DiscoveryService.discoveryServicePrefix + "Received discovery packet from host " + packet.getAddress());
						}
						actionOnRemotePackage(packet, targetPort, packetPlatformID);
					}
				}
			}
			//reset timeout
			timeOut = false;
			
			//handle shutdown properly
			if(!running){
				shutdown = true;
			}
			if(shutdown){
				if(debug){
					System.out.println(DiscoveryService.discoveryServicePrefix + "Network discovery listener has shut down.");
				}
			}
		}
	}
}
