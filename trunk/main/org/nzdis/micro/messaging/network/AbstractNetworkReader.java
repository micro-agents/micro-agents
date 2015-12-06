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
package org.nzdis.micro.messaging.network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import org.nzdis.micro.events.RemotePlatformChangePropagationEvent;
import org.nzdis.micro.events.RemotePlatformLocationEvent;
import org.nzdis.micro.events.RemotePlatformShutdownEvent;
import org.nzdis.micro.events.RemotePlatformSynchronizedEvent;
import org.nzdis.micro.messaging.SocketAddress;
import org.nzdis.micro.MTConnector;
import org.nzdis.micro.MicroMessage;
import org.nzdis.micro.Role;
import org.nzdis.micro.SystemOwner;
import org.nzdis.micro.messaging.MTRuntime;
import org.nzdis.micro.messaging.message.Message;
import org.nzdis.micro.messaging.network.netty.NettyNetworkConnector;

/**
 * The AbstractNetworkReader is response for the post-processing of received
 * messages and maps them onto the according functionality on the Message Routing
 * Layer of the micro-agent platform. It is called by the network transport implementation
 * upon deserialization of the received Message.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */

public abstract class AbstractNetworkReader {
	
	/** debug switch */
	final boolean debug = true;
	
	/** holds a list of disconnecting platforms (to avoid reaction discovery messages) **/
	private static ArrayList<String> disconnectedPlatforms = new ArrayList<String>();
	
