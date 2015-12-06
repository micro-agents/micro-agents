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

import java.net.DatagramPacket;

import org.nzdis.micro.constants.DiscoveryModes;
import org.nzdis.micro.messaging.MTRuntime;

/**
 * Handler for discovery service - holds all important constants relevant 
 * for sending of packets and processing of incoming packets. 
 * Discovery packets contain tcp port, platform ID and serialization information 
 * to match platforms and initiate connections if suitable.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.1 $ $Date: 2013/04/18 00:00:00 $
 *
 */
public class DiscoveryService {

	private static DiscoveryListener listener = null; 
	private static DiscoverySender sender = null;
	protected final static String discoveryServicePrefix = "Discovery Service: ";
	private static String discoveryMode = MTRuntime.getDiscoveryMode();
	protected final static int packetSize = 64;
	protected final static String packetPrefix = "NZDIS_MICRO";
	protected static String ownContent = MTRuntime.getTcpPort().toString();
	protected final static String delimiter = "|";
	protected static String data = new StringBuffer(packetPrefix).append(delimiter)
			.append(ownContent).append(delimiter)
			.append(MTRuntime.getPlatformID()).append(delimiter)
			.append(MTRuntime.getSerializationType()).append(delimiter).toString();
	protected static byte[] buffer = null;
	private static boolean debug = false;
	
	static{
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.net.preferIPv6Addresses", "false");
		
		buffer = new byte[DiscoveryService.packetSize];
		buffer = data.getBytes();
	}
	
	public synchronized static void startDiscovery(){
		//System.out.println("Distributed: " + MTRuntime.isDistributed()); 
		//System.out.println("Discovery :" + MTRuntime.isDiscoveryActivated());
		//System.out.println(listener);
		//System.out.println(sender);
		if(MTRuntime.isDistributed() && MTRuntime.isDiscoveryActivated() && listener == null && sender == null){
			if(listener == null){
				if(discoveryMode.equals(DiscoveryModes.MULTICAST)){
					listener = new MulticastListener(MTRuntime.getMulticastAddress(), MTRuntime.getMulticastPort());
				}
				if(discoveryMode.equals(DiscoveryModes.BROADCAST)){
					listener = new BroadcastListener(MTRuntime.getBroadcastPort());
				}
				listener.start();
			}
			
			if(sender == null){
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length, 
		                listener.getDiscoveryTargetAddress(), listener.getTargetPort());
				if(discoveryMode.equals(DiscoveryModes.MULTICAST)){
					sender = new DiscoverySender(listener, MTRuntime.getDiscoveryFrequency(), packet);
				}
				if(discoveryMode.equals(DiscoveryModes.BROADCAST)){
					sender = new DiscoverySender(listener, MTRuntime.getDiscoveryFrequency(), packet);
				}
			}
			//Discovery sender only started if indicated in configuration
			if(MTRuntime.discoveryAutostart()){
				sender.start();
			}
			
			if(debug){
				StringBuffer output = new StringBuffer(MTRuntime.getPlatformPrefix()).append("Network Discovery (Receiver");
				if(sender != null){
					output.append(", Sender");
				}
				output.append(") has been started."); 
				System.out.println(output.toString());
			}
		} else {
			//the sender.time field is set if sender thread is running
			if(MTRuntime.isDiscoveryActivated() && sender.time != null && listener != null){
				System.out.println(MTRuntime.getPlatformPrefix() + "Discovery already started.");
			} else {
				if(MTRuntime.isDiscoveryActivated() && sender.time == null){
					sender.start();
				} else {
					if(MTRuntime.isDiscoveryActivated()){
						//if user forgot to activate distributed mode and tried to start Discovery directly.
						System.out.println(MTRuntime.getPlatformPrefix() + "Discovery only starts if distributed mode and discovery is activated in configuration.");
					}
					//else distributed mode is activated but discovery not (no message necessary)
				}	
			}
		}
	}
	
	public static void pause(){
		System.out.println(MTRuntime.getPlatformPrefix() + "Network Discovery paused.");
		sender.pauseAnnouncements();
		listener.pauseDiscovery();
	}
	
	public static void resume(){
		System.out.println(MTRuntime.getPlatformPrefix() + "Network Discovery resumed.");
		sender.resumeAnnouncements();
		listener.resumeDiscovery();
	}
	
	public synchronized static void stopDiscovery(){
		if(sender != null || listener != null){
			System.out.println(MTRuntime.getPlatformPrefix() + "Network Discovery shut down.");
		}
		if(sender != null){
			sender.shutdown();
			sender = null;
		}
		if(listener != null){
			listener.shutdown();
			listener = null;
		}
	}
}
