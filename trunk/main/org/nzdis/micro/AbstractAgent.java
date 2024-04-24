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
import java.util.HashMap;
import java.util.Iterator;
import org.nzdis.micro.events.AgentActivatedEvent;
import org.nzdis.micro.events.AgentDyingEvent;
import org.nzdis.micro.events.AgentSuspendedEvent;
import org.nzdis.micro.exceptions.InvalidDisposalOfRoleOnLivingAgent;
import org.nzdis.micro.exceptions.RoleNotInitializedOnThisAgentException;
import org.nzdis.micro.inspector.annotations.Inspect;
import org.nzdis.micro.messaging.AbstractCommunicator;
import org.nzdis.micro.messaging.MTRuntime;
import org.nzdis.micro.util.SimpleSemaphore;

/**
 * The Class AbstractAgent is the core implementation to the agent interface
 * provided with the OPAL micro-agent framework. Its specialization needs to 
 * implement facilities for handling of incoming messages. The default agent
 * will use AnonymousAgent as its implementation.
 * 
 * Created: 26/06/2010
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a>
 * @version $Revision: 1.0 $ $Date: 2011/06/24 00:00:00 $
 *
 */
public abstract class AbstractAgent extends AbstractCommunicator implements Agent {

	/** Reference to the Clojure connector for this agent (if activated) */
	private ClojureConnector cljConn = null;
	/** Indicates whether agent is to be registered for Message Transport */
	protected boolean agentMTSupport = false;
	/** Name of Clojure agent script to be run at initialization time */
	protected String agentScript = "";
	/** Indicates if Clojure is to be supported by this agent (is overridden by global support) */
	protected boolean agentCljSupport = false;
	/** Indicates if Clojure support has been initialized */
	protected boolean cljRegistered = false;
	/** Indicates if agent is to be integrated into OPAL hierarchy (and as such registered 
	 * with the agent loaders (for lookup)).
	 * Default: Agent is registered. */
	private boolean noOpalHierarchy = false;
	
	/** Very simple Log for individual agent - helpful for debugging */
	private Log log = null;
	
	public Log getLog(){
		if(log == null){
			//if(log == null){
				log = new Log(this);
			//}
		}
		return log;
	}
	/** Switch to de/activate logging */
	public boolean logActive = false;
	
	/** Agent owning this agent */
	protected Agent owner = null;
	
	/** Indicates if passed agent name is only prefix (unification of name will be done by platform) */
	private boolean agentNameIsPrefix = false;
	
	/** Indicates if MicroHierarchy is initialized (only once) */
	private boolean MicroHierarchyInitialized = false;
	/** Group reference for agent (when registered in OpalHierarchy) */
	protected Group group = null;
	/**
	 * Defines the subagent handling strategy:
	 * Indicates if subagents in group are to be killed upon death of owning agent or
	 * reassigned to dead agent's owner's group (i.e. assigned to the next higher agent)
	 */
	protected boolean killSubAgentsOnDeath = false;
	
	protected SimpleSemaphore semaphore = new SimpleSemaphore(this);
	
	/** Local lookup tables for intent role associations */
	private HashMap<Class<Intent>, Role> intentRoleMap = new HashMap<Class<Intent>, Role>();
	
	private HashMap<Class<Intent>, Role> getIntentRoleMap(){
		if(intentRoleMap == null){
			intentRoleMap = new HashMap<Class<Intent>, Role>(2);
		}
		return intentRoleMap;
	}
	
	private HashMap<Class<Intent>, SocialRole> socialIntentRoleMap = null;
	
	private HashMap<Class<Intent>, SocialRole> getSocialIntentRoleMap(){
		if(socialIntentRoleMap == null){
			socialIntentRoleMap = new HashMap<Class<Intent>, SocialRole>(2);
		}
		return socialIntentRoleMap;
	}
	
	private HashMap<Class<Intent>, PassiveRole> reactiveIntentRoleMap = null;
	
	private HashMap<Class<Intent>, PassiveRole> getReactiveIntentRoleMap(){
		if(reactiveIntentRoleMap == null){
			reactiveIntentRoleMap = new HashMap<Class<Intent>, PassiveRole>(2);
		}
		return reactiveIntentRoleMap;
	}
	/** List of all roles registered for this agent. */
	@Inspect
	private ArrayList<Role> roles = null;
	
	private ArrayList<Role> getRolesMap(){
		if(roles == null){
			roles = new ArrayList<Role>(1);
		}
		return roles;
	}
	/** Separate lists for social and reactive roles to ease reindexing */
	private ArrayList<PassiveRole> reactiveRoles = null; 
	
