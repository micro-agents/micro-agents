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
package org.nzdis.micro.messaging;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetlang.core.Callback;
import org.jetlang.fibers.Fiber;
import org.jetlang.fibers.PoolFiberFactory;
import org.nzdis.micro.Event;
import org.nzdis.micro.Intent;
import org.nzdis.micro.MTConnector;
import org.nzdis.micro.MicroMessage;
import org.nzdis.micro.Role;
import org.nzdis.micro.constants.AgentConsoleOutputLevels;
import org.nzdis.micro.constants.AgentStates;
import org.nzdis.micro.constants.MessagePassingFrameworks;
import org.nzdis.micro.inspector.annotations.Inspect;
import org.nzdis.micro.messaging.message.Message;


/**
 * The AbstractCommunicator augments the generic agent implementation with
 * facilities for asynchronous message passing. All related functionality
 * is provided here.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 */
public abstract class AbstractCommunicator extends AgentStates implements MessageCommunicator{

	/** indicates the agent name */
	@Inspect
	protected String agentName;
	/** indicates if agent name is set and initialized (can only be done once) */
	protected boolean agentNameInitialized = false;
	/** indicates whether Message Transport is registered for the according agent */
	protected boolean MTRegistered = false;
	/** indicates if collectAndPrint is activated (which can be deactivated for performance/memory reasons */
	protected boolean collectAndPrint = false;
	/** Current state of the agent. */
	@Inspect
	protected int state = CREATED;
	
	/**
	 * returns the status of the agent
	 */
	public int getStatus(){
		return state;
	}
	
	/**
	 * registers with Message Transport system (called by setup())
	 */
	protected void registerMT(){
		if(!MTRegistered){
			MTConnector.register(agentName, this);
			MTRegistered = true;
			//only start Jetlang fibers if Jetlang is used
			if(MTConnector.getInternalMessagePassingFramework().equals(MessagePassingFrameworks.JETLANG)){
				if(service == null){
					service = Executors.newCachedThreadPool();
				}
				if(fact == null){
					fact = new PoolFiberFactory(service);
				}
				if(fiber == null){
					fiber = fact.create();
				}
				if(inBox == null){
					inBox = new Callback<MicroMessage>(){
	
						@Override
						public void onMessage(MicroMessage message) {
							if(state == ACTIVE || state == DYING){
								if(!message.getSender().equals(agentName)){
									try{
										receive(message);
									} catch(Exception e){
										System.err.println(new StringBuffer("Jetlang Message passing: Error during message processing by recipient ")
										.append(agentName).append("!").append(MTRuntime.LINE_DELIMITER)
										.append("Please check the application code.").append(MTRuntime.LINE_DELIMITER)
										.append(message).toString());
										e.printStackTrace();
									}
								}
							} else {
								print(new StringBuffer("Sent message to ").append(agentName).append(" which is in state ").append(getStateDescription(state)));
							}
						}
	
					};
				}
				
				MTConnector.getAgentChannel(agentName).subscribe(fiber, inBox);
				MTConnector.getCommonChannel().subscribe(fiber, inBox);
				fiber.start();
			}
		} else {
			printError(new StringBuffer("Tried to register to message transport although already registered!").toString());
		}
	}
	
	protected void unregisterMT(){
		if(MTRegistered){
			MTConnector.unregister(agentName);
			MTRegistered = false;
			if(fiber != null){
				fiber.dispose();
			}
		} else {
			printError(new StringBuffer("Tried to unregister from message transport although not registered!").toString());
		}
	}

	/**
	 * Returns agent's name.
	 * @return
	 */
	public String getAgentName() {
		return agentName;
	}

	/** Jetlang fiber for receiving messages */
	private Fiber fiber = null;
	static volatile ExecutorService service = null;
    PoolFiberFactory fact = null;

	
	/**
	 *  Jetlang callback for redirecting MicroMessages for unified handling in receive(). 
	 */
	private Callback<MicroMessage> inBox = null;
	
	/**
	 * serveMessage() method for redirecting incoming messages to according role's handleMessage()
	 */
	public void serveMessage(Message message){
		if(state == ACTIVE || state == DYING){
			/* casting instead of recreating message upon receipt. 
			 * Advantage: proprietary message structures (specializations), 
			 * Disadvantage: potential in-memory manipulation during sending
			 */
			//receive(new MicroMessage(message));
			receive((MicroMessage) message);
		} else {
			print(new StringBuffer("Sent message to ").append(agentName).append(" which is in state ").append(getStateDescription(state)));
		}
	}
	
