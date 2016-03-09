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
package org.nzdis.micro;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import org.nzdis.micro.constants.MessagePassingFrameworks;
import org.nzdis.micro.events.LocalPlatformShutdownEvent;
import org.nzdis.micro.events.RemotePlatformLocationEvent;
import org.nzdis.micro.gip.GenericIntentProcessor;
import org.nzdis.micro.gip.annotations.GenericIntentInterface;
import org.nzdis.micro.messaging.MTRuntime;
import org.nzdis.micro.messaging.MessageCommunicator;
import org.nzdis.micro.messaging.SocketAddress;
import org.nzdis.micro.messaging.message.Message;
import org.nzdis.micro.util.StackTracePrinter;

/**
 * The MTConnector (or MessageTransportConnector) links the agent implementation with the message 
 * passing framework sending side. It handles registration, sending of goal messages and events
 * transparently using the underlying messaging framework. Not actual message passing is involved
 * in this class but rather filtering invalid messages as well as directing messages containing
 * goals and events.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a>
 * @version $Revision: 1.0 $ $Date: 2013/03/01 00:00:00 $
 *
 */

public class MTConnector extends MTRuntime {

	/** 
	 * Predefined date format for time output 
	 */
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	/**
	 * Predefined time format for time output 
	 */
	public static final String TIME_FORMAT_NOW = "HH:mm:ss";

	
	/**
	 * Registers agent on Message Transport Service.
	 * @param agentName Agent name to be registered
	 * @param agent Agent instance associated with the name
	 */
	public static void register(String agentName, MessageCommunicator agent){
		MTRuntime.register(agentName, agent);
	}
	