	private ArrayList<PassiveRole> getReactiveRoles(){
		if(reactiveRoles == null){
			reactiveRoles = new ArrayList<PassiveRole>(1);
		}
		return reactiveRoles;
	}
	
	private ArrayList<SocialRole> socialRoles = null;
	
	private ArrayList<SocialRole> getSocialRoles(){
		if(socialRoles == null){
			socialRoles = new ArrayList<SocialRole>(1);
		}
		return socialRoles;
	}
	
	/** Separate list for message filters (which are a specialization of social roles 
	 *  but not held in that list */
	private ArrayList<MessageFilter> messageFilterRoles = null; 
	
	protected ArrayList<MessageFilter> getMessageFilterRoles(){
		if(messageFilterRoles == null){
			messageFilterRoles = new ArrayList<MessageFilter>(1);
		}
		return messageFilterRoles;
	}
	
	/** List for managing prohibited roles */
	private ArrayList<Class> prohibitedRoles = null;
	
	private ArrayList<Class> getProhibitedRoles(){
		if(prohibitedRoles == null){
			prohibitedRoles = new ArrayList<Class>(1);
		}
		return prohibitedRoles;
	}

	/** Indicates if message filter role was added */
	protected boolean messageFilterRolesRegistered = false;

	
	/**
	 * returns Clojure connector for this agent if registered with
	 * Clojure support
	 * @return ClojureConnector
	 */
	public ClojureConnector getCljConnector(){
		if(cljRegistered){
			return cljConn;
		} else {
			//printError("Agent is not Clojure-enabled. Enable support in order to use Clojure features.");
			//return null;
			activateClojureSupport(agentScript);
			return cljConn;
		}
	}
	
	/**
	 * Constructor reserved for SystemOwner instance.
	 */
	protected AbstractAgent(){
		this.killSubAgentsOnDeath = true;
		this.owner = this;
	}
	
	/**
	 * Main Constructor for AbstractAgent providing all configuration options
	 * @param agentName - agent name
	 * @param useNameAsPrefix - indicator if the passed agent name is only to be used as prefix (and completed by the platform with a unique ID suffix)
	 * @param agentScript - Clojure Script name for agent (or "" if none provided) (expected to be in indivCljScript folder)
	 * @param owner - owner agent (will be SystemOwner if none provided)
	 * @param noOpalHierarchy - indicates the integration into the OPAL hierarchy (usually the case)
	 * @param ClojureActivated - indicates if Clojure is to be activated for this instance
	 * @param MessageTransportActivated - indicates if Asynchronous Message Passing support should be activated (not the case for purely reactive agents)
	 * @param killSubAgentsOnDeath - configures how this agent will treat its subagents upon death (true --> kill them, false --> reassign to next higher agent)
	 */
	public AbstractAgent(String agentName, boolean useNameAsPrefix, String agentScript, Agent owner, boolean noOpalHierarchy, boolean ClojureActivated, boolean MessageTransportActivated, boolean killSubAgentsOnDeath){
		this.noOpalHierarchy = noOpalHierarchy;
		this.agentScript = agentScript;
		this.agentName = agentName;
		this.agentNameIsPrefix = useNameAsPrefix;
		if(owner == null){
			this.owner = SystemOwner.getInstance();
		} else {
			this.owner = owner;
		}
		if(!this.agentScript.equals("")){
			this.agentCljSupport = true;
		} else {
			this.agentCljSupport = ClojureActivated;
		}
		this.agentMTSupport = MessageTransportActivated;
		this.killSubAgentsOnDeath = killSubAgentsOnDeath;
		setup();
	}
	
	public AbstractAgent(String agentName, boolean noOPALHierarchy){
		this(agentName, false, "", null, noOPALHierarchy, false, false, true);
	}
	
	public AbstractAgent(String agentName, String agentScript, boolean noOPALHierarchy){
		this(agentName, false, agentScript, null, noOPALHierarchy, false, false, true);
	}
	
	public AbstractAgent(String agentName, String agentScript){
		this(agentName, false, agentScript, null, false, false, false, true);
	}

	public AbstractAgent(String agentName, boolean useNameAsPrefix, String agentScript, Agent owner){
		this(agentName, useNameAsPrefix, agentScript, owner, false, false, false, true);
	}
	