	public abstract void receive(MicroMessage message);
	
	/**
	 * Creates message with supplied intent and sends it
	 * off.
	 * @param intent - intent to be processed by other agent
	 */
	public void send(Intent intent){
		MicroMessage msg = new MicroMessage(intent);
		send(msg);
	}
	
	/**
	 * Creates message with supplied event and sends it
	 * off.
	 * @param event - event to be raised
	 */
	public void send(Event event){
		MicroMessage msg = new MicroMessage(event);
		send(msg);
	}
	
	private void checkOnRegistrationAndStateErrors(){
		if(!MTRegistered){
			printError("Agent not registered properly!");
		} else {
			printError(new StringBuffer("Agent could not send message as in state ").append(getStateDescription(state)));
		}
	}
	
	/**
	 * Sends message via default message transport.
	 * @param message
	 */
	public void send(MicroMessage message){
		if(!message.sendUnchanged){
			message.setSender(agentName);
		}
		if(MTRegistered && (state == ACTIVE || state == DYING)){
			MTConnector.send(message);
		} else {
			checkOnRegistrationAndStateErrors();
		}
	}
	
	/**
	 * Sends broadcast via default message transport. Broadcast is
	 * restricted to local platform.
	 * @param message
	 */
	public void sendBroadcast(MicroMessage message){
		if(!message.sendUnchanged){
			message.setSender(agentName);
		}
		if(MTRegistered && (state == ACTIVE || state == DYING)){
			MTConnector.sendLocalBroadcast(message);
		} else {
			checkOnRegistrationAndStateErrors();	
		}
	}
	
	/**
	 * Sends broadcast via default message transport. Broadcast is spreaded
	 * to all connected distributed platforms.
	 * @param message
	 */
	public void sendGlobalBroadcast(MicroMessage message){
		if(!message.sendUnchanged){
			message.setSender(agentName);
		}
		if(MTRegistered && (state == ACTIVE || state == DYING)){
			MTConnector.sendGlobalBroadcast(message);
		} else {
			checkOnRegistrationAndStateErrors();
		}
	}
	
	/**
	 * Sends Randomcast to <numberOfTargets> agents which are chosen randomly.
	 * If global is set to true, random choice includes all connected remote nodes.
	 * @param message
	 * @param numberOfTargets
	 * @param global
	 * @param trueIndicatesListofCandidatesFalseIndicatesExcludedAgents Specifies if list is exclusive or excluding (true: exclusive) for random targets
	 * @param inOrExclusionList ArrayList of agents (agent names) to be in/excluded from Randomcast. If null, all platform-registered agents can be potentially selected.
	 * @return list of agents that have been chosen for message dispatch
	 */
	public ArrayList<String> sendRandomcast(MicroMessage message, int numberOfTargets, boolean global, boolean trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, ArrayList<String> inOrExclusionList){
		if(!message.sendUnchanged){
			message.setSender(agentName);
		}
		if(MTRegistered && (state == ACTIVE || state == DYING)){
			return MTConnector.sendRandomcast(message, numberOfTargets, global, trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, inOrExclusionList);
		} else {
			checkOnRegistrationAndStateErrors();	
		}
		return null;
	}
	
	/**
	 * Sends Fuzzycast to the specified <quota> of agents which are chosen randomly.
	 * If global is set to true, random choice includes all connected remote nodes.
	 * @param message
	 * @param numberOfTargets
	 * @param global
	 * @param trueIndicatesListofCandidatesFalseIndicatesExcludedAgents Specifies if list is exclusive or excluding (true: exclusive) for random targets
	 * @param inOrExclusionList ArrayList of agents (agent names) to be in/excluded from Fuzzycast. If null, all platform-registered agents can be potentially selected.
	 * @return list of agents that have been chosen for message dispatch
	 */
	public ArrayList<String> sendFuzzycast(MicroMessage message, Float quota, boolean global, boolean trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, ArrayList<String> inOrExclusionList){
		if(!message.sendUnchanged){
			message.setSender(agentName);
		}
		if(MTRegistered && (state == ACTIVE || state == DYING)){
			return MTConnector.sendFuzzycast(message, quota, global, trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, inOrExclusionList);
		} else {
			checkOnRegistrationAndStateErrors();	
		}
		return null;
	}
	