	/**
	 * Unregisters agent from Message Transport Service and clears
	 * all role and intent registrations as well as event subscriptions.
	 * @param agentName Name of agent to be unregistered
	 */
	public synchronized static void unregister(String agentName) {
		MTRuntime.unregister(agentName);
		clearApplicableIntents(agentName);
		clearEventSubscriptions(agentName);
		if(platformOutputLevel > 0){
			System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append(agentName).append(" unregistered successfully."));
		}
	}

	/**
	 * Sends a message to the recipient based on either recipient field, else by automatic
	 * determining the recipient(s) based on intent or event objects.
	 * Semantics of the filtering process:
	 * If message recipient is supplied the message is directed to this, independent from
	 * further fields.
	 * If message contains a intent, the first agent holding a role for which the intent is
	 * applicable will be addressed.
	 * If message contains an event, the message is further filtered on the related performative
	 * for subscription (SUBSCRIBE), desubscription (UNSUBSCRIBE) from this specific event class, 
	 * clearing of all subscriptions (CLEAR_SUBSCRIPTIONS) for the sender (independent from event
	 * class passed). If those performatives do not match the event is considered to be raised and 
	 * all subscribed agents are notified.
	 * 
	 * @param message Message to be sent
	 */
	public synchronized static void send(MicroMessage message){
		//Multi-level check (if agent name is locally available --> send to local agent)
		
		boolean error = false;
		boolean noSending = false;
		
		/** if message has already been handled (sent) by this node, no handling - could be looped message from other host
		  * only exception are NOT_UNDERSTOOD message which are responses to previous requests. */
		if(message.messageAlreadyHandledbyLocalNode() && !message.getPerformative().equals(MicroMessage.NOT_UNDERSTOOD)){
			System.err.println("Message ignored as already handled by local node.");
			return;
		}
		
		//System.out.println(MTRuntime.getPlatformPrefix() + "Platform: " + getPlatformID() + "Incoming message: " +  message);
		//System.out.println(MTRuntime.getPlatformPrefix() + " contains intent: " + message.containsGoal());
		
		//automatic recipient determination by intent and event (if recipient field is empty)
		if(!message.containsRecipient()){
			/* INTENT CHECKS
			 * check for intent first - if intent exists, no check for event!
			 */
			if(message.containsIntent()){
				//check on generic intents
				if(message.containsGenericIntent()){
					//start generic intent processor if not already running
					if(!registeredIntents.containsKey(GenericIntentInterface.class)){
						SystemAgentLoader.newAgent(new GenericIntentProcessor(), GENERIC_INTENT_PROCESSOR_NAME);
					}
					message.setRecipient(registeredIntents.get(GenericIntentInterface.class).getFirst().toString());
				} else {
					//else check on conventional intents
					if(registeredIntents.containsKey(message.getIntent().getClass())){
						message.setRecipient(registeredIntents.get(message.getIntent().getClass()).getFirst().toString());
					} else {
						/* if is distributed send broadcast to other nodes 
						 * (if not coming from another node as this would cause indefinite ping pong) */
						if(isDistributed && !message.containsKey(MicroMessage.MSG_PARAM_SENDER_NODE)){
							
							if(getPropagatedNodes().size() > 0){
								//add keyword indicating the necessary intent resolution on target host rather than actual broadcast
								message.put(intentResolutionKeyword, intentResolutionRequestKeyword);
								notifySender(message, MessageFields.INFORM_MESSAGE, 4, true, true);
								sendGlobalBroadcast(message, false);
							} else {
								notifySender(message, MessageFields.ERROR_MESSAGE, 3, true, true);
							}
							error = true;
							noSending = true;
						} else {
							//if contains sender node information, it is remote
							if(message.containsKey(MicroMessage.MSG_PARAM_SENDER_NODE)){
								message.put(intentResolutionKeyword, intentResolutionFailedKeyword);
								notifySender(message, MessageFields.ERROR_MESSAGE, 7, true, false);
								sendRemote(message.get(Message.MSG_PARAM_SENDER_NODE).toString(), message.getRecipient(),
										Integer.parseInt(message.get(Message.MSG_PARAM_SENDER_PORT).toString()), message);
								return;
							} else {
								//local message
								notifySender(message, MessageFields.ERROR_MESSAGE, 3, true, true);
							}
							error = true;
							noSending = true;
						}
					}
				}
			} else {
				/* EVENT CHECKS
				 * if no intent field available hunt for event 
				 */
				if(message.containsEvent()){ 
					//consider as RAISED event
					if(eventSubscriptions.containsKey(message.getEvent().getClass()) && !noSending){
						Iterator<String> it = eventSubscriptions.get(message.getEvent().getClass()).iterator();
						while(it.hasNext()){
							message.setRecipient(it.next());
							definiteSend(message);
							//System.out.println(MTRuntime.getPlatformPrefix() + "Sent message " + message.toString());
						}
					} else {
						if(!noSending){
							// do nothing as nobody needs to be subscribed to event
							//System.out.println(MTRuntime.getPlatformPrefix() + "No subscriptions for event found. (Message: " + message.toString());
						}
					}
					//send event to remote host if indicated by event and message from local machine (else ping pong effect)
					if(((Event)message.getEvent()).raiseRemoteEvent && isDistributed && !noSending && !message.containsKey(MicroMessage.MSG_PARAM_SENDER_NODE)){
						sendGlobalBroadcast(message, false);
					}
					noSending = true;
					//no error but no further processing should be done
					error = true;
					
					//handling of location event from other platform (just print off)
					if(message.getEvent().getClass().equals(RemotePlatformLocationEvent.class)){
						RemotePlatformLocationEvent event = (RemotePlatformLocationEvent)message.getEvent();
						System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Connected remote platform '").append(event.getPlatformId())
							.append("' has location ").append(event.getLocation().toString()).toString());
					}
				}
			}
		}
		
		//if recipient not found locally, try remote before returning error
		if(!MTRuntime.getRegisteredAgents().containsKey(message.getRecipient()) && !error){
			if(MTRuntime.isDistributed){
				
				//if found in remote node table --> send there
				if(MTRuntime.getRemoteProcessMap().containsKey(message.getRecipient())){
					//using first entry in case of multiple agents
					String targetAddress = MTRuntime.getRemoteProcessMap().get(message.getRecipient()).get(0);
					SocketAddress addr = SocketAddress.inflate(targetAddress);
					MTRuntime.sendRemote(addr.getHostAddress(), message.getRecipient(), addr.getPort(), message);
				} else {
					//try to send directly if platform name or ip is provided (pattern: agent@platformName(:port))
					if(message.getRecipient().contains("@")){
						StringTokenizer t = new StringTokenizer(message.getRecipient(), "@");
						if(t.countTokens() == 2){
							String agent = t.nextToken();
							String address = t.nextToken();
							// per default assume same remote port as local port
							String port = MTRuntime.getTcpPort().toString();
							//address contains port information, split this as well
							if(address.contains(":")){
								StringTokenizer t2 = new StringTokenizer(address, ":");
								if(t2.countTokens() == 2){
									address = t2.nextToken();
									port = t2.nextToken();
								}
							}
							//try to resolve real host address (in case of platform name)
							if(MTRuntime.getNodeNameTable().containsKey(address)){
								address = MTRuntime.getNodeNameTable().get(address);
							}
							if(address == null){
								notifySender(message, MessageFields.ERROR_MESSAGE, 2, true, true);
							} else {
								//blind sending
								notifySender(message, MessageFields.INFORM_MESSAGE, 5, true, true);
								MTRuntime.sendRemote(address, agent, Integer.parseInt(port), message);
							}
						} else {
							notifySender(message, MessageFields.ERROR_MESSAGE, 2, true, true);
						}
					}
					//System.err.println(MTRuntime.getPlatformPrefix() + message.getSender() + " sent message to unregistered recipient (neither local nor global) "+ message.getRecipient());
					notifySender(message, MessageFields.ERROR_MESSAGE, 2, true, true);
				}
			} else {
				//System.out.println("Message: " + message.toString());
				//System.err.println(MTRuntime.getPlatformPrefix() + message.getSender() + " Sent message to unregistered recipient "+ message.getRecipient());
				notifySender(message, MessageFields.ERROR_MESSAGE, 1, true, true);
			}
		} else {
			if(!noSending){
				definiteSend(message);
			}
		}
	}
	
	/**
	 * Sends a message to a remote platform agent of a platform identified
	 * via the (runtime) platform ID. Be aware that this ID changes with
	 * every restart of a platform. An alternative is sendToPlatform(SocketAddress, MicroMessage).
	 * @param platformID Current platform ID
	 * @param message MicroMessage to be sent to platform agent
	 */
	public static void sendToPlatform(String platformID, MicroMessage message){
		if(MTRuntime.getPropagatedNodeIDs().containsValue(platformID)){
			Iterator<String> it = MTRuntime.getPropagatedNodeIDs().keySet().iterator();
			String key = "";
			while(it.hasNext()){
				key = it.next();
				if(platformID.equals(MTRuntime.getPropagatedNodeIDs().get(key))){
					break;
				}
			}
			if(!key.equals("")){
				sendToPlatform(SocketAddress.inflate(key), message);
				return;
			}
		}
		message.put(platformIdKeyword, platformID);
		notifySender(message, MessageFields.ERROR_MESSAGE, 6, true, true);
	}
	
	/**
	 * Sends message to platform identified via network address.
	 * @param address SocketAddress identifying network address
	 * @param message MicroMessage to be sent to platform agent of the according platform
	 */
	public static void sendToPlatform(SocketAddress address, MicroMessage message){
		message.setRecipient(MTRuntime.platformProcess);
		message.setSender(MTRuntime.platformProcess);
		//System.out.println(MTRuntime.getPlatformPrefix() + "Will send message to platfrom " + address + ", Message " + message);
		MTRuntime.sendRemote(address.getHostAddress(), MTRuntime.platformProcess, address.getPort(), message);
	}
	
	/**
	 * private method for actual sending of message after validation and filtering for goals as well as events.
	 * @param message Message to be sent (definitely)
	 */
	private static void definiteSend(MicroMessage message){
		if(MicroMessage.globalValidation || message.validation){
			validateMessage(message);
		}
		MTRuntime.send(message);
	}
	
	/**
	 * Broadcasts a message locally (i.e. on local platform)
	 * @param message Message to be sent
	 */
	public static void sendLocalBroadcast(MicroMessage message){
		if(MicroMessage.globalValidation || message.validation){
			validateMessage(message);
		}
		MTRuntime.sendLocalBroadcast(message);
	}
	
	/**
	 * Broadcasts a message globally (i.e. across all connected distributed nodes)
	 * @param message Message to be broadcast
	 */
	public static void sendGlobalBroadcast(MicroMessage message){
		sendGlobalBroadcast(message, true);
	}
	
	/**
	 * Broadcasts a message locally or globally.
	 * @param message Message to be broadcast
	 * @param localBroadcast Boolean indicator if broadcast should be global
	 */
	public static void sendGlobalBroadcast(MicroMessage message, boolean localBroadcast){
		if(MicroMessage.globalValidation || message.validation){
			validateMessage(message);
		}
		MTRuntime.sendGlobalBroadcast(message, localBroadcast);
	}

	/**
	 * Constant for Random- and FuzzyCast messages (filled in the case of dispatch errors to prepare error message): 
	 * Key indicates the number of originally requested targets for random/fuzzy dispatch.
	 */
	public static final String RANDOMCAST_ERROR_NUMBER_OF_REQUESTED_TARGETS = "RANDOMCAST_ERROR_NUMBER_OF_REQUESTED_TARGETS";
	/**
	 * Constant for Random- and FuzzyCast messages (filled in the case of dispatch errors to prepare error message): 
	 * Key indicates the number of available targets for random/fuzzy dispatch.
	 */
	public static final String RANDOMCAST_ERROR_NUMBER_OF_AVAILABLE_TARGETS = "RANDOMCAST_ERROR_NUMBER_OF_AVAILABLE_TARGETS";
	/** 
	 * Constant for Random- and FuzzyCast messages (filled in the case of dispatch errors to prepare error message):
	 * Key indicates that invalid quota is passed in fuzzycast (e.g. < 0 or > 1). 
	 */
	public static final String FUZZYCAST_ERROR_INVALID_QUOTA = "FUZZYCAST_ERROR_INVALID_QUOTA";
	
	/**
	 * Sends RandomCast to <numberOfTargets> randomly chosen, constrained by further parameters (mostly in/exclusion list). 
	 * If global is set to true, includes all connected remote nodes. Should only be called by an agent, not externally.
	 * Will return an error message to the sender if not sufficient targets are available.
	 * @param message Message to be sent
	 * @param numberOfTargets Number of target agents
	 * @param global Boolean indicator if platforms should be global
	 * @param trueIndicatesListofCandidatesFalseIndicatesExcludedAgents true indicates to use the exOrInclusionList as list for candidate agents, 
	 * 			false indicates agents that are excluded from random choice
	 * @param inOrExclusionList ArrayList of agents (agent names) to be excluded from Randomcast. If null, all platform-registered agents can be potentially selected.
	 * @return List of agents that have been chosen by the system as message recipient (or empty list)
	 */
	public static ArrayList<String> sendRandomcast(MicroMessage message, Integer numberOfTargets, boolean global, boolean trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, ArrayList<String> inOrExclusionList){
		return sendRandomOrFuzzyCast(message, numberOfTargets, null, global, trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, inOrExclusionList);
	}
	
	/**
	 * Sends FuzzyCast to <quota> of registered or specified agents - constrained by further parameters (mostly in/exclusion list). 
	 * If global is set to true, includes all connected remote nodes. Should only be called by an agent, not externally.
	 * Will return an error message to the sender if not sufficient targets are available.
	 * @param message Message to be sent
	 * @param quota Quota of agents to be addressed (either from registered agents, or agents provided in inclusion list (see further parameters))
	 * @param global Boolean indicator if platforms should be global
	 * @param trueIndicatesListofCandidatesFalseIndicatesExcludedAgents true indicates to use the exOrInclusionList as list for candidate agents, 
	 * 			false indicates agents that are excluded from random choice
	 * @param inOrExclusionList ArrayList of agents (agent names) to be excluded from Randomcast. If null, all platform-registered agents can be potentially selected.
	 * @return List of agents that have been chosen by the system as message recipient (or empty list)
	 */
	public static ArrayList<String> sendFuzzycast(MicroMessage message, Float quota, boolean global, boolean trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, ArrayList<String> inOrExclusionList){
		return sendRandomOrFuzzyCast(message, null, quota, global, trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, inOrExclusionList);
	}
	
	/**
	 * Sends FuzzyCast to <quota> of registered or specified agents - constrained by further parameters (mostly in/exclusion list). 
	 * If global is set to true, includes all connected remote nodes. Should only be called by an agent, not externally.
	 * Will return an error message to the sender if not sufficient targets are available.
	 * @param message Message to be sent
	 * @param quota Quota of agents to be addressed (either from registered agents, or agents provided in inclusion list (see further parameters))
	 * @param global Boolean indicator if platforms should be global
	 * @param trueIndicatesListofCandidatesFalseIndicatesExcludedAgents true indicates to use the exOrInclusionList as list for candidate agents, 
	 * 			false indicates agents that are excluded from random choice
	 * @param inOrExclusionList ArrayList of agents (agent names) to be excluded from Randomcast. If null, all platform-registered agents can be potentially selected.
	 * @return List of agents that have been chosen by the system as message recipient (or empty list)
	 */
	public static ArrayList<String> sendFuzzycast(MicroMessage message, Double quota, boolean global, boolean trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, ArrayList<String> inOrExclusionList){
		return sendRandomOrFuzzyCast(message, null, quota.floatValue(), global, trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, inOrExclusionList);
	}
	
	/**
	 * Sends RandomCast or FuzzyCast to <numberOfTargets> randomly chosen, respectively to <quota> 
	 * of agents - or constrained by further parameters (mostly in/exclusion list). 
	 * If global is set to true, includes all connected remote nodes. Should only be called by an agent, not externally.
	 * Will return an error message to the sender if not sufficient targets are available.
	 * @param message Message to be sent
	 * @param numberOfTargets Number of target agents (or null if using quota)
	 * @param quota Quota of agents to be addressed (either from registered agents, or agents provided in inclusion list (see further parameters))
	 * @param global Boolean indicator if platforms should be global
	 * @param trueIndicatesListofCandidatesFalseIndicatesExcludedAgents true indicates to use the exOrInclusionList as list for candidate agents, 
	 * 			false indicates agents that are excluded from random choice
	 * @param inOrExclusionList ArrayList of agents (agent names) to be excluded from Randomcast. If null, all platform-registered agents can be potentially selected.
	 * @return List of agents that have been chosen by the system as message recipient (or empty list)
	 */
	private synchronized static ArrayList<String> sendRandomOrFuzzyCast(MicroMessage message, Integer numberOfTargets, Float quota, boolean global, boolean trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, ArrayList<String> inOrExclusionList){
		if(message.getSender().equals("")){
			if(quota != null){
				System.err.println(getPlatformPrefix() + "FuzzyCast needs to be sent via agent.");
			} else {
				System.err.println(getPlatformPrefix() + "RandomCast needs to be sent via agent.");
			}
		} else {
			
			
			//list of agents on the platform
			ArrayList<String> list = new ArrayList<String>();
			//list of delivered random number (no double pick)
			ArrayList<Integer> deliveredRandoms = new ArrayList<Integer>();
			//temporary data structure for target agent
			Integer tempTarget;
			
			if(global && isDistributed()){
				list.addAll(getRemoteProcessMap().keySet());
			}
			list.addAll(getRegisteredAgents().keySet());
	
			//reduce agents to allowed agents, respectively remove excluded agents from list
			if(inOrExclusionList != null){
				if(trueIndicatesListofCandidatesFalseIndicatesExcludedAgents){
					//only take from this list
					for(int x=0; x<list.size(); x++){
						if(!inOrExclusionList.contains(list.get(x))){
							list.remove(x);
							x--;
						}
						//possibility that there are less agents registered than suggested in inclusive list - ignored at this point
					}
				} else {
					//exclude all agents from that list
					for(int x=0; x<inOrExclusionList.size(); x++){
						for(int l=0; l<list.size(); l++){
							if(inOrExclusionList.get(x).toString().equals(list.get(l).toString())){
								list.remove(l);
								l--;
							}
						}
					}
				}
			}
			
			//if fuzzycast, calculate number of targets
			if(quota != null){
				//handle invalid cases first before calculating number of target agents from quota
				if(quota < 0.0 || quota > 1.0){
					message.setCustomField(FUZZYCAST_ERROR_INVALID_QUOTA, quota);
					notifySender(message, MessageFields.ERROR_MESSAGE, 11, true, true);
					return new ArrayList<String>();
				}
				if(quota == 0.0){
					//empty list and no deliveries as quota is zero
					return new ArrayList<String>();
				}
				if(quota == 1.0){
					numberOfTargets = list.size();
				} else {
					Float res = list.size()*quota;
					numberOfTargets = (int)Math.round(res);
					/*System.err.println(new StringBuffer(getPlatformPrefix()).append("Calculated ")
								.append(numberOfTargets).append(" from given quota ").append(quota)
								.append(" and list of ").append(list.size()));*/
				}
			}
			
			//if number of available registered agents is too low, report error to sender (and print on console)
			if(list.isEmpty() || list.size() < numberOfTargets){
				/*System.err.println(new StringBuffer(getPlatformPrefix())
					.append("RandomCast: Not sufficient candidates available for message dispatch (Requested: ")
					.append(numberOfTargets).append(", available: ").append(list.size()).append(")."));*/
				
				//fuzzy cast response
				if(quota != null){
					//no notification necessary, as by ratio and thus correct
				} else { //randomcast response
					message.setCustomField(RANDOMCAST_ERROR_NUMBER_OF_REQUESTED_TARGETS, numberOfTargets);
					message.setCustomField(RANDOMCAST_ERROR_NUMBER_OF_AVAILABLE_TARGETS, list.size());
					notifySender(message, MessageFields.ERROR_MESSAGE, 10, true, true);
				}
				return new ArrayList<String>();
			}
			
			ArrayList<String> recipients = new ArrayList<String>();
			if(list.size() == numberOfTargets){
				//if all members from the list are addressed, no need for randomness
				for(int i=0; i<list.size(); i++){
					final MicroMessage finalMsg = (MicroMessage)message.clone();
					finalMsg.setRecipient(list.get(i));
					recipients.add(finalMsg.getRecipient());
					send(finalMsg);
				}
			} else {
				//starting random number generator - only here it will be necessary
				startRandomNoGenerator();
				
				for(int i=0; i<numberOfTargets; i++){
					tempTarget = random.nextInt(list.size());
					while(deliveredRandoms.contains(tempTarget)
							|| list.get(tempTarget).equals(message.getSender())){
						tempTarget = random.nextInt(list.size());
					}
					deliveredRandoms.add(tempTarget);
					//System.out.println(i + "Selected target: " + tempTarget + list.get(tempTarget));
					final MicroMessage finalMsg = (MicroMessage)message.clone();
					finalMsg.setRecipient(list.get(tempTarget));
					recipients.add(finalMsg.getRecipient());
					send(finalMsg);
				}
			}
			return recipients;
		}
		return new ArrayList<String>();
	}
	
	/**
	 * Returns random locally registered agents.
	 * Returns exactly the requested number of agents (numberOfTargets) or none.
	 * @param numberOfTargets Number of agents to be returned
	 * @return List of randomly chosen locally registered agents
	 */
	public static List<String> getRandomAgents(int numberOfTargets){
		return getRandomAgents(numberOfTargets, (String)null, false);
	}
	
	/**
	 * Returns random locally registered agents.
	 * Returns less than requested agents (numberOfTargets) if only less available.
	 * @param numberOfTargets Number of agents to be returned
	 * @return List of randomly chosen locally registered agents
	 */
	public static List<String> getRandomOrLessAgents(int numberOfTargets){
		return getRandomAgents(numberOfTargets, (String)null, true);
	}
	
	/**
	 * Returns random locally or globally registered agents.
	 * Returns exactly the requested number of agents (numberOfTargets) or none.
	 * @param numberOfTargets Number of agents to be returned
	 * @param global Indicator if agents on other platforms should be considered for random selection
	 * @return List of randomly chosen registered agents
	 */
	public static List<String> getRandomAgents(int numberOfTargets, boolean global){
		return getRandomAgents(numberOfTargets, (String)null, global);
	}
	
	/**
	 * Returns random locally registered agents, offers exclusion of one specified agent (by name).
	 * Returns exactly the requested number of agents (numberOfTargets) or none.
	 * @param numberOfTargets Number of agents to be returned
	 * @param excludedAgent Agent to be excluded from random selection
	 * @return List of randomly chosen agents from local platform
	 */
	public static List<String> getRandomAgents(int numberOfTargets, String excludedAgent){
		return getRandomAgents(numberOfTargets, excludedAgent, false);
	}
	
	/**
	 * Returns random locally registered agents, offers exclusion of one specified agent (by name).
	 * Returns less than requested agents (numberOfTargets) if only less available.
	 * @param numberOfTargets Number of agents to be returned
	 * @param excludedAgent Agent to be excluded from random selection
	 * @return List of randomly chosen agents from local platform
	 */
	public static List<String> getRandomOrLessAgents(int numberOfTargets, String excludedAgent){
		return getRandomAgents(numberOfTargets, excludedAgent, true);
	}
	
	/**
	 * Returns random locally or globally registered agents, offers exclusion of one specified agent (by name).
	 * Returns exactly the requested number of agents (numberOfTargets) or none.
	 * @param numberOfTargets Number of agents to be returned
	 * @param excludedAgent Agent to be excluded from random selection
	 * @param global Indicator if agents on other platforms should be considered for random selection
	 * @return List of randomly chosen agents from local platform
	 */
	public static List<String> getRandomAgents(int numberOfTargets, String excludedAgent, boolean global){
		ArrayList<String> exclusionList = null;
		if(excludedAgent != null && !excludedAgent.equals("")){
			exclusionList = new ArrayList<String>();
			exclusionList.add(excludedAgent);
		}
		return getRandomAgents(numberOfTargets, exclusionList, global, false);
	}
	
	/**
	 * Returns random locally registered agents, offers exclusion of arbitrary number of agents (by name).
	 * Returns exactly the requested number of agents (numberOfTargets) or none.
	 * @param numberOfTargets Number of agents to be returned
	 * @param exclusionList List of agents that should be excluded from random selection (or null if none to be excluded)
	 * @return List of randomly chosen agents from local platform
	 */
	public static List<String> getRandomAgents(int numberOfTargets, ArrayList<String> exclusionList){
		return getRandomAgents(numberOfTargets, exclusionList, false, false);
	}
	
	/**
	 * Returns random locally registered agents, offers exclusion of arbitrary number of agents (by name). 
	 * Returns less than requested agents (numberOfTargets) if only less available.
	 * @param numberOfTargets Number of agents to be returned
	 * @param exclusionList List of agents that should be excluded from random selection (or null if none to be excluded)
	 * @return List of randomly chosen agents from local platform
	 */
	public static List<String> getRandomOrLessAgents(int numberOfTargets, ArrayList<String> exclusionList){
		return getRandomAgents(numberOfTargets, exclusionList, false, true);
	}
	
	/**
	 * Returns random registered agents, optionally allows inclusion of agents from other connected platforms.
	 * @param numberOfTargets Number of agents to be returned
	 * @param exclusionList List of agents that should be excluded from random selection (or null if none to be excluded)
	 * @param global Indicator if agents on other platforms should be considered for random selection
	 * @param allowLessTargets Indicates if returning less than requested number of agents is permissible. If not, it returns
	 * 	empty list if insufficient agents available.
	 * @return List of randomly chosen agents
	 */
	public static List<String> getRandomAgents(int numberOfTargets, ArrayList<String> exclusionList, boolean global, boolean allowLessTargets){
		
		startRandomNoGenerator();
		
		//list of agents on the platform
		ArrayList<String> list = new ArrayList<String>();
		//list of delivered random number (no double pick)
		ArrayList<String> deliveredRandoms = new ArrayList<String>();
		//temporary data structure for target agent
		String tempTarget;
		
		if(global && isDistributed()){
			list.addAll(getRemoteProcessMap().keySet());
		}
		list.addAll(getRegisteredAgents().keySet());
		
		//remove excluded agents from list
		if(exclusionList != null){
			for(int x = 0; x < exclusionList.size(); x++){
				for(int l = 0; l < list.size(); l++){
					if(exclusionList.get(x).toString().equals(list.get(l).toString())){
						list.remove(l);
						l--;
					}
				}
			}
		}
		
		//check if too little candidates after cleanup
		if(numberOfTargets > list.size()){
			if(allowLessTargets){
				//if less targets permitted, then return entire candidate list immediately - no need for picking
				return list;
			} else {
				if(showErrorsWhenPickingRandomly){
					System.err.println(getPlatformPrefix() + "Number of random pick candidates smaller than number of requested random agents (" + numberOfTargets + ").");
					if(showStackTraceOnErrors){
						StackTracePrinter.printStackTrace(2);
					}
				}
				return deliveredRandoms;
			}
		}
		
		//check if number is equal, just return entire list
		if(numberOfTargets == list.size()){
			return list;
		}
		
		//ensure that list has entries before picking
		if(list.size() > 0){
			for(int i = 0; i < numberOfTargets; i++){
				tempTarget = list.get(random.nextInt(list.size()));
				while(deliveredRandoms.contains(tempTarget)){
					tempTarget = list.get(random.nextInt(list.size()));
				}
				deliveredRandoms.add(tempTarget);
			}
		}
		return deliveredRandoms;
	}
	
	/**
	 * Returns randomly picked agents from list of given agents. Checks for agents' registration prior to 
	 * picking.
	 * Returns exactly the requested number of agents (numberOfTargets) or none.
	 * @param numberOfTargets Number of targets to be picked
	 * @param inclusionList List from which agents are to be picked
	 * @return List of randomly chosen agents; returns empty list if too little agents available in candidate list for picking.
	 */
	public static List<String> getRandomAgentsFromList(int numberOfTargets, ArrayList<String> inclusionList){
		return getRandomAgentsFromList(numberOfTargets, inclusionList, null, false);
	}
	
	/**
	 * Returns randomly picked agents from list of given agents. Checks for agents' registration prior to 
	 * picking.
	 * Returns exactly the requested number of agents (numberOfTargets) or none.
	 * @param numberOfTargets Number of targets to be picked
	 * @param inclusionList List from which agents are to be picked
	 * @param exclusionList List of agents (in inclusionList) that should not be picked. Ignored if null.
	 * @return List of randomly chosen agents; returns empty list if too little agents available in candidate list for picking.
	 */
	public static List<String> getRandomAgentsFromList(int numberOfTargets, ArrayList<String> inclusionList, ArrayList<String> exclusionList){
		return getRandomAgentsFromList(numberOfTargets, inclusionList, exclusionList, false);
	}
	
	/**
	 * Returns randomly picked agents from list of given agents. Checks for agents' registration prior to 
	 * picking.
	 * @param numberOfTargets Number of targets to be picked
	 * @param inclusionList List from which agents are to be picked
	 * @param exclusionList List of agents (in inclusionList) that should not be picked. Ignored if null.
	 * @param allowLessTargets Indicates if returning less than requested number of agents is permissible. If not, it returns
	 * 	empty list if insufficient agents available.
	 * @return List of randomly chosen agents; returns empty list if too little agents available in candidate list for picking.
	 */
	public static List<String> getRandomAgentsFromList(int numberOfTargets, ArrayList<String> inclusionList, ArrayList<String> exclusionList, boolean allowLessTargets){
		
		//list of delivered random number (no double pick)
		ArrayList<String> deliveredRandoms = new ArrayList<String>();
		
		//check for null list
		if(inclusionList == null){
			if(showErrorsWhenPickingRandomly){
				System.err.println(getPlatformPrefix() + "List if targets to be picked from (requested " + numberOfTargets + " picks) is null.");
				if(showStackTraceOnErrors){
					StackTracePrinter.printStackTrace(2);
				}
			}
			return deliveredRandoms;
		}
		
		startRandomNoGenerator();
		
		//check if all listed agents are actually registered
		for(int i = 0; i < inclusionList.size(); i++){
			if(!getRegisteredAgents().containsKey(inclusionList.get(i))){
				//remove non-registered agent
				inclusionList.remove(inclusionList.get(i));
				i--;
			}
		}
		
		//filter agents that are in exclusionList
		if(exclusionList != null){
			for(int i = 0; i < exclusionList.size(); i++){
				inclusionList.remove(exclusionList.get(i));
			}
		}
		
		//check if there are enough agents to pick after all
		if(numberOfTargets > inclusionList.size()){
			if(allowLessTargets){
				//if less targets permitted, then return entire candidate list immediately - no need for picking
				return inclusionList;
			} else {
				if(showErrorsWhenPickingRandomly){
					System.err.println(getPlatformPrefix() + "Number of random pick candidates smaller than number of requested random agents (" + numberOfTargets + ").");
					if(showStackTraceOnErrors){
						StackTracePrinter.printStackTrace(2);
					}
				}
				return deliveredRandoms;
			}
		}
		
		//check if number is equal, just return entire list
		if(numberOfTargets == inclusionList.size()){
			return inclusionList;
		}
		
		//now, pick random agents from list

		//temporary data structure for target agent
		String tempTarget;
		
		//but check that the list is not empty
		if(!inclusionList.isEmpty()){
			for(int i = 0; i < numberOfTargets; i++){
				tempTarget = inclusionList.get(random.nextInt(inclusionList.size()));
				while(deliveredRandoms.contains(tempTarget)){
					tempTarget = inclusionList.get(random.nextInt(inclusionList.size()));
				}
				deliveredRandoms.add(tempTarget);
			}
		}
		return deliveredRandoms;
	}
	
	/**
	 * Sends role cast (message to all agents with a given role). If global = true, 
	 * all connected nodes are contacted as well.
	 * Deliver is based on role class, not role instance, e.g. all TestRole() instances.
	 * @param message Message to be sent
	 * @param role Role type the message to be sent to
	 * @param global Indicates if roles on other platforms should be notified
	 */
	public static void sendRolecast(MicroMessage message, Role role, boolean global){
		if(message.getSender().equals("")){
			System.err.println(getPlatformPrefix() + "RoleCast needs to be sent via agent.");
		} else {
			Iterator<Role> it = roleMap.keySet().iterator();
			Role tempRole;
			message.put(roleKeyword, role);
			while(it.hasNext()){
				tempRole = it.next();
				if(tempRole.getClass().equals(role.getClass())){
					message.setRecipient(roleMap.get(tempRole));
					send(message);
				}
			}
			if(global){
				sendRemotecast(message, rolecastPrimitive);
			}
		}
	}
	
	/**
	 * Validates the MicroMessage with the MicroMessage validator defined
	 * in the configuration file. (if validation is activated)
	 * @param message Message to check
	 * @return Boolean indicator if message is valid (true) or not (false)
	 */
	public static boolean validateMessage(MicroMessage message){
		return MTRuntime.getMicroMessageValidator().validate(message);	
	}
	
	/*
	 * Returns Jetlang channel for a given agent.
	 * @param agentName - agent name to lookup
	 * @return MemoryChannel for agent.
	 */
	/*
	public static MemoryChannel<MicroMessage> getAgentChannel(String agentName) {
		return MTRuntime.getAgentChannel(agentName);
	}*/
	
	/*
	 * Returns the Jetlang broadcast channel
	 * @return - broadcast memorychannel
	 */
	/*
	public static MemoryChannel<MicroMessage> getCommonChannel(){
		return MTRuntime.getCommonChannel();
	}*/
	
	/**
	 * Sends message to sending agent in case of distribution problems. Types of
	 * problems --> 
	 * 		1, recipient agent not registered locally
	 * 		2, recipient agent neither registered locally nor globally
	 * 		3, intent processor could not be found (if no recipient supplied)
	 * 		4, intent forwarded to remote host for execution
	 * 		5, inform sender about blind forwarding to remote host (not error but information)
	 * 		6, specified remote platform not connected with local one
	 * 		7, intent processor could not be found on this platform (intent request from other platform)
	 * 		8, invalid GenericIntent provided
	 * 		9, execution error for GenericIntent
	 * 	   10, not enough agents available for RandomCast (either too many excluded or not enough registered)
	 * 	   11, invalid quota passed for FuzzyCast (values < 0 or > 1)
	 * 
	 * @param message Message of concern for delivery
	 * @param messageType Either ERROR_MESSAGE or INFORM_MESSAGE constant
	 * @param type Notification type (see above)
	 * @param print Indicates if message should be printed in console
	 * @param sendLocally Indicates if the notification should be sent to the original message sender
	 */
	public static void notifySender(MicroMessage message, String messageType, int type, boolean print, boolean sendLocally) {
		//create error MicroMessage for sender ...
		message.setCustomField(MessageFields.ORIG_RECIPIENT, message.getRecipient());
		message.setCustomField(MessageFields.ORIG_PERFORMATIVE, message.getPerformative());
		message.setCustomField(MessageFields.ORIG_CONTENT, message.getContent());
		if(type == 7){
			//if sending remote, set up original requester
			message.setRecipient(message.get(MessageFields.ORIG_SENDER).toString());
		} else {
			if((message.getSender() == null || message.getSender().equals(""))
					&& !(message.get(MessageFields.ORIG_SENDER) == null || message.get(MessageFields.ORIG_SENDER).toString().equals(""))){
				message.setRecipient(message.get(MessageFields.ORIG_SENDER).toString());
			} else {
				message.setCustomField(MessageFields.ORIG_SENDER, message.getSender());
				message.setRecipient(message.getSender());
			}
		}
		message.setSender(MTRuntime.platformProcess);
		message.setPerformative(messageType);
		StringBuffer errorMessage = new StringBuffer();
		switch(type){
			case 1:
				errorMessage.append("Recipient '");
				errorMessage.append(message.getCustomField(MessageFields.ORIG_RECIPIENT));
				errorMessage.append("' for message sent by ");
				errorMessage.append(message.getRecipient());
				errorMessage.append(" could not be found (locally).");
				break;
			case 2:
				errorMessage.append("Recipient '");
				errorMessage.append(message.getCustomField(MessageFields.ORIG_RECIPIENT));
				errorMessage.append("' for message sent by ");
				errorMessage.append(message.getRecipient());
				errorMessage.append(" could not be found neither locally nor globally.");
				break;
			case 3:
				errorMessage.append("Intent processor for '");
				errorMessage.append( message.getIntent() );
				errorMessage.append("' (requested by ");
				errorMessage.append(message.getRecipient());
				errorMessage.append(") could not be found.");
				break;
			case 4:
				int numberOfNodes = getPropagatedNodes().size();
				errorMessage.append("Intent processor for '");
				errorMessage.append( message.getIntent() );
				errorMessage.append("' (requested by ");
				errorMessage.append(message.getRecipient());
				errorMessage.append(") could not be found locally, now forwarded to ");
				errorMessage.append(numberOfNodes);
				errorMessage.append(" remote node(s).");
				//add additional field containing the number of nodes forwarded to - to keep track of results/answers
				message.setCustomField(numberOfRemoteNodes, numberOfNodes);
				break;
			case 5:
				errorMessage.append("Sending message to user-defined target '");
				errorMessage.append( message.getRecipient() );
				errorMessage.append("'");
				break;
			case 6:
				errorMessage.append("Remote platform '");
				errorMessage.append(message.get(platformIdKeyword));
				errorMessage.append("' is not synchronized with local platform.");
				break;
			case 7:
				errorMessage.append("Intent processor for '");
				errorMessage.append( message.getIntent() );
				errorMessage.append("' (requested by ");
				errorMessage.append(message.getRecipient());
				errorMessage.append(") could not be found on platform ");
				errorMessage.append(getPlatformID());
				break;
			case 8:
				errorMessage.append("Intent '");
				errorMessage.append( message.getIntent() );
				errorMessage.append("' (requested by ");
				errorMessage.append(message.getRecipient());
				errorMessage.append(") is NOT a valid GenericIntent.");
				break;
			case 9:
				errorMessage.append("GenericIntent '");
				errorMessage.append( message.getIntent() );
				errorMessage.append("' (requested by ");
				errorMessage.append(message.getRecipient());
				errorMessage.append(") could not be successfully executed.");
				break;
			case 10:
				errorMessage.append("RandomCast for ");
				errorMessage.append(message.getCustomField(RANDOMCAST_ERROR_NUMBER_OF_REQUESTED_TARGETS));
				errorMessage.append(" target(s) (requested by ");
				errorMessage.append(message.getRecipient());
				errorMessage.append(") could not be executed as only ");
				errorMessage.append(message.getCustomField(RANDOMCAST_ERROR_NUMBER_OF_AVAILABLE_TARGETS));
				errorMessage.append(" candidate(s) are available for selection.");
				break;
			case 11:
				errorMessage.append("Invalid quota for FuzzyCast (");
				errorMessage.append(message.getCustomField(FUZZYCAST_ERROR_INVALID_QUOTA));
				errorMessage.append(") sent by ");
				errorMessage.append(message.getRecipient());
				errorMessage.append(".");
				break;
		}
		message.setContent(errorMessage.toString());
		//... and sending it if still registered.
		if(MTRuntime.getRegisteredAgents().containsKey(message.getRecipient())
				&& sendLocally){
			//System.out.println("Trying to send FAILURE to " + message.getRecipient());
			send(message);
		}
		if(print){
			if(messageType.equals(MessageFields.INFORM_MESSAGE)){
				System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append(errorMessage.toString()));
			}
			if(messageType.equals(MessageFields.ERROR_MESSAGE)){
				System.err.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append(errorMessage.toString())
						.append(LINE_DELIMITER).append("Please check for '").append(MessageFields.ERROR_MESSAGE)
						.append("' performative in agent '").append(message.getRecipient())
						.append("' for detailed information."));
			}
		}
	}
	
	/** map containing intent classes and according subscribed agents */
	private static ConcurrentHashMap<Class, LinkedList> registeredIntents = new ConcurrentHashMap<Class, LinkedList>();
	
	/**
	 * Adds an applicable intent for an agent
	 * @param agentName Agent to register intent for
	 * @param intentClass Intent class to register
	 * @return Boolean indicating successful registration of intent
	 */
	protected static boolean addApplicableIntent(String agentName, Class intentClass){
		if(registeredIntents.containsKey(intentClass)){
			LinkedList<String> list = registeredIntents.get(intentClass);
			return list.add(agentName);
		} else {
			LinkedList<String> list = new LinkedList<String>();
			boolean success = list.add(agentName);
			registeredIntents.put(intentClass, list);
			return success;
		}
	}
	
	/**
	 * Removes an applicable intent for an agent 
	 * @param agentName Agent name
	 * @param intentClass Intent class to unregister
	 * @return Boolean indicating success of removal
	 */
	protected static boolean removeApplicableIntent(String agentName, Class intentClass){
		if(registeredIntents.containsKey(intentClass)){
			LinkedList<String> list = registeredIntents.get(intentClass);
			boolean success = list.remove(agentName);
			if(list.isEmpty()){
				registeredIntents.remove(intentClass);
			}
			return success; 
		} else {
			System.err.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Removal of applicable intent ").append(intentClass).append(" for agent ").append(agentName).append(" failed."));
			return false;
		}
	}
	
	/**
	 * Clears applicable intents for an agent.
	 * @param agentName Agent whose intents should be deregistered
	 */
	protected static void clearApplicableIntents(String agentName){
		Iterator<Class> it = registeredIntents.keySet().iterator();
		while(it.hasNext()){
			Class itClass = it.next();
			LinkedList<String> list = registeredIntents.get(itClass);
			while(list.contains(agentName)){
				list.remove(agentName);
			}
			if(list.isEmpty()){
				registeredIntents.remove(itClass);
				it = registeredIntents.keySet().iterator();
			}
		}
	}
	
	/**
	 * Prints all registered applicable intents.
	 */
	public static void printApplicableIntents(){
		StringBuffer buf = new StringBuffer();
		Iterator<Class> it = registeredIntents.keySet().iterator();
		Iterator<String> innerIt;
		buf.append(MTRuntime.LINE_DELIMITER).append("Intent List:");
		if(registeredIntents.isEmpty()){
			buf.append(" empty");
		} else {
			while(it.hasNext()){
				Class itClass = it.next();
				buf.append(MTRuntime.LINE_DELIMITER).append(" Intent: ");
				buf.append(itClass.getName());
				buf.append(MTRuntime.LINE_DELIMITER).append("  Registered Agents:");
				LinkedList list = registeredIntents.get(itClass);
				innerIt = list.iterator();
				while(innerIt.hasNext()){
					buf.append(MTRuntime.LINE_DELIMITER).append("   - ");
					buf.append(innerIt.next());
				}
			}
		}
		System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix())
			.append("Applicable intents: ").append(buf.toString()));	
	}
	
	private static HashMap<Class, LinkedList<String>> eventSubscriptions = new HashMap<Class, LinkedList<String>>();
	
	/**
	 * Subscribes an agent to an event (class).
	 * @param eventClass Event type to subscribe to 
	 * @param agent Subscribing agent
	 * @return Boolean indicating success of subscription
	 */
	public synchronized static boolean subscribeToEvent(Class eventClass, String agent){
		if(eventClass.getSuperclass().equals(Event.class)){
			if(eventSubscriptions.containsKey(eventClass)){
				LinkedList<String> list = eventSubscriptions.get(eventClass);
				if(list.contains(agent)){
					return true;
				} else {
					return list.add(agent);
				}
			} else {
				LinkedList<String> list = new LinkedList<String>();
				boolean success = list.add(agent);
				eventSubscriptions.put(eventClass, list);
				return success;
			}
		} else {
			System.err.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Agent ")
					.append(agent).append(" tried to subscribe to non-Event class ").append(eventClass).toString());
			return false;
		}
	}
	
	/**
	 * Unsubscribes an agent from an event type.
	 * @param eventClass Event class to unsubscribe
	 * @param agent Unsubscribing agent
	 * @return Boolean indicating success of unsubscription
	 */
	public synchronized static boolean unsubscribeFromEvent(Class eventClass, String agent){
		if(eventClass.getSuperclass().equals(Event.class)){
			if(eventSubscriptions.containsKey(eventClass)){
				LinkedList<String> list = eventSubscriptions.get(eventClass);
				boolean success = list.remove(agent);
				if(list.isEmpty()){
					eventSubscriptions.remove(eventClass);
				}
				return success; 
			} else {
				System.err.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Removal of event subscription ").append(eventClass).append(" for agent ").append(agent).append(" failed."));
				return false;
			}
		} else {
			System.err.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Agent ")
					.append(agent).append(" tried to unsubscribe from non-Event class ").append(eventClass).toString());
			return false;
		}
	}
	
	/**
	 * Clears event subscription for a given agent (e.g. on deregistration).
	 * @param agent Agent whose event subscriptions should be cleared
	 */
	public synchronized static void clearEventSubscriptions(String agent){
		Iterator<Class> it = eventSubscriptions.keySet().iterator();
		Iterator<String> listIt;
		Class eventToTest;
		while(it.hasNext()){
			eventToTest = it.next();
			listIt = eventSubscriptions.get(eventToTest).iterator();
			String candidate;
			while(listIt.hasNext()){
				candidate = listIt.next();
				if(candidate.equals(agent)){
					eventSubscriptions.get(eventToTest).remove(agent);
					if(eventSubscriptions.get(eventToTest).size() == 0){
						eventSubscriptions.remove(eventToTest);
						it = eventSubscriptions.keySet().iterator();
						break;
					} else {
						listIt = eventSubscriptions.get(eventToTest).iterator();
					}
				}
			}
		}
	}
	
	/**
	 * Prints all current event subscriptions by agents.
	 */
	public static void printEventSubscriptions(){
		StringBuffer buf = new StringBuffer();
		Iterator<Class> it = eventSubscriptions.keySet().iterator();
		Iterator<String> innerIt;
		buf.append(MTRuntime.LINE_DELIMITER).append("Event Subscription List:");
		if(eventSubscriptions.isEmpty()){
			buf.append(" empty");
		} else {
			while(it.hasNext()){
				Class itClass = it.next();
				buf.append(MTRuntime.LINE_DELIMITER).append(" Event: ");
				buf.append(itClass);
				buf.append(MTRuntime.LINE_DELIMITER).append("  Subscribing Agents:");
				LinkedList list = eventSubscriptions.get(itClass);
				innerIt = list.iterator();
				while(innerIt.hasNext()){
					buf.append(MTRuntime.LINE_DELIMITER).append("   - ");
					buf.append(innerIt.next());
				}
			}
		}
		System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Event subscriptions: ").append(buf.toString()));
	}
	
	
	/** Data container handling the association of role with agent name */
	private static HashMap<Role, String> roleMap = new HashMap<Role, String>();
	
	/**
	 * registers role and associates it with existing agent. Attention: Goals are added
	 * by the implementing agent after initialization of role.
	 * @param agent agent name
	 * @param role role to associate
	 */
	protected static void registerRole(String agent, Role role){
		roleMap.put(role, agent);
	}
	
	/**
	 * Removes registered role for registered agent from register. 
	 * @param agent Agent name
	 * @param role Role instance
	 */
	protected static void unregisterRole(String agent, Role role){
		if(roleMap.containsKey(role) && roleMap.get(role).equals(agent)){
			roleMap.remove(role);
		}
	}
	
	/**
	 * Retrieves agent for a given role.
	 * @param role Role type to look up
	 * @return Name of agent playing this role
	 */
	public static String getAgentForRole(Role role){
		return roleMap.get(role);
	}
	
	/**
	 * Initializes the shutdown of the platform and ensures that all
	 * agents, role assignments, intents and events are removed.
	 */
	public static void shutdown(){
		//wait a little bit before shutting down (often useful if shutdown is immediately called after execution code)
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		send(new MicroMessage(new LocalPlatformShutdownEvent(SystemOwner.ownName)));
		MicroMessage msg = new MicroMessage();
		msg.setRecipient(SystemOwner.ownName);
		msg.setSender(platformProcess);
		msg.setPerformative(shutdownPerformative);
		SystemOwner.getInstance().receive(msg);
		if(!MTRuntime.getRegisteredAgents().isEmpty()){
			sendLocalBroadcast(msg);
			System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Sent shutdown notification to all remaining agents."));
		}
		System.out.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Shutting down."));

		shutdownCheck();
		shutdownPlatform();
	}
	
	/**
	 * Checks the key data structures and indicates if those have been unloaded for
	 * system consistency.
	 * @return boolean true indicates that everything is unloaded
	 */
	public static boolean shutdownCheck(){
		boolean check = false;
		long start = System.currentTimeMillis();
		
		while(check == false && (System.currentTimeMillis()-start) < 200){
			check = true;
			if(MTRuntime.getRegisteredAgents().size() != 0){
				check = false;
				System.err.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Not all agents have been unloaded."));
			}
			if(registeredIntents.size() != 0){
				check = false;
				System.err.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Not all goals have been unloaded."));
			}
			if(eventSubscriptions.size() != 0){
				check = false;
				System.err.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Not all event subscriptions have been unloaded."));
			}
			if(getInternalMessagePassingFramework().equals(MessagePassingFrameworks.MICRO_FIBER)){
				if(!allMicroFiberMessagesDelivered()){
					check = false;
					System.err.println(new StringBuffer(MTRuntime.getPlatformPrefix()).append("Not all message have been delivered yet."));
				}
			}
		}

		return check;
	}
	
	/**
	 * Returns the current time as Date object.
	 * 
	 * @return Current time as Date object
	 */
	public static Date getCurrentTime(){
		Calendar cal = Calendar.getInstance();
		return cal.getTime();
	}
	
	/**
	 * Returns the formatted current date/time as String (for output/logging purposes)
	 * supported formats:
	 * "yyyy-MM-dd HH:mm:ss", "HH:mm:ss" (see constants {@link DATE_FORMAT_NOW} and {@link TIME_FORMAT_NOW}).  
	 * @param includingDate Boolean indicating if date should be returned or only time 
	 * @return String representation of current time
	 */
	public static String getCurrentTimeString(boolean includingDate){
		 String dateFormat = "";
		 if(includingDate){
			 dateFormat = DATE_FORMAT_NOW;
		 } else {
			 dateFormat = TIME_FORMAT_NOW;
		 }
		 SimpleDateFormat simpleFormat = new SimpleDateFormat(dateFormat);
		 return simpleFormat.format(getCurrentTime());
	}
	
	/**
	 * Validates a generic intent with regards to specification in 
	 * GenericIntentProcessor.
	 * @param intent Generic intent to be validated
	 * @return Boolean indicating if validation was successful
	 */
	public static boolean validateGenericIntent(Intent intent){
		return GenericIntentProcessor.validateGenericIntent(intent);
	}
}