	/**
	 * Sets up the agent according to constructor-defined configuration.
	 * Can be called multiple times in case of configuration changes (Messaging 
	 * and Clojure support activation)
	 */
	private void setup(){		

		//call MTRuntime just to execute the static block in advance
		MTRuntime.load();
		
		/* Name initialization */ 

		if(!this.agentNameInitialized){
			if(MTRuntime.getOutputLevel() > 0){
				System.out.print("Initializing agent ");
			}
			
			if(this.agentName.equals("")){
				this.agentName = MTConnector.getNextAnonymousAgentName().intern();
			} else {
				if(agentNameIsPrefix){
					//complete name with unique ID as suffix
					this.agentName = this.agentName.concat(MTConnector.getNextSuffixForAgentName().toString()).intern();
				} else {
					this.agentName = this.agentName.intern();
				}
			}
			this.agentNameInitialized = true;
			if(MTRuntime.getOutputLevel() > 0){
				System.out.print(this.agentName);
			}
		}
		
		
		/* Micro-agent hierarchy initialization */

		if(!noOpalHierarchy && !MicroHierarchyInitialized){
			//initializing of agent group assignments
			initMicroHierarchy();
		}

		/* Message Transport initialization */
		if(agentMTSupport && !MTRegistered){
			registerMT();
		}
		
		/* Clojure initialization */
		if(MTRuntime.isClojureActivated() && agentCljSupport && !cljRegistered){
			this.cljConn = new ClojureConnector(this, this.agentScript);
			cljRegistered = true;
			//execClj(new StringBuffer("(greeting)"));
		} else {
			if(!MTRuntime.isClojureActivated() && agentCljSupport){
				printError("Ensure to activate Clojure globally in order to use it within an agent.");
			}
		}

		//will only be call in first round
		if(state != ACTIVE && MTRuntime.getOutputLevel() > 0){
			System.out.println(" --- DONE");
		}
		setActiveStatus();
	}
	
	/**
	 * Executes all actions necessary when agent switches to activated status
	 */
	private void setActiveStatus(){
		this.state = ACTIVE;
		if(MTRegistered){
			MicroMessage message = new MicroMessage();
			message.setEvent(new AgentActivatedEvent(this.agentName));
			send(message);
		}
	}

	/**
	 * Indicates if messages received by the agent are to be processed or
	 * put into wait state in order to allow transactional activities 
	 * to complete (example: creation of new conversation in Conversation manager)
	 */
	private boolean waitForMessageFilter = false;
	
	/**
	 * Indicates if processing of subsequent messages should be blocked
	 * to allow transactions to complete
	 * @return boolean - true --> block processing, false --> proceed
	 */
	public boolean messageProcessingBlocked(){
		return waitForMessageFilter;
	}
	
	/**
	 * Blocks processing for subsequent messages when called. Don't forget to 
	 * release in order allow messages to be processed
	 */
	public void blockMessageProcessing(){
		waitForMessageFilter = true;
	}
	
	/**
	 * Releases the block status for message processing
	 */
	public void allowMessageProcessing(){
		waitForMessageFilter = false;
	}
	
