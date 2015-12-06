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

import java.util.ArrayList;
import java.util.Collection;
import org.nzdis.micro.inspector.annotations.Inspect;
import org.nzdis.micro.messaging.MTRuntime;
import org.nzdis.micro.msgvalidator.MicroMessageValidator;
import org.nzdis.micro.util.SimpleSemaphore;

/**
 * Basis for Social role implementation. Interaction via Asynchronous Message
 * Passing. This class wraps agent functionality for direct access in 
 * implementation
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a>
 * @version $Revision: 2.0 $ $Date: 2013/02/28 00:00:00 $
 */

public abstract class DefaultSocialRole extends AbstractRole implements SocialRole{

	@Inspect
	private ArrayList<MicroMessage> messageList = new ArrayList<MicroMessage>(5); 
	
	protected SimpleSemaphore semaphore = new SimpleSemaphore("Role message queue", false);
	
	@Override
	public void handleMessage(final MicroMessage message){
		if(MTRuntime.isSynchronousOperationMode()){
			printError(new StringBuffer("The platform is configured to use synchronous message passing.\n")
					.append("To achieve that fully, the DefaultSocialRole implementation (i.e. this role implementation) needs to override the 'handleMessage()' method,\n")
					.append("otherwise the message is handled via the role's message queue (which results in asynchronous execution)."));
		}
		//asynchronous execution
		semaphore.acquire();
		messageList.add(message);
		semaphore.release();
	}
	
	/**
	 * Returns first message from message queue.
	 * @return First message from queue
	 */
	protected synchronized MicroMessage getMessage(){
		MicroMessage msg = null;
		semaphore.acquire();
		if(!messageList.isEmpty()){
			msg = messageList.remove(0);
		}
		semaphore.release();
		return msg;
	}
	
	/**
	 * Returns true if message list contains unprocessed messages.
	 * @return true -> queue contains unprocessed messages, false -> no messages
	 */
	protected boolean haveUnprocessedMessages(){
		return !messageList.isEmpty();
	}
	