	/**
	 * Sends Fuzzycast to the specified <quota> of agents which are chosen randomly.
	 * If global is set to true, random choice includes all connected remote nodes.
	 * @param message
	 * @param numberOfTargets
	 * @param global
	 * @param trueIndicatesListofCandidatesFalseIndicatesExcludedAgents Specifies if list is exclusive or excluding (true: exclusive) for random targets
	 * @param inOrExclusionList ArrayList of agents (agent names) to be in/excluded from Fuzzycast. If null, all platform-registered agents can be potentially selected.
	 * @return list of agents that have been chosen for message dispatch
	 */
	public ArrayList<String> sendFuzzycast(MicroMessage message, Double quota, boolean global, boolean trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, ArrayList<String> inOrExclusionList){
		if(!message.sendUnchanged){
			message.setSender(agentName);
		}
		if(MTRegistered && (state == ACTIVE || state == DYING)){
			return MTConnector.sendFuzzycast(message, quota, global, trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, inOrExclusionList);
		} else {
			checkOnRegistrationAndStateErrors();	
		}
		return null;
	}
	
	
	/**
	 * Sends message to agents playing a given role. If indicated, dispatches
	 * this message globally. 
	 * @param message Message to be sent
	 * @param role Role instance
	 * @param global If set to true, will sent message to connected nodes
	 */
	public void sendRolecast(MicroMessage message, Role role, boolean global){
		if(!message.sendUnchanged){
			message.setSender(agentName);
		}
		if(MTRegistered && (state == ACTIVE || state == DYING)){
			MTConnector.sendRolecast(message, role, global);
		} else {
			checkOnRegistrationAndStateErrors();	
		}
	}
	
	/**
	 * Helper function for subscription to events via Message Transport system
	 * @param event Event to subscribe to
	 */
	public void subscribe(Class event){
		MTConnector.subscribeToEvent(event, getAgentName());
	}
	
	/**
	 * Helper function to unsubscribe from event 
	 * @param event - event to unsubscribe from
	 */
	public void unsubscribe(Class event){
		MTConnector.unsubscribeFromEvent(event, getAgentName());
	}
	
	/**
	 * Clears all event subscriptions of this agent.
	 */
	public void clearSubscriptions(){
		MTConnector.clearEventSubscriptions(getAgentName());
	}
	
	/** StringBuffer holding collected messages */
	@Inspect
	StringBuffer collectedOutput = null;
	
	/**
	 * This function collects console output (and adds a linebreak
	 * for each entry) but does not 
	 * print it unless print is set to true. The collected
	 * output is deleted upon printing.
	 * @param output Message to be collected
	 * @param print Indicates if collected messages are to be printed
	 * @param reset Resets the buffer independent from printing (even if printing is false)
	 */
	public void collectAndPrint(StringBuffer output, boolean print, boolean reset){
		if(collectAndPrint){
			if(collectedOutput == null){
				collectedOutput = new StringBuffer();
			}
			if(output != null){
				collectedOutput.append(output).append(MTRuntime.LINE_DELIMITER);
			}
			if(print){
				print(new StringBuffer(MTRuntime.LINE_DELIMITER)
					.append("---- START OF COLLECTED OUTPUT ----").append(MTRuntime.LINE_DELIMITER)
					.append(collectedOutput).append("---- END OF COLLECTED OUTPUT ----"));
				collectedOutput = null;
			}
			if(reset){
				collectedOutput = null;
			}
		}
	}
	
	/**
	 * Clears all collected output (collected via {@link #collectAndPrint(StringBuffer, boolean, boolean)}) without printing it.
	 */
	public void clearCollectedOutput(){
		collectedOutput = null;
	}
	
	/**
	 * StringBuffer version of {@link #print(String)} (faster!)
	 * In case of multiple parameters use {@link #print(StringBuffer...)}.
	 * @param message
	 */
	public void print(StringBuffer message){
		if(MTRuntime.agentConsoleOutputLevel > AgentConsoleOutputLevels.ERROR){
			System.out.println(prefixAgentOutputIfActivated(message, 1));
		}
		//if activated via inspector or otherwise, additionally collect and print message
		if(collectAndPrint){
			collectAndPrint(prefixAgentOutputIfActivated(message, 1), false, false);
		}
	}
	
	/**
	 * StringBuffer varargs version of {@link #print(String)} (faster!)
	 * Different arguments are concatenated in a lazy fashion.
	 * @param message
	 */
	public void print(StringBuffer... message){
		if(MTRuntime.agentConsoleOutputLevel > AgentConsoleOutputLevels.ERROR){
			StringBuffer builder = new StringBuffer();
			for(int i = 0; i < message.length; i++){
				builder.append(message[i]);
			}
			print(builder);
		}
	}
	