	@Override
	public synchronized void receive(MicroMessage message) {
		//printError("Received message "+  message.toString() + " for processing.");
		boolean passThrough = true;
		//Check on message filters in subgroup
		//print("MessageFilters: " + messageFilterRoles.toString());
		//print("Message filter registered: " + messageFilterRegistered);
		if(group != null || messageFilterRolesRegistered){
			
			MicroMessage msg = new MicroMessage();
			/* if received message is already a MessageFilter message, 
			 * strip the encapsulating message before passing on to
			 * own message filter (else cascaded encapsulation)
			 */
			if(message.containsKey(MicroMessage.MSG_PARAM_MSG_TO_FILTER)){
				/* extract message filter payload for encapsulation in own
				 * message */
				msg.setCustomField(MicroMessage.MSG_PARAM_MSG_TO_FILTER, message.getCustomField(MicroMessage.MSG_PARAM_MSG_TO_FILTER));
			} else {
				/* introduce new message */
				msg.setCustomField(MicroMessage.MSG_PARAM_MSG_TO_FILTER, message);
			}
			
			//block message filter processing if complete processing of previous message is required
			if(messageProcessingBlocked()){
				print("Processing of Message filter blocked - awaiting processing of previous message");
				while(messageProcessingBlocked()){
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				};
				print("Continuing processing of Message filters.");
			}
			
			//check if holding the filter role itself
			if(messageFilterRolesRegistered){
				/* access own message filter synchronously but make copy of list first as of potentially 
				 * changing registrations during execution (e.g. registration of new message filter as result
				 * of processing of message) */
				ArrayList<MessageFilter> copiedList = (ArrayList<MessageFilter>)messageFilterRoles.clone();
				Iterator<MessageFilter> it = copiedList.iterator();
				MessageFilter targetRole;
				while(it.hasNext()){
					targetRole = it.next();
					if(passThrough){
						passThrough = ((MessageFilter)targetRole).processingByNonMessageFiltersAllowed();
					}

					//print("Sent to filter " + targetRole.getAgent().getAgentName() + ": " + msg.toString());

					//synchronous
					targetRole.handleMessage(msg);
					//asynchronous
					//msg.setRecipient(targetRole.getAgent().getAgentName());
					//send(msg);
				}
			}
			
			//check if subgroup holds filter role (only on subgroup level, not recursively!)
			if(group != null){
				if(getGroup().findRolesByTypeNonRecursive(MessageFilter.class, false).length > 0){
					//print("Group: " + getGroup().toString());
					//print("Number of subfilters: " + getGroup().findRoles(MessageFilter.class).length);
					Role[] filters = getGroup().findRolesByTypeNonRecursive(MessageFilter.class, false);

					for(int i=0; i<filters.length; i++){
						
						msg.setRecipient(filters[i].getAgent().getAgentName());
						if(passThrough){
							passThrough = ((MessageFilter)filters[i]).processingByNonMessageFiltersAllowed();
						}
						//print("Sent to own group's filter " + filters[i].getAgent().getAgentName() + ": " + msg.toString());
						
						send(msg);
					}
				}
			}
		}

		//continue processing if no message filters or pass through allowed
		if(passThrough){
			//if message contains intent --> direct to according role
			if(message.containsIntent()){
				if(getSocialIntentRoleMap().containsKey(message.getIntent().getClass())){
					socialIntentRoleMap.get(message.getIntent().getClass()).handleMessage(message);
				} else {
					//deliver to all (intent in message but no role applicable for intent)
					for(int i=0; i<getSocialRoles().size(); i++){
						socialRoles.get(i).handleMessage(message);
					}
				}
			} else { //else dispatch to all social roles
				for(int i=0; i<getSocialRoles().size(); i++){					
					socialRoles.get(i).handleMessage(message);
				}
			}
		}
		//check if shutdown has been requested
		if(message.getPerformative().equals(MTRuntime.shutdownPerformative)){
			die();
		}
	}
	
	/**
	 * Initializes the agent in a given group and owner
	 */
	private void initMicroHierarchy(){

		if(!agentName.equals(SystemOwner.getSystemOwnerName())){
			owner.getGroup().reregister(this);
		}
		MicroHierarchyInitialized = true;
	}
	
	/**
	 * Returns the Owner of the agent
	 */
	public Agent getOwner(){
		return this.owner;
	}
	
	/**
	 * Activates Clojure support (in case of configuration change during agent life-time)
	 * @param agentScript - Clojure script to be loaded (or "" if none)
	 */
	public void activateClojureSupport(String agentScript){
		if(!this.cljRegistered){
			this.agentCljSupport = true;
			this.agentScript = agentScript;
			setup();
		}
	}
	
	/**
	 * Activates Message Transport support (in case of configuration change during agent life-time)
	 */
	public void activateMTSupport(){
		if(!this.MTRegistered){
			this.agentMTSupport = true;
			setup();
		}
	}
	
	/**
	 * Configures the subagent handling strategy upon death of agent
	 * true --> kill subagents on death
	 * false --> reassign subagents to owner agent upon death
	 * @param strategy (boolean with values above)
	 */
	public void killSubHierarchyUponDeath(boolean strategy){
		this.killSubAgentsOnDeath = strategy;
	}
	
	/**
	 * Deregisters agent from all his functions (Message Transport, Roles, Intents) 
	 * and frees memory.
	 */
	public void die(){
		if(this.state != DEAD && !this.agentName.equals(SystemOwner.getSystemOwnerName())){
			//Message Transport handling - state change delayed in order to be able to message
			this.state = DYING;
			if(MTRuntime.getOutputLevel() > 0){
				print("State: " + getStateDescription(state));
			}
			if(MTRegistered){
				MicroMessage msg = new MicroMessage();
				msg.setEvent(new AgentDyingEvent(this.agentName));
				send(msg);
			}
			//OPAL hierarchy handling
			if(!noOpalHierarchy){
				//handling of own group
				handleAgentHierarchyUponDeath();
				//deregistering from owner's group
				if(this.owner != null){
					((DefaultGroup)this.owner.getGroup()).deregister(this);	
				}
				/* clear all message filters (i.e. message filter agents
				 * running in this agent's sub-group */
				clearMessageFilters();
				//change state to DEAD before disposing roles
				this.state = DEAD;
				//destroy all roles
				disposeAllRoles();
			}
			this.state = DEAD;
			//unregistering from message transport (as roles might send final messages during disposal)
			if(MTRegistered){
				unregisterMT();
			}
			if(MTRuntime.getOutputLevel() > 0){
				print("State: " + getStateDescription(state));
			}
		} else {
			if(this.getAgentName().equals(SystemOwner.getSystemOwnerName())){
				handleAgentHierarchyUponDeath();
			} else {
				printError("Calling dead agent.");
			}
		}
	}

