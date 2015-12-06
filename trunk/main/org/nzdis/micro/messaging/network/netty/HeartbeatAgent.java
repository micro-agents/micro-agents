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

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.netty.channel.ChannelFuture;
import org.nzdis.micro.AbstractAgent;
import org.nzdis.micro.MicroMessage;
import org.nzdis.micro.messaging.MTRuntime;
import org.nzdis.micro.messaging.network.netty.NettyNetworkConnector;

public class HeartbeatAgent extends AbstractAgent implements Runnable{

	int heartbeatMs = 5000;
	boolean running = false;
	private static boolean debug = false;
	public static final String heartbeatAgentName = "HEARTBEAT_AGENT";
	public static final String heartbeatRequestPerformative = "HEARTBEAT_REQUEST";
	public static final String heartbeatResponsePerformative = "HEARTBEAT_RESPONSE";
	public static final String heartbeatConnectionKeyword = "HEARTBEAT_CONNECTION";
	private int toleranceFactor;
	private int toleranceTime;
	private ConcurrentHashMap<String,Long> heartbeatRegister = new ConcurrentHashMap<String,Long>();
	private Thread thread = null;
	
	public HeartbeatAgent(){
		super(heartbeatAgentName, true);
		this.activateMTSupport();
	}
	
	public static void setDebug(boolean activateDebugOutput){
		debug = activateDebugOutput;
	}
	
	public void start(){
		this.setHeartbeatTimeoutFactor(MTRuntime.getHeartbeatTimeoutFactor());
		if(!running){
			running = true;
			thread = new Thread(this);
			thread.start();
			thread.setName("Heartbeat");
		} else {
			System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Heartbeat is already running.").toString());
		}
	}
	
	protected void setHeartbeatRate(int seconds){
		
		heartbeatMs = seconds * 1000;
		if(debug){
			System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Heartbeat frequency set to ").append(heartbeatMs).append(" milliseconds.").toString());
		}
		setHeartbeatTimeoutFactor(MTRuntime.getHeartbeatTimeoutFactor());
	}
	
	protected void setHeartbeatTimeoutFactor(int factor){
		
		toleranceFactor = factor;
		toleranceTime = heartbeatMs * toleranceFactor;
		if(debug){
			System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Heartbeat timeout set to ").append(toleranceTime).append(" milliseconds.").toString());
		}
	}
	
	@Override
	public synchronized void receive(MicroMessage message){
		//print("Received message: " + message.toString());
		if(message.getPerformative().equals(heartbeatRequestPerformative)){
			if(debug){
				print(new StringBuilder("Received Heartbeat REQUEST from ").append(message.get(MicroMessage.MSG_PARAM_SENDER_NODE))
						.append(":").append(message.get(MicroMessage.MSG_PARAM_SENDER_PORT)).toString());
			}
			//MicroMessage response = message.createReply();
			MicroMessage response = new MicroMessage();
			response.setRecipient(heartbeatAgentName);
			response.setCustomField(heartbeatConnectionKeyword, message.getCustomField(heartbeatConnectionKeyword));
			response.setPerformative(heartbeatResponsePerformative);
			if(debug){
				print(new StringBuffer("Sent response ").append(response.toString())
						.append(" to message ").append(message).toString());
			}
			MTRuntime.sendRemote(message.getSenderNode(), heartbeatAgentName, message.getSenderPort(), response);
			return;
		}
		if(message.getPerformative().equals(heartbeatResponsePerformative)){
			if(debug){
				print(new StringBuilder("Received RESPONSE to Heartbeat request from " + message.get(MicroMessage.MSG_PARAM_SENDER_NODE))
				.append(":").append(message.get(MicroMessage.MSG_PARAM_SENDER_PORT)).toString());
			}
			heartbeatRegister.put(message.getCustomField(heartbeatConnectionKeyword).toString(), System.currentTimeMillis());
			return;
		}
	}

