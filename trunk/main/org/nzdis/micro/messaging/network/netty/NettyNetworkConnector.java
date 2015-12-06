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
package org.nzdis.micro.messaging.network.netty;

import org.nzdis.micro.MTConnector;
import org.nzdis.micro.MicroMessage;
import org.nzdis.micro.constants.SerializationTypes;
import org.nzdis.micro.events.LocalPlatformNetworkStartFailedEvent;
import org.nzdis.micro.events.LocalPlatformNetworkStartedEvent;
import org.nzdis.micro.messaging.MTRuntime;
import org.nzdis.micro.messaging.message.Message;
import org.nzdis.micro.messaging.network.*;
import org.nzdis.micro.messaging.network.netty.object.ObjectReader;
import org.nzdis.micro.messaging.network.netty.object.ObjectWriter;
import org.nzdis.micro.messaging.network.netty.string.StringReader;
import org.nzdis.micro.messaging.network.netty.string.StringWriter;

/**
 * NettyNetworkConnector manages the Netty-based network transport for the micro-agent
 * platform. 
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public class NettyNetworkConnector extends AbstractNetworkReader implements NetworkConnectorInterface {

	private final String nettyPrefix = new StringBuilder(MTRuntime.getPlatformPrefix()).append("Network Transport: ").toString();
	//private static boolean debug = false;
	private static String serialization = SerializationTypes.XML;
	private AbstractReader reader = null;
	private AbstractWriter writer = null;
	private HeartbeatAgent heartbeat;
	private static boolean networkStarted = false;
	//indicates if network is currently shutting down
	private static boolean networkShuttingDown = false;
	//if something goes wrong, a shutdown - immediatey after start - can be requested 
	private static boolean shutdownImmediatelyAfterStart = false;
	public MonitoredConnection connections = new MonitoredConnection();
	private int sendError = 0;
	//used to encapsulate the target node for a message sent within the message
	public static final String TARGET_NODE_KEYWORD = MTRuntime.nodeKeyword;
	public static final String TARGET_PORT_KEYWORD = "TARGET_PORT_KEYWORD";
	//indicates message field holding local connection address (for Netty Channel)
	public static final String SENDER_LOCAL_ADDRESS = "SENDER_LOCAL_ADDRESS";
	
	NettyNetworkConnector(){
	
	}
	 
	private static class InstanceHolder {
	    public static NettyNetworkConnector instance = new NettyNetworkConnector();
	}
	
	public static NettyNetworkConnector getInstance() {
		return InstanceHolder.instance;
	}
	
	public String getSerialization(){
		return serialization;
	}
	
	@Override
	public void setSerialization(String serial){
		serialization = serial;
	}
	
	@Override
	public String getPrefix(){
		return nettyPrefix;
	}
	
	@Override
	public void sendMessage(Message message, String host, int port){
		if(!networkStarted){
			startNetwork();
		}
		
		if(writer != null && writer.isAlive() && !networkShuttingDown){
			writer.sendMessage(message, host, port);
		}
	}
	
	@Override
	public synchronized void startNetwork(){
		if(!networkStarted && !networkShuttingDown){
			if(serialization.equals(SerializationTypes.JAVA) || serialization.equals(SerializationTypes.JAVA_COMPATIBILITY)){
				//enforce compatibility serialization if running on Android
				if(System.getProperty("java.vm.name").equals("Dalvik") || serialization.equals(SerializationTypes.JAVA_COMPATIBILITY)){
					ObjectWriter.compatibilityMode = true;
				}
				if(writer == null){
					writer = new ObjectWriter();
					writer.start();
				}
				if(reader == null){
					reader = new ObjectReader();
				}
			}
			if(serialization.equals(SerializationTypes.XML) || serialization.equals(SerializationTypes.JSON)){
				//enforce XML serialization if running on Android
				if(System.getProperty("java.vm.name").equals("Dalvik") && serialization.equals(SerializationTypes.XML)){
					serialization = SerializationTypes.XML;
					System.out.println(new StringBuilder(nettyPrefix).append("Enforced serialization type ")
							.append(serialization).append(" as JSON for this platform is not yet available on Android."));
				}
				if(writer == null){
					writer = new StringWriter();
					if(writer.initialized){
						writer.start();
					}
				}
				if(reader == null){
					reader = new StringReader();
				}
			}
			
			if(writer == null || reader == null){
				System.err.println(new StringBuilder(nettyPrefix).append("Please select a valid serialization type (Current setting: ")
						.append(serialization).append(").\n").append("Network start will be aborted.").toString());
			}
			
			if(writer != null && reader != null && writer.initialized && reader.initialized){
				networkStarted = true;
				//Heartbeat agent will be started once reader and writer are available
				heartbeat = new HeartbeatAgent();
				heartbeat.setHeartbeatRate(MTRuntime.getHeartbeatFrequency());
				if(MTRuntime.isHeartbeatActivated()){
					//but only if explicitly activated it will be actively checking connections on the local platform
					heartbeat.start();
				}
				MTConnector.send(new MicroMessage(new LocalPlatformNetworkStartedEvent(MTConnector.platformProcess)));
				//System.out.println(new StringBuilder(nettyPrefix).append("Network started."));
				//if(debug){
					System.out.println(new StringBuilder(nettyPrefix).append("Netty network started on port " + getPort() + " with " + serialization + " serialization."));
				//}
			} else {
				MTConnector.send(new MicroMessage(new LocalPlatformNetworkStartFailedEvent(MTConnector.platformProcess)));
				System.err.println(new StringBuilder(nettyPrefix).append("Netty network support start with " + serialization + " serialization failed."));
				shutdownImmediatelyAfterStart = true;
				shutdown();
			}
		} else {
			System.out.println(new StringBuilder(nettyPrefix).append("Requested network start although already running."));
		}
	}
	
	@Override
	public boolean networkStarted(){
		return networkStarted && !networkShuttingDown;
	}

	public void setSendError(Message message) {
		sendError++;
		System.err.println(new StringBuilder(nettyPrefix).append("Error when sending message to node ").append(message.get(MTRuntime.nodeKeyword))
				.append("! Message: ").append(message.toString()));
	}
	
	public int getNumberOfSendErrors(){
		return sendError;
	}

	@Override
	public Integer getPort() {
		return MTRuntime.getTcpPort();
	}

	@Override
	public void shutdown() {
		if((networkStarted || shutdownImmediatelyAfterStart) && !networkShuttingDown){
			
			networkShuttingDown = true;
			//Heartbeat agent shutdown
			if(heartbeat != null){
				heartbeat.shutdown();
				heartbeat = null;
			}
			
			//Network writer shutdown
			System.out.println(new StringBuilder(nettyPrefix).append("Shutting down writer ..."));
			if(writer != null){
				writer.shutdown();
				writer = null;
			}
			
			//Network reader shutdown
			System.out.println(new StringBuilder(nettyPrefix).append("Shutting down reader ..."));
			if(reader != null){
				reader.shutdown();
				reader = null;
			}
			
			System.out.println(new StringBuilder(nettyPrefix).append("Network has been shut down."));
			networkStarted = false;
			shutdownImmediatelyAfterStart = false;
			networkShuttingDown = false;
			
		} /*else {
			System.err.println(new StringBuilder(nettyPrefix).append("Network will not be shut down as not fully started."));
		}*/
	}
}