	/**
	 * Handles the agent's subhierarchy depending on the strategy chosen
	 * (see variable killSubAgentsOnDeath) either by destruction or reallocation
	 * to next higher agent in hierarchy. 
	 */
	private synchronized void handleAgentHierarchyUponDeath(){
		
		if(getGroup() != null){
			while(getGroup().getAgents().length > 0){
				Agent agentToHandle = getGroup().getAgents()[0]; //agentsToBeTreated[i];
				if(!killSubAgentsOnDeath && !getGroup().equals(SystemOwner.getInstance().getGroup())){
					//Alternative 1:
					/* unregistering all agents from this group and registering in owner's group
					/- unless SystemOwner's group --> then all agents need to be killed */
					if(!getGroup().equals(SystemOwner.getInstance().getGroup())){
						getOwnerGroup().reregister(agentToHandle);
					} 
				} else {
					//Alternative 2:
					//killing all agents in group (Caution: They might pass their subagent upwards!)
					((AbstractAgent)agentToHandle).killSubHierarchyUponDeath(true);
					agentToHandle.die();
				}
			}
		}
	}
	
	/**
	 * Sends message to all agents in specified local group. If group is not
	 * specified, message is sent to all sub-agents of the sending agent.
	 * @param message
	 * @param group
	 */
	public void sendGroupcast(MicroMessage message, Group group){
		if(!message.sendUnchanged){
			message.setSender(agentName);
		}
		if(MTRegistered && (state == ACTIVE || state == DYING)){
			if(group == null){
				getGroup().sendGroupcast(message);
			} else {
				group.sendGroupcast(message);
			}
		} else {
			if(!MTRegistered){
				printError("Agent not registered properly!");
			} else {
				printError("Agent could not send message as in state " + state);
			}	
		}
	}
	
	/**
	 * Sends message via customized pattern considering all non-unicast and non-Globalcast 
	 * functionality. -- NOT YET IMPLEMENTED
	 * 
	 * @param message - MicroMessage
	 * @param group - Group to send to
	 * @param role - Roles to address
	 * @param numberOfTargets - number of targets for randomized addressing
	 * @param exclusionList - list of agents to be excluded
	 * @param localcast - indicates if message should be send to all agents on local platform
	 * @param global - indicates if Rolecast, Randomcast and Groupcast functionality should be executed globally
	 */
	public void sendCustomcast(MicroMessage message, Group group, Role role, int numberOfTargets, ArrayList<String> exclusionList, boolean localcast, boolean global){
		throw new RuntimeException("This method has not been implemented yet.");
	}
	
	/**
	 * Executes Clojure s-expressions. Should be used with care as inefficient.
	 * Support lazy initialization of Clojure
	 * Alternatively link directly to Clojure vars 
	 * (using getClojureConnector.registerFunction(functionName)) if possible.
	 * @param clj s-expression to be executed
	 * @return
	 */
	public Object execClj(StringBuffer cljCmd){
		
		if(!cljRegistered){
			activateClojureSupport(agentScript);
		};
			/*if(executeCljCmd == null){
				executeCljCmd = RT.var(getCljConnector().getAgentNamespace(), "eval-cmd");
			}
			try {
				return executeCljCmd.invoke(cljStuff.toString());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}*/
			//full compiler run - slow!
		return (Object) this.cljConn.execInClojure(cljCmd);
		 /*else {
			printError("Called for execution of Clojure command although Clojure support is not activated!");
			return false;
		}*/	
	}
	
	
	
	
	/**
	 * Prints current log to console
	 */
	public void printLog(){
		getLog().printLog();
	}
	
	
	/**
	 * Suspends the agent from work.
	 */
	public boolean suspend(){
		if(state == ACTIVE){
			if(MTRegistered){
				MicroMessage msg = new MicroMessage();
				msg.setEvent(new AgentSuspendedEvent(this.agentName));
				send(msg);
			}
			state = SUSPENDED;
			return true;
		} else {
			printError("Only active agents can be suspended. Current state " + getStateDescription(state));
			return false;
		}
	}
	