	/**
	 * Console print function providing current agent's name as prefix.
	 * Use is discouraged. Use {@link #print(StringBuffer)} or {@link #print(StringBuffer...)} 
	 * in case of multiple String elements for lazy concatenation. 
	 * @param message
	 */
	public void print(String message){
		print(new StringBuffer(message));
	}
	
	/**
	 * Varargs version of Console print including current agent's name as prefix.
	 * Preferably use {@link #print(StringBuffer...)}. 
	 * @param message
	 */
	public void print(String... message){
		if(MTRuntime.agentConsoleOutputLevel > AgentConsoleOutputLevels.ERROR){
			StringBuffer builder = new StringBuffer();
			for(int i = 0; i < message.length; i++){
				builder.append(message[i]);
			}
			print(builder);
		}
	}
	
	/**
	 * Console print function providing current agent's name as prefix.
	 * @param message
	 */
	public void print(Object message){
		if(message != null){
			print(new StringBuffer(message.toString()));
		}
	}
	
	/**
	 * Console error print function providing current agent's name as prefix.
	 * @param message
	 */
	public void printError(String message){
		printError(new StringBuffer(message));
	}
	
	/**
	 * Varargs version of Console print including current agent's name as prefix.
	 * Preferably use {@link #printError(StringBuffer...)}. 
	 * @param message
	 */
	public void printError(String... message){
		if(MTRuntime.agentConsoleOutputLevel >= AgentConsoleOutputLevels.ERROR){
			StringBuffer builder = new StringBuffer();
			for(int i = 0; i < message.length; i++){
				builder.append(message[i]);
			}
			printError(builder);
		}
	}
	
	/**
	 * StringBuffer version of printError() (faster!)
	 * @param message
	 */
	public void printError(StringBuffer message){
		if(MTRuntime.agentConsoleOutputLevel >= AgentConsoleOutputLevels.ERROR){
			System.err.println(prefixAgentOutputIfActivated(message, 2));
		}
		//if activated via inspector or otherwise, additionally collect and print message
		if(collectAndPrint){
			collectAndPrint(prefixAgentOutputIfActivated(message, 2), false, false);
		}
	}
	
	/**
	 * StringBuffer varargs version of {@link #printError(StringBuffer...)} (faster!)
	 * Different arguments are concatenated in a lazy fashion. 
	 * @param message
	 */
	public void printError(StringBuffer... message){
		if(MTRuntime.agentConsoleOutputLevel >= AgentConsoleOutputLevels.ERROR){
			StringBuffer builder = new StringBuffer();
			for(int i = 0; i < message.length; i++){
				builder.append(message[i]);
			}
			printError(builder);
		}
	}
	
	/**
	 * Console error print function providing current agent's name as prefix.
	 * @param message
	 */
	public void printError(Object message){
		if(message != null){
			printError(new StringBuffer(message.toString()));
		}
	}
	
	/**
	 * Filters the output and puts the agent name as a prefix for each 
	 * new line if activated (see @prefixOutputLinesWithAgentPrefix switch)
	 * @param message Message to be filtered
	 * @param messageType Indication of message type:
	 * 		1 --> Information message (INFO)
	 * 		2 --> Error message (ERROR)
	 * @return Message with agent name as prefix if activated, else unchanged message
	 */
	private StringBuffer prefixAgentOutputIfActivated(StringBuffer message, Integer messageType){
		if(MTRuntime.prefixMessageTypeIndicationForAgentOutput){
			switch(messageType){
				case 1:
					message = new StringBuffer("INFO - ").append(message);
					break;
				case 2:
					message = new StringBuffer("ERROR - ").append(message);
					break;
			}
		}
		message = new StringBuffer(this.agentName).append(": ").append(message);
		if(MTRuntime.prefixAgentOutputWithNanoTime){	
			String time = new Long(System.nanoTime()).toString();
			message = new StringBuffer(time).append(" - ").append(message);
		}
		
		if(MTRuntime.prefixOutputLinesWithAgentPrefix){
			Pattern p = Pattern.compile(MTRuntime.LINE_DELIMITER); 
			Matcher m = p.matcher(message);
			return new StringBuffer(m.replaceAll(new StringBuffer(MTRuntime.LINE_DELIMITER).append(this.agentName).append(": ").toString()));
		}
		return message;
	}
	
}