	/**
	 * Blocks until a message is in the message list.
	 */
	protected void awaitMessage(){
		while(messageList.isEmpty()){
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Blocks until a message that complies with the MicroMessageValidator 
	 * arrives in the message queue.
	 * @param validator MicroMessageValidator specifying the validity.
	 */
	protected void awaitMessage(MicroMessageValidator validator){
		while(!messageList.isEmpty()){
			semaphore.acquire();
			for(MicroMessage message: messageList){
				if(validator.validate(message)){
					semaphore.release();
					return;
				}
			}
			semaphore.release();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Returns the number of unprocessed messages 
	 * in message queue.
	 * @return Number of queued messages
	 */
	protected Integer getNumberOfUnprocessedMessages(){
		return messageList.size();
	}
	
	/**
	 * Sends message via default message transport.
	 * @param message Message to be sent
	 */
	public void send(MicroMessage message){
		((AbstractAgent)getAgent()).send(message);
	}
	
	/**
	 * Sends intent (automatically encapsulated in message) 
	 * via default message transport.
	 * @param intent Intent to be raised
	 */
	public void send(Intent intent){
		((AbstractAgent)getAgent()).send(intent);
	}
	
	/**
	 * Sends event (automatically encapsulated in message) 
	 * via default message transport.
	 * @param event Event to be raised (sent) across the platform
	 */
	public void send(Event event){
		((AbstractAgent)getAgent()).send(event);
	}
	
	/**
	 * Sends message to agents playing a given role. If indicated, dispatches
	 * this message globally. 
	 * @param message Message to be sent
	 * @param role Role instance
	 * @param global if set to true, will sent message to connected nodes
	 */
	public void sendRolecast(MicroMessage message, Role role, boolean global){
		((AbstractAgent)getAgent()).sendRolecast(message, role, global);
	}
	
	/**
	 * Sends Randomcast to <numberOfTargets> agents which are chosen randomly.
	 * If global is set to true, random choice includes all connected remote nodes.
	 * @param message Message to be sent
	 * @param numberOfTargets Number of targets to be randomly chosen
	 * @param global Should the message be sent globally (distributed across connected platforms)?
	 * @param trueIndicatesListofCandidatesFalseIndicatesExcludedAgents Specifies if list is exclusive or excluding (true: exclusive) for random targets
	 * @param exclusionList ArrayList of agents (agent names) to be in/excluded from Randomcast. If null, all platform-registered agents can be potentially selected. 
	 * @return List of agents that have been chosen for message dispatch
	 */
	public ArrayList<String> sendRandomcast(MicroMessage message, int numberOfTargets, boolean global, boolean trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, ArrayList<String> exclusionList){
		return ((AbstractAgent)getAgent()).sendRandomcast(message, numberOfTargets, global, trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, exclusionList);
	}
	
	/**
	 * Sends Randomcast to <numberOfTargets> agents which are chosen randomly.
	 * If global is set to true, random choice includes all connected remote nodes.
	 * @param message Message to be sent
	 * @param numberOfTargets Number of targets to be randomly chosen
	 * @param global Should the message be sent globally (distributed across connected platforms)?
	 * @param trueIndicatesListofCandidatesFalseIndicatesExcludedAgents Specifies if list is exclusive or excluding (true: exclusive) for random targets
	 * @param exclusionList ArrayList of agents (agent names) to be in/excluded from Randomcast. If null, all platform-registered agents can be potentially selected.
	 * @return Collection of agents that have been chosen for message dispatch
	 */
	public ArrayList<String> sendRandomcast(MicroMessage message, int numberOfTargets, boolean global, boolean trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, Collection<String> exclusionList){
		return ((AbstractAgent)getAgent()).sendRandomcast(message, numberOfTargets, global, trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, new ArrayList<String>(exclusionList));
	}
	
	/**
	 * Sends message to all agents in specified local group. If group is not
	 * specified, message is sent to all sub-agents of the sending agent.
	 * @param message Message to be sent
	 * @param group Group the message should be dispatched to
	 */
	public void sendGroupcast(MicroMessage message, Group group){
		((AbstractAgent)getAgent()).sendGroupcast(message, group);
	}
	
	/**
	 * Sends broadcast via default message transport. Broadcast is
	 * restricted to local platform.
	 * @param message Message to be sent
	 */
	public void sendBroadcast(MicroMessage message){
		((AbstractAgent)getAgent()).sendBroadcast(message);
	}
	
	/**
	 * Sends broadcast via default message transport. Broadcast is spread
	 * to all connected distributed platforms.
	 * @param message Message to be sent
	 */
	public void sendGlobalBroadcast(MicroMessage message){
		((AbstractAgent)getAgent()).sendGlobalBroadcast(message);
	}
	
	/**
	 * Sends Fuzzycast to the specified <quota> of agents which are chosen randomly.
	 * If global is set to true, random choice includes all connected remote nodes.
	 * @param message Message to be sent
	 * @param quota Fraction of all agents that should be chosen randomly and the message addressed to
	 * @param global Should the message be sent globally (distributed across connected platforms)?
	 * @param trueIndicatesListofCandidatesFalseIndicatesExcludedAgents Specifies if list is exclusive or excluding (true: exclusive) for random targets
	 * @param inOrExclusionList ArrayList of agents (agent names) to be in/excluded from Fuzzycast. If null, all platform-registered agents can be potentially selected.
	 * @return List of agents that have been chosen for message dispatch
	 */
	public ArrayList<String> sendFuzzycast(MicroMessage message, Float quota, boolean global, boolean trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, ArrayList<String> inOrExclusionList){
		return ((AbstractAgent)getAgent()).sendFuzzycast(message, quota, global, trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, inOrExclusionList);
	}
	
	/**
	 * Sends Fuzzycast to the specified <quota> of agents which are chosen randomly.
	 * If global is set to true, random choice includes all connected remote nodes.
	 * @param message Message to be sent
	 * @param quota Fraction of all agents that should be chosen randomly and the message addressed to
	 * @param global Should the message be sent globally (distributed across connected platforms)?
	 * @param trueIndicatesListofCandidatesFalseIndicatesExcludedAgents Specifies if list is exclusive or excluding (true: exclusive) for random targets
	 * @param inOrExclusionList ArrayList of agents (agent names) to be in/excluded from Fuzzycast. If null, all platform-registered agents can be potentially selected.
	 * @return List of agents that have been chosen for message dispatch
	 */
	public ArrayList<String> sendFuzzycast(MicroMessage message, Double quota, boolean global, boolean trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, ArrayList<String> inOrExclusionList){
		return ((AbstractAgent)getAgent()).sendFuzzycast(message, quota, global, trueIndicatesListofCandidatesFalseIndicatesExcludedAgents, inOrExclusionList);
	}
	
	/**
	 * Sends message via customized pattern considering all non-unicast and non-Globalcast 
	 * functionality. -- NOT YET IMPLEMENTED
	 * @param message MicroMessage
	 * @param group Group to send to
	 * @param role Roles to address
	 * @param numberOfTargets Number of targets for randomized addressing
	 * @param exclusionList List of agents to be excluded
	 * @param localcast Indicates if message should be send to all agents on local platform
	 * @param global Indicates if Rolecast, Randomcast and Groupcast functionality should be executed globally
	 */
	@Deprecated
	public void sendCustomcast(MicroMessage message, Group group, Role role, int numberOfTargets, ArrayList<String> exclusionList, boolean localcast, boolean global){
		((AbstractAgent)getAgent()).sendCustomcast(message, group, role, numberOfTargets, exclusionList, localcast, global);
	}
	
	/**
	 * Adds message filter (on a dedicated sub micro-agent) to this agent
	 * @param filter Message filter instance
	 */
	public void addMessageFilter(MessageFilter filter){
		((AbstractAgent)getAgent()).addMessageFilter(filter);
	}
	
	/**
	 * Adds message filter (on a dedicated sub micro-agent) to this agent
	 * @param filter Message filter instance
	 * @param messageFilterAgentName Agent name for sub micro-agent
	 * @return
	 */
	public void addMessageFilter(MessageFilter filter, String nameForMessageFilterAgent){
		((AbstractAgent)getAgent()).addMessageFilter(filter, nameForMessageFilterAgent);
	}
	
	/**
	 * Returns an array of all running message filters (i.e. sub-agents
	 * dedicated to this role). It is returned
	 * as role array as MessageFilter instances instantiated as inner classes
	 * cannot be cast to MessageFilter.
	 * @return Array of all active message filter instances
	 */
	public Role[] getMessageFilters(){
		return ((AbstractAgent)getAgent()).getMessageFilters();
	}
	
	/**
	 * Removes Message filter (from sub-group) identified by its role instance reference
	 * @param messageFilter MessageFilter to be removed
	 */
	public void removeMessageFilter(MessageFilter messageFilter){
		((AbstractAgent)getAgent()).removeMessageFilter(messageFilter);
	}
	
	/**
	 * Removes Message filter (from sub-group) identified by agent name
	 * @param messageFilterAgentName Name of MessageFilter to be removed
	 */
	public void removeMessageFilter(String messageFilterAgentName){
		((AbstractAgent)getAgent()).removeMessageFilter(messageFilterAgentName);
	}
	
	/**
	 * Clears all message filters (i.e. kills all message filter sub-agents)
	 */
	public void clearMessageFilters(){
		((AbstractAgent)getAgent()).clearMessageFilters();
	}
	
	/**
	 * Helper function for subscription to events via Message Transport system
	 * @param event Event to subscribe to
	 */
	public void subscribe(Class event){
		((AbstractAgent)getAgent()).subscribe(event);
	}
	
	/**
	 * Helper function to unsubscribe from event 
	 * @param event Event to unsubscribe from
	 */
	public void unsubscribe(Class event){
		((AbstractAgent)getAgent()).unsubscribe(event);
	}
	
	/**
	 * Clears all event subscriptions of this agent
	 */
	public void clearSubscriptions(){
		((AbstractAgent)getAgent()).clearSubscriptions();
	}
	
}