	@Override
	public void run() {
		//if(debug){
			System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Heartbeat will be initiated every ").append(heartbeatMs).append(" ms.").toString());
		//}
		while(running){
			try {
				Thread.sleep(heartbeatMs);
			} catch (InterruptedException e) {
				//e.printStackTrace();
				//Thread is to be interrupted for proper exit
			}
			if(running){
				if(!NettyNetworkConnector.getInstance().connections.isEmpty()){
					if(debug){
						System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Heartbeat sent for all active connections.").toString());
					}
					Iterator<String> connIt = NettyNetworkConnector.getInstance().connections.keySet().iterator();
					while(connIt.hasNext()){
						String stringId = connIt.next().toString();
						ConnectionID connectID = ConnectionID.inflate(stringId);
						//NettyNetworkConnector.getInstance().sendMessage(HeartbeatMessageFactory.getHeartbeatMessage(stringId), connectID.remoteAddress, Integer.parseInt(connectID.remoteServerPort.toString()));
						MTRuntime.sendRemote(connectID.remoteAddress, heartbeatAgentName, Integer.parseInt(connectID.remoteServerPort.toString()), HeartbeatMessageFactory.getHeartbeatMessage(stringId));
						if(!heartbeatRegister.containsKey(stringId)){
							//check if remote platform has propagated its agent register (not only pure network connection) before activating heartbeat
							String key = new StringBuffer(connectID.remoteAddress).append(":").append(connectID.remoteServerPort).toString();
							if(MTRuntime.getPropagatedNodes().containsKey(key) && MTRuntime.getPropagatedNodes().get(key)){
								heartbeatRegister.put(stringId, System.currentTimeMillis());
								if(debug){
									System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Added entry to Heartbeat register: ").append(stringId).toString());
								}
							}
							if(debug){
								System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Current heartbeat registry: ").append(heartbeatRegister.toString()).toString());
							}
						}
					}
				} else {
					if(debug){
						System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("No remote platform connected.").toString());
					}
				}
				//check of old conversations
				Iterator<String> it = heartbeatRegister.keySet().iterator();
				
				/*
				if(debug){
					System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Heartbeat register entries to check: ")
							.append(heartbeatRegister.toString()).toString());
					System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Platform connections to check: ")
							.append(NettyNetworkConnector.getInstance().connections.toString()).toString());
					System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Platform ID db: ")
							.append(MTRuntime.getPropagatedNodeIDs()).toString());
				}*/
				
				while(it.hasNext()){
					String tmpConnID = it.next();
					//if connection ID does not exist in the connection register, delete it from heartbeat register
					ConnectionID connectID = ConnectionID.inflate(tmpConnID);
					/*if(debug){
						System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
							.append("Connection ID: ").append(connectID.getId()).toString());
						System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
							.append("Connections registry: ").append(NettyNetworkConnector.getInstance().connections.toString()).toString());
						System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
							.append("Heartbeat registry: ").append(heartbeatRegister.toString()).toString());
					}*/
					if(!NettyNetworkConnector.getInstance().connections.containsKey(connectID.getId())){
						heartbeatRegister.remove(tmpConnID);
						if(debug){
							System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
								.append("Removed connection from heartbeat register as not active any more: ")
								.append(tmpConnID).toString());
						}
						it = heartbeatRegister.keySet().iterator();
					} else {
						//else remove expired connections
						if(debug){
							/*System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
								.append("Check on Heartbeart register: ").append(heartbeatRegister.toString()).toString());
							System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
								.append("Current time: ").append(System.currentTimeMillis()).toString());
							System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Calculation: ")
								.append((System.currentTimeMillis() - heartbeatRegister.get(tmpConnID))).toString());
							System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Tolerance: ")
								.append(toleranceTime).toString());*/
							System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
								.append("Test if time period since last response (").append((System.currentTimeMillis() - heartbeatRegister.get(tmpConnID)))
								.append(") is smaller than tolerance: ")
								.append(((System.currentTimeMillis() - heartbeatRegister.get(tmpConnID)) < toleranceTime)).toString());
						}
						if((System.currentTimeMillis() - heartbeatRegister.get(tmpConnID)) < toleranceTime){
							//is connected
							if(debug){
								System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Connection ")
										.append(tmpConnID).append(" is connected.").toString());
							}
						} else {
							//not connected anymore
							ConnectionID connID = ConnectionID.inflate(tmpConnID);
							//String remoteDisconnectedPlatform = new StringBuffer(connID.remoteAddress).append(connID.remoteServerPort).toString();
							if(debug){
								System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
									.append("Connection to platform " + tmpConnID + " has been lost.").toString());
							}
							ChannelFuture future = NettyNetworkConnector.getInstance().connections.remove(connID);
							future.getChannel().close();
							//MTRuntime.purgeRemoteNodeEntries(remoteDisconnectedPlatform);
							//send event
							//MTConnector.send(new MicroMessage(new RemotePlatformConnectionLostEvent(MTRuntime.platformProcess, remoteDisconnectedPlatform)));
						}
					}
				}
				if(debug){
					System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Number of active connections after check iteration: ")
							.append(NettyNetworkConnector.getInstance().connections.size()).toString());
				}
			}
		}
		//if(debug){
			System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Heartbeat has been shutdown.").toString());
		//}
	}
	
	public synchronized void shutdown(){
		if(debug){
			System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Heartbeat shutdown requested.").toString());
		}
		if(running){
			running = false;
			if(thread != null){
				thread.interrupt();
			} else {
				System.err.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Heartbeat thread not properly assigned.").toString());
			}
		}
		die();
	}
	
}