	/**
	 * Resumes agent's activity.
	 */
	public boolean resume(){
		if(state == SUSPENDED){
			setActiveStatus();
			return true;
		} else {
			printError("Only suspended agents can be resumed. Current state " + getStateDescription(state));
			return false;
		}
		
	}

	/**
	 * Indicates if agent plays message filter role itself.
	 * @return
	 */
	public boolean isMessageFilter(){
		return messageFilterRolesRegistered;
	}
	
	/**
	 * Indicates if agent has message filters (in its group).
	 * @return
	 */
	public boolean hasMessageFilter(){
		if(getGroup().findRolesByTypeNonRecursive(MessageFilter.class, false).length > 0){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Adds a role to the agent and registers its role's intents.
	 * If message transport support is not enabled and a social role is
	 * initialized it will be enabled.
	 * @param role
	 */
	public synchronized void addRole(Role role){
		if(!getProhibitedRoles().contains(role.getClass())){
			//initializes role and should check whether role has been initialized before
			role.performThreadSafetyCheck();
			getRolesMap().add(role);
			getOwnerGroup().registerRoleForAgent(this, role);
			
			if(role instanceof MessageFilter){
				//print(new StringBuffer("Role ").append(role).append(" is Message filter."));
				activateMTSupport();
				getMessageFilterRoles().add((MessageFilter)role);
				messageFilterRolesRegistered = true;
			} else {
				if(role instanceof SocialRole){
					//print(new StringBuffer("Role ").append(role).append(" is social role."));
					activateMTSupport();
					getSocialRoles().add((SocialRole)role);
					for(int u=0; u<role.getApplicableIntentTypes().length; u++){
						getSocialIntentRoleMap().put(role.getApplicableIntentTypes()[u], (SocialRole)role);
					}
				} else {
					//print(new StringBuffer("Role ").append(role).append(" is reactive role."));
					getReactiveRoles().add((PassiveRole)role);
					for(int u=0; u<role.getApplicableIntentTypes().length; u++){
						getReactiveIntentRoleMap().put(role.getApplicableIntentTypes()[u], (PassiveRole)role);
					}
				}
			}
			Class[] intents = role.getApplicableIntentTypes();
			for(int i=0; i<intents.length; i++){
				if(MTRegistered){
					//put into central directory
					MTConnector.addApplicableIntent(agentName, intents[i]);
				}
				//put into local lookup map
				getIntentRoleMap().put(intents[i], role);
			}
			if(MTRegistered){
				MTConnector.registerRole(agentName, role);
			}
			//finally initialize role itself
			role.init(this);
		} else {
			printError("Tried to initialize prohibited role! Initialization aborted.");
		}
	}
	
	/**
	 * Returns all roles the agent currently executes.
	 */
	public Role[] getRoles(){
		return (Role[])getRolesMap().toArray(new Role[getRolesMap().size()]);
	}
	
	/**
	 * Returns all roles for a given role type the agent executes.
	 * @param aRoleType - Role type
	 */
	public Role[] getRoles(final Class aRoleType){
		Iterator<Role> it = getRolesMap().iterator();
		ArrayList<Role> list = new ArrayList<Role>();
		while(it.hasNext()){
			Role candidate = it.next();
			if(candidate.getClass().equals(aRoleType)){
				list.add(candidate);
			}
		}
		return (Role[])list.toArray(new Role[list.size()]);
	}
	
	/**
	 * Checks if agent plays a specified role instance.
	 * @param role - role instance to be checked.
	 * @return
	 */
	public boolean playsRole(Role role){
		return getRolesMap().contains(role);
	}
	
	/**
	 * Returns an array of all applicable intents for this agent.
	 * @return
	 */
	public Class[] getApplicableIntents(){
		return (Class[])getIntentRoleMap().keySet().toArray(new Class[getIntentRoleMap().keySet().size()]);
	}

	/**
	 * returns the group this agent belongs to (= lives in).
	 * @return
	 */
	public Group getOwnerGroup(){
		if(owner.getAgentName().equals(SystemOwner.getSystemOwnerName())){
			return SystemOwner.getInstance().getGroup();
		} else {
			return owner.getGroup();
		}
	}
	
	/**
	 * Returns this agent's own (sub) group and creates it
	 * if not existing (default empty).
	 * @return
	 */
	public Group getGroup() {
		if(group == null){
			group = new DefaultGroup(this);
		}
		return group;
	}
	
	/**
	 * Indicates if agent has group and potential sub-agents.
	 */
	public boolean hasGroup(){
		if(group != null){
			return true;
		}
		return false;
	}
	

	/**
	 * This function adds applicable intents for a given role instance. Role must
	 * be initialized on this agent. 
	 * @param intent - intent class to be added.
	 * @param role - role to add applicable intent for.
	 */
	protected synchronized void addApplicableIntentForRole(Class intent, Role role){
		if(!getRolesMap().contains(role)){
			throw new RoleNotInitializedOnThisAgentException(role, this);
		}
		//check if intent has already been added to role, if not, call role's addApplicableIntent method
		if(!role.hasApplicableIntentType(intent)){
			role.addApplicableIntent(intent);
		} else {
			semaphore.acquire();
			if(roles.contains(role)){
				getIntentRoleMap().put(intent, role);
				if(MTRegistered){
					MTConnector.addApplicableIntent(agentName, intent);
				}
				
				//when adding intents, check if role is registered as social role
				if(getSocialRoles().contains(role)){
					getSocialIntentRoleMap().put(intent, (SocialRole)role);
				} else {
					//then message filter (which are social roles as well)
					if(getMessageFilterRoles().contains(role)){
						getSocialIntentRoleMap().put(intent, (SocialRole)role);
					} else {
						//and else assume they are passive roles
						getReactiveIntentRoleMap().put(intent, (PassiveRole)role);
					}
				}
				//comprehensive rebuild of index - could be more granular ...
				getOwnerGroup().updateRoleIntentIndex();
			} else {
				printError("Tried to add intent for role which is not added to agent.");
			}
			semaphore.release();
		}
	}
	
	/**
	 * This function removes an applicable intent from a given role instance. Role must
	 * be initialized on this agent. 
	 * @param intent - intent class to be registered for role
	 * @param role - role instance
	 */
	protected synchronized void removeApplicableIntentForRole(Class intent, Role role){
		if(!getRolesMap().contains(role)){
			throw new RoleNotInitializedOnThisAgentException(role, this);
		}
		//if role still has the applicable intent, call remove from role
		if(role.hasApplicableIntentType(intent)){
			role.removeApplicableIntent(intent);
		} else {
			semaphore.acquire();
			Iterator<Class<Intent>> it = getIntentRoleMap().keySet().iterator();
			while(it.hasNext()){
				Class<Intent> tempIntent = it.next();
				if(tempIntent.equals(intent) && intentRoleMap.get(tempIntent).equals(role)){
					intentRoleMap.remove(tempIntent);
					it = intentRoleMap.keySet().iterator();
					if(MTRegistered){
						MTConnector.removeApplicableIntent(agentName, tempIntent);
					}
				}
			}
			getOwnerGroup().updateRoleIntentIndex();
			updateSpecificIntentMap();
			semaphore.release();
		}
	}
	
	/**
	 * This function removes all applicable intents for the given role instance. Role must
	 * be initialized on this agent. 
	 * @param role - role instance
	 */
	protected synchronized void clearApplicableIntentsForRole(Role role){
		if(!getRolesMap().contains(role)){
			throw new RoleNotInitializedOnThisAgentException(role, this);
		}
		if(role.getApplicableIntentTypes().length > 0){
			role.clearApplicableIntents();
		} else {
			semaphore.acquire();
			Iterator<Class<Intent>> it = getIntentRoleMap().keySet().iterator();
			while(it.hasNext()){
				Class<Intent> tempIntent = it.next();
				//double checking that it is there
				if(intentRoleMap.containsKey(tempIntent)){
					if(intentRoleMap.get(tempIntent).equals(role)){
						intentRoleMap.remove(tempIntent);
						it = intentRoleMap.keySet().iterator();
						if(MTRegistered){
							MTConnector.removeApplicableIntent(agentName, tempIntent);
						}
					}
				}
			}
			getOwnerGroup().updateRoleIntentIndex();
			updateSpecificIntentMap();
			semaphore.release();
		}
	}

	/**
	 * Re-indexes the social and reactive intent-to-role assignments.
	 */
	private void updateSpecificIntentMap(){
		//updates social role intent map
		getSocialIntentRoleMap().clear();
		Class<Intent>[] appIntents;
		for(int i=0; i<getSocialRoles().size(); i++){
			appIntents = ((Role)socialRoles.get(i)).getApplicableIntentTypes();
			for(int u=0; u<appIntents.length; u++){
				socialIntentRoleMap.put(appIntents[u], socialRoles.get(i));
			}
		}
		//updates reactive role intent map
		getReactiveIntentRoleMap().clear();
		for(int i=0; i<getReactiveRoles().size(); i++){
			appIntents = ((Role)reactiveRoles.get(i)).getApplicableIntentTypes();
			for(int u=0; u<appIntents.length; u++){
				reactiveIntentRoleMap.put(appIntents[u], reactiveRoles.get(i));
			}
		}
	}
	
	/**
	 * Disposes a role which is initialized on that agent. Checks if
	 * at least one role remains - agents play at least one role if living.
	 * @param role - role to be disposed.
	 */
	public void disposeRole(Role role){
		//checks if role is initialized on this agent
		if(!getRolesMap().contains(role)){
			throw new RoleNotInitializedOnThisAgentException(role, this);
		}
		//agent must be dead before releasing last role
		if(roles.contains(role) && roles.size() == 1 && state != DEAD){
			throw new InvalidDisposalOfRoleOnLivingAgent(role, this);
		}
		//check if role is played by agent at all
		if(roles.contains(role)){
			if(MTRegistered){
				MTConnector.unregisterRole(agentName, role);
			}
			roles.remove(role);
			getSocialRoles().remove(role);
			getReactiveRoles().remove(role);
			getMessageFilterRoles().remove(role);
			if(messageFilterRoles.isEmpty()){
				messageFilterRolesRegistered = false;
			}
			//delete intent entries
			updateSpecificIntentMap();
			//finally call dispose on the role itself (will doublecheck if deletion from agent was successful)
			role.dispose();
		}
	}
	
	/**
	 * Disposes of all roles of an agent (must only be called by dead agent).
	 */
	protected synchronized void disposeAllRoles(){
		semaphore.acquire();
		for(int i=0; i<getRolesMap().size(); i++){
			disposeRole(roles.get(i));
		}
		semaphore.release();
	}

	/**
	 * Adds a role to the list of prohibited roles (classes, not instances) for this agent.
	 * Roles initialized before adding prohibited roles won't be checked!
	 * @param role - role class to be prohibited
	 */
	public void prohibit(Class aRole) {
		semaphore.acquire();
		getProhibitedRoles().add(aRole);
		semaphore.release();
	}
	
	/**
	 * Removes a role from the list of prohibited roles for this agent.
	 * @param role - role class to be permitted
	 */
	public void permit(Class role) {
		semaphore.acquire();
		getProhibitedRoles().remove(role);
		semaphore.release();
	}
	
	/**
	 * Adds message filter (on a dedicated sub micro-agent) to this agent
	 * @param filter - Message filter instance
	 * @param messageFilterAgentName - Agent name for sub micro-agent
	 * @return
	 */
	public AgentController addMessageFilter(MessageFilter filter, String messageFilterAgentName){
		return this.getGroup().getAgentLoader().newAgent(filter, messageFilterAgentName);
	}
	
	/**
	 * Adds message filter (on a dedicated sub micro-agent) to this agent
	 * @param filter - Message filter instance
	 */
	public AgentController addMessageFilter(MessageFilter filter){
		return this.getGroup().getAgentLoader().newAgent(filter);
	}
	
	/**
	 * Returns an array of all running message filters (i.e. sub-agents
	 * dedicated to this role). It is returned
	 * as role array as MessageFilter instances instantiated as inner classes
	 * cannot be cast to MessageFilter.
	 * @return
	 */
	public Role[] getMessageFilters(){
		return this.getGroup().findRolesByTypeNonRecursive(MessageFilter.class, false);
	}
	
	/**
	 * Removes Message filter (from sub-group) identified by its role instance reference
	 * @param messageFilter
	 */
	public void removeMessageFilter(MessageFilter messageFilter){
		messageFilter.getAgent().die();
	}
	
	/**
	 * Removes Message filter (from sub-group) identified by agent name
	 * @param messageFilterAgentName
	 */
	public void removeMessageFilter(String messageFilterAgentName){
		Role[] filters = getMessageFilters();
		for(int i=0; i<filters.length; i++){
			if(filters[i].getAgent().getAgentName().equals(messageFilterAgentName)){
				filters[i].getAgent().die();
			}
		}
	}
	
	/**
	 * Clears all message filters (i.e. kills all message filter sub-agents)
	 */
	public void clearMessageFilters(){
		Role[] filters = getMessageFilters();
		for(int i=0; i<filters.length; i++){
			filters[i].getAgent().die();
		}
	}
	
	@Override
	public Role getRole(String roleName){
		for(Role role: getRolesMap()){
			if(role.getRoleName().equals(roleName)){
				return role;
			}
		}
		return null;
	}

}