	/**
	 * Handles message from other node. Message should contain sending node address (field
	 * MicroMessage.MSG_PARAM_SENDER_ADDRESS) and port (MicroMessage.MSG_PARAM_SENDER_PORT).
	 * @param sendingNodeAddress - SocketAddress of remote node
	 * @param message - Message from other node
	 */
	public synchronized void handleMessage(SocketAddress sendingNodeAddress, Message message) throws Exception {
		//System.out.println(MTRuntime.getPlatformID() + " - Received message: " + new MicroMessage(message).toString());
		String processName = (String) message.get(MTRuntime.processSerializationKeyword);
		
		//System.out.println("Process name: "+ processName);
		boolean deliverToTarget = true;
		if(MTRuntime.isPropagating() && processName.equals(MTRuntime.platformProcess)){
			/** if message contains initialization keyword, expect bidirectional synchronization --> delete all old entries of according platform */
			//System.out.println("Propagation keyword check: "+ message.containsKey(KorusRuntime.getPropagationInitializationKeyword()));
			
			/** Do not try to deliver to a real target agent as only platform management message */
			deliverToTarget = false;
			
			/** prepare response address (which needs to hold the target port (not sending port)) */
			SocketAddress responseAddress = new SocketAddress(sendingNodeAddress.getHostAddress(), Integer.parseInt(message.get(MicroMessage.MSG_PARAM_SENDER_PORT).toString()));
			
			/** if remote platform has shutdown, ignore messages from it as long as in disconnectPlatforms list **/
			if(disconnectedPlatforms.contains(responseAddress.toString())){
				return;
			}
			
			if(message.containsKey(MTRuntime.propagationInitializationKeyword)){
				System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Received propagation initialization request from ")
						.append(responseAddress).append(" (Platform name: ").append(message.get(MTRuntime.propagationInitializationKeyword)).append(")."));
				/** add name to resolution table - will only be sent upon initialization */
				MTRuntime.addToNodeNameTable(MTRuntime.propagationInitializationKeyword, sendingNodeAddress.getHostAddress());
				/** remove existing entries for remote agents as reinitialization is requested (e.g. after broken network link) */
				MTRuntime.removeAllPropagatedRemoteProcessesOfNode(responseAddress.toString());
			} else {
				System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Received propagation from ").append(responseAddress).append(".").toString());
			}
			int countAdd = 0;
			int countDel = 0;
			Iterator<Entry<String, Object>> it = message.entrySet().iterator();
			while(it.hasNext()){
				Entry<String,Object> remoteProcessEntry = it.next();
				if(!remoteProcessEntry.getKey().equals(MTRuntime.processSerializationKeyword)){
					if(remoteProcessEntry.getValue().equals(MTRuntime.processAdditionKeyword)){
						MTRuntime.addPropagatedRemoteProcess(remoteProcessEntry.getKey(), responseAddress.toString());
						if(debug){
							System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Agent ")
									.append(remoteProcessEntry.getKey()).append(" added for remote platform ").append(responseAddress.toString()).toString());
						}
						countAdd++;
					}
					if(remoteProcessEntry.getValue().equals(MTRuntime.processRemovalKeyword)){
						MTRuntime.removePropagatedRemoteProcess(remoteProcessEntry.getKey(), responseAddress.toString());
						if(debug){
							System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Agent ")
									.append(remoteProcessEntry.getKey()).append(" deleted for remote platform ").append(responseAddress.toString()).toString());
						}
						countDel++;
					}
					//identify sent platform ID (upon initial process propagation)
					if(remoteProcessEntry.getValue().equals(MTRuntime.platformIdKeyword)){
						if(!MTRuntime.getPropagatedNodeIDs().containsKey(responseAddress.toString())){
							MTRuntime.addPropagatedNodeID(responseAddress.toString(), remoteProcessEntry.getKey());
							if(debug){
								System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Saved platform ID for remote platform ").append(responseAddress.toString()));
							}
						}
					}
				}
			}
			if(countAdd != 0 || countDel != 0){
				System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append(countAdd).append(" agents added, ")
						.append(countDel).append(" agents deleted.").toString());
				MTConnector.send(new MicroMessage(new RemotePlatformChangePropagationEvent(SystemOwner.ownName, responseAddress.toString(), countAdd, countDel)));
			}
			
			/** if node unknown or explicitly demands for resynchronization --> send own process list */
			if(!MTRuntime.getPropagatedNodes().keySet().contains(responseAddress.toString()) 
					|| message.containsKey(MTRuntime.propagationInitializationKeyword)){
				System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Sending propagation to ").append(responseAddress));
				MTRuntime.propagateProcessesToNode(responseAddress);
			}
			
			/** if node has been propagated to - but no propagation previously received, set to true */
			if(MTRuntime.getPropagatedNodes().keySet().contains(responseAddress.toString()) 
					&& !message.containsKey(MTRuntime.propagationInitializationKeyword)){
				if(!MTRuntime.getPropagatedNodes().get(responseAddress.toString())){
					MTRuntime.addPropagatedNode(responseAddress.toString(), true);
					//send event
					MTConnector.send(new MicroMessage(new RemotePlatformSynchronizedEvent(SystemOwner.ownName, responseAddress.toString())));
					//send location information (of this local platform) to remote platform which has just been synchronized
					MTConnector.sendToPlatform(responseAddress, new MicroMessage(new RemotePlatformLocationEvent(MTRuntime.platformProcess, MTRuntime.getPlatformID(), MTRuntime.getLocation())));
				}
			}
			
			/** if message contains event notification addressed to platform (e.g. location notification), 
			 *  let it pass through to MTConnector (by deleting the recipient in order to raise event)*/
			if(message.containsKey(MicroMessage.MSG_PARAM_EVENT)){
				message.remove(MicroMessage.MSG_PARAM_RECIPIENT);
				deliverToTarget = true;
			}
			
			/** shutdown of the remote platform (or network disconnection) indicated (needs to checked last to avoid propagation) */
			if(message.containsKey(MTRuntime.remotePlatformShutdownKeyword)){
				markRemotePlatformShutdown(responseAddress.toString());
				MTRuntime.purgeRemoteNodeEntries(responseAddress.toString());
				//send event
				MTConnector.send(new MicroMessage(new RemotePlatformShutdownEvent(SystemOwner.ownName, responseAddress.toString())));
			}

		}
		if(deliverToTarget){
			MicroMessage finalMessage = new MicroMessage(message);
			finalMessage.put(Message.MSG_PARAM_SENDER_NODE, sendingNodeAddress.getHostAddress());
			
			//network broadcast
			if(processName.equals(MTConnector.broadcastPrimitive) || 
					processName.equals(MTConnector.rolecastPrimitive)){
				  /*- if message contains intent (and broadcast 
					  (must be from other node then)), send it directly to potential target 
					  if intentResolutionKeyword is provided(!) (by deleting Broadcast recipient field) --> else normal broadcast 
					- same case for event (raised from remote host) */
				if((finalMessage.containsIntent() && finalMessage.containsValue(MTRuntime.intentResolutionRequestKeyword)) || finalMessage.containsEvent()){
					finalMessage.remove(MicroMessage.MSG_PARAM_RECIPIENT);
					MTConnector.send(finalMessage);
				} else {
					// else broadcast
					if(processName.equals(MTConnector.broadcastPrimitive)){
						System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Received broadcast from ").append(sendingNodeAddress));
						MTConnector.sendLocalBroadcast(finalMessage);
					}
					// or rolecast
					if(processName.equals(MTConnector.rolecastPrimitive)){
						System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Received rolecast from ").append(sendingNodeAddress));
						MTConnector.sendRolecast(finalMessage, (Role)finalMessage.get(MTRuntime.roleKeyword), false);
					}
				}
			} else {
				//regular sending	
				MTConnector.send(finalMessage);
			}
		}
	}
	
	private void markRemotePlatformShutdown(final String remoteAddress){
		disconnectedPlatforms.add(remoteAddress);
		new Thread(new Runnable(){

			@Override
			public void run() {
				System.out.println(new StringBuffer(NettyNetworkConnector.getInstance().getPrefix())
					.append("Platform ").append(remoteAddress)
					.append(" messages will be blocked as platform is shutting down.").toString());
				try {
					Thread.sleep(15000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				disconnectedPlatforms.remove(remoteAddress);
			}
			
		}).start();
	}
}
