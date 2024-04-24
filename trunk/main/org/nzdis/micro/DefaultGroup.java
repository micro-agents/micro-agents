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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.nzdis.micro.constants.RoleStates;
import org.nzdis.micro.exceptions.RegisteredOwnerInOwnGroupException;
import org.nzdis.micro.messaging.MTRuntime;
import org.nzdis.micro.util.SimpleSemaphore;

/**
 * Default implementation of a Group role. Each agent by default can
 * play Group via this implementation. This implementation uses native
 * arrays for efficiency - the code is not optimized for readability,
 * but for performance. 
 * 
 *<br><br>
 * DefaultGroup.java<br>
 * Created: Wed Mar 14 11:05:48 2001<br>
 *
 * @author <a href="mariusz@rakiura.org">Mariusz Nowostawski</a> (Original author)
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> (Revision author)
 * @version $Revision: 2.0 $ $Date: 2011/06/22 00:00:00 $
 */
final class DefaultGroup extends AbstractRole implements Group {

  private Agent groupOwner;
  private transient AgentLoader loader;
  private String groupName;

  // List of Roles
  private List<Role> roles = null;
  
  private List<Role> getRolesList(){
	  if(roles == null){
		  roles =  new ArrayList<Role>(2);
	  }
	  return roles;
  }
  
  // List of Agents
  private List<Agent> agents = null;

  private List<Agent> getAgentsList(){
	  if(agents == null){
		  agents = new ArrayList<Agent>(2);
	  }
	  return agents;
  }
  
  // List of Intents
  private List<Class> intents = null;
  
  private List<Class> getIntentsList(){
	  if(intents == null){
		  intents = new ArrayList<Class>(3);
	  }
	  return intents;
  }
  
  // Mapping of intents to agents
  private ConcurrentHashMap<Class, Agent> intentAgentMap = null;

  private ConcurrentHashMap<Class, Agent> getIntentAgentMap(){
	  if(intentAgentMap == null){
		  intentAgentMap = new ConcurrentHashMap<Class, Agent>(3);
	  }
	  return intentAgentMap;
  }
  
  private transient SimpleSemaphore semaphore = null;

  /** Default constructor. */
  public DefaultGroup(){}

  /** Creates new Group role from an agent. */
  public DefaultGroup(Agent owner){
	  init(owner);
	  //handleInitialization(owner);
  }

  /** Initialize this role. */
  @Override
  public void init(final Agent anAgent){
	  //super.init(anAgent);
	  //do not use conventional initialization (via super.init(), instead simply override core method isInitialized())
	  handleInitialization(anAgent);
  }
  
  @Override
  public boolean isInitialized(){
	  return this.groupOwner != null;
  }
  
  private void handleInitialization(Agent anAgent){
	  this.groupOwner = anAgent;
	  this.loader = new AgentLoader(anAgent);
	  setGroupName(groupOwner.getAgentName() + "_Group");
  }
  
  /**
   * Sets the group name after initialization
   * @param groupName
   */
  public void setGroupName(String groupName){
	  this.groupName = groupName;
	  this.roleName = groupName;
	  semaphore = new SimpleSemaphore(groupName + "_Semaphore");
  }
  
  /**
   * Returns the group's name.
   * @return
   */
  public String getGroupName(){
	  return this.groupName;
  }
  
  /** Returns an AgentLoader for this group. */
  public final AgentLoader getAgentLoader() {
    return this.loader;
  }
  
  /**
   * Looks up agents by their intent capabilities recursively throughout
   * all sub-groups (Alternative: findAgentsByIntentNonRecursive()).
   * @param intentType type of the intent
   * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
   * @return array of agents capable of achieving the given intent.
   */
  @Override
  public final Agent[] findAgentsByIntent(final Class intentType, boolean includeGroupOwnerInSearch) {  
	  return agentByIntentSearch(intentType, true, includeGroupOwnerInSearch);
  }
  
  /**
   * Looks up agents by their intent capabilities. DOES NOT
   * work recursively, but only looks up in this agent's group.
   * @param intentType - intent type to be looked up
   * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
   * @return - array of agents
   */
  @Override
  public final Agent[] findAgentsByIntentNonRecursive(final Class intentType, boolean includeGroupOwnerInSearch) {  
	  return agentByIntentSearch(intentType, false, includeGroupOwnerInSearch);
  }

  private Agent[] agentByIntentSearch(final Class intentType, boolean recursive, boolean includeGroupOwner){
	  final List<Agent> aList = new ArrayList<Agent>();
	  
	  if(includeGroupOwner){
		  Class[] intentTypes = groupOwner.getApplicableIntents();
		  for(int i = 0; i < intentTypes.length; i++){
			  if(intentTypes[i].equals(intentType)){
				  if(!aList.contains(groupOwner)){
					  aList.add(groupOwner);
					  break;
				  }
			  }
		  }
	  }
	  
	  if(getIntentsList().contains(intentType)){
		  Agent agent = getIntentAgentMap().get(intentType);
		  if(!aList.contains(agent)){
			  aList.add(agent);
		  }
	  }
	    
	  if(recursive){
		  //recursive search
		  for (int i = 0; i < getAgentsList().size(); i++ ) {
			  final Agent[] p = ((Agent)agents.get(i)).getGroup().findAgentsByIntent(intentType, false);
			  for (int j = 0; j < p.length; j++ ){
				  Agent agent = p[j];
				  if(!aList.contains(agent)){
					  aList.add(agent);
				  }
			  }
		  }
	  }
	  return (Agent[])aList.toArray(new Agent[aList.size()]);
  }
  
  /**
   * Looks up agent names by their capabilities recursively throughout
   * all sub-groups (Alternative: findAgentNamesByRoleTypeNonRecursive()). 
   * @param roleType type of the role
   * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
   * @return array of agent names for a given capability.
   */
   @Override
   public synchronized final String[] findAgentNamesByRoleType(final Class roleType, boolean includeGroupOwnerInSearch) {
 	  return agentNameByRoleSearch(roleType, true, includeGroupOwnerInSearch);
   }

   /**
    * Looks up agents by their capabilities BUT ONLY in THIS GROUP.
    * findAgentNamesByRoleType() looks up recursively (throughout sub-groups, ...). 
    * @param roleType type of the role
    * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
    * @return array of agent names for a given capability.
    */
   @Override
    public synchronized final String[] findAgentNamesByRoleTypeNonRecursive (final Class roleType, boolean includeGroupOwnerInSearch) {
 	   return agentNameByRoleSearch(roleType, false, includeGroupOwnerInSearch);
    }
   
  private String[] agentNameByRoleSearch(final Class roleType, boolean recursive, boolean includeGroupOwner){
	   	final List<String> aList = new ArrayList<String>();
	    
	   	if(includeGroupOwner){
	   		Role[] ownerRoles = groupOwner.getRoles();
	   		for(int i = 0; i < ownerRoles.length; i++) {
	  	      if(roleType.isInstance(ownerRoles[i])){
	  	    	  Role role = ownerRoles[i];
	  	    	  if(!aList.contains(role.getAgent().getAgentName())){
	  	    		  aList.add(role.getAgent().getAgentName());
	  	    	  }
	  	      }
	  	    }
	   	}
	   	
	   	//search this group
	    for (int i = 0; i < getRolesList().size(); i++) {
	      if (roleType.isInstance(roles.get(i))){
	    	  Role role = roles.get(i);
	    	  if(!aList.contains(role.getAgent().getAgentName())){
	    		  aList.add(role.getAgent().getAgentName());
	    	  }
	      }
	    }
	    
	    if(recursive){
		    //recursive search
		    for (int i = 0; i < getAgentsList().size(); i++ ) {
		      final Role[] p = ((AbstractAgent) agents.get(i)).getGroup().findRolesByType(roleType, false);
		      for (int j = 0; j < p.length; j++ ) {
		    	  Role role = p[j];
		    	  if(!aList.contains(role.getAgent().getAgentName())){
		    		  aList.add(role.getAgent().getAgentName());
		    	  }
		      }
		    }
	    } 
	    return (String[]) aList.toArray (new String[aList.size()]);
	  }
  
  /**
   * Looks up agents by their capabilities recursively throughout
   * all sub-groups (Alternative: findAgentsByRoleTypeNonRecursive()). 
   * @param roleType type of the role
   * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
   * @return array of agents for a given capability.
   */
   @Override
   public synchronized final Agent[] findAgentsByRoleType(final Class roleType, boolean includeGroupOwnerInSearch) {
 	  return agentByRoleSearch(roleType, true, includeGroupOwnerInSearch);
   }

   /**
    * Looks up agents by their capabilities BUT ONLY in THIS GROUP.
    * findAgentsByRoleType() looks up recursively (throughout sub-groups, ...). 
    * @param roleType type of the role
    * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
    * @return array of agents for a given capability.
    */
   @Override
    public synchronized final Agent[] findAgentsByRoleTypeNonRecursive (final Class roleType, boolean includeGroupOwnerInSearch) {
 	   return agentByRoleSearch(roleType, false, includeGroupOwnerInSearch);
    }
  
  private Agent[] agentByRoleSearch(final Class roleType, boolean recursive, boolean includeGroupOwner){
   	final List<Agent> aList = new ArrayList<Agent>();
    
   	if(includeGroupOwner){
   		Role[] ownerRoles = groupOwner.getRoles();
   		for(int i = 0; i < ownerRoles.length; i++) {
  	      if(roleType.isInstance(ownerRoles[i])){
  	    	  Role role = ownerRoles[i];
  	    	  if(!aList.contains(role.getAgent())){
  	    		  aList.add(role.getAgent());
  	    	  }
  	      }
  	    }
   	}
   	
   	//search this group
    for (int i = 0; i < getRolesList().size(); i++) {
      if (roleType.isInstance(roles.get(i))){
    	  Role role = roles.get(i);
    	  if(!aList.contains(role.getAgent())){
    		  aList.add(role.getAgent());
    	  }
      }
    }
    
    if(recursive){
	    //recursive search
	    for (int i = 0; i < getAgentsList().size(); i++ ) {
	      final Role[] p = ((AbstractAgent) agents.get(i)).getGroup().findRolesByType(roleType, false);
	      for (int j = 0; j < p.length; j++ ) {
	    	  Role role = p[j];
	    	  if(!aList.contains(role.getAgent())){
	    		  aList.add(role.getAgent());
	    	  }
	      }
	    }
    } 
    return (Agent[]) aList.toArray (new Agent[aList.size()]);
  }
  
  
  /**
  * Looks up agents by their capabilities recursively throughout
  * all sub-groups (Alternative: findRolesNonRecursive()). 
  * @param roleType type of the role
  * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
  * @return array of roles for a given capability.
  */
  @Override
  public synchronized final Role[] findRolesByType(final Class roleType, boolean includeGroupOwnerInSearch) {
	  return roleByRoleSearch(roleType, true, includeGroupOwnerInSearch);
  }

  /**
   * Looks up agents by their capabilities BUT ONLY in THIS GROUP.
   * findRoles() looks up recursively (throughout sub-groups, ...). 
   * @param roleType type of the role
   * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
   * @return array of roles for a given capability.
   */
   public synchronized final Role[] findRolesByTypeNonRecursive (final Class roleType, boolean includeGroupOwnerInSearch) {
	   return roleByRoleSearch(roleType, false, includeGroupOwnerInSearch);
   }
  
   private Role[] roleByRoleSearch(final Class roleType, boolean recursive, boolean includeGroupOwner){
	   	final List<Role> rList = new ArrayList<Role>();
	    
	   	if(includeGroupOwner){
	   		Role[] ownerRoles = groupOwner.getRoles();
	   		for(int i = 0; i < ownerRoles.length; i++) {
	  	      if(roleType.isInstance(ownerRoles[i])){
	  	    	  Role role = ownerRoles[i];
	  	    	  if(!rList.contains(role)){
	  	    		  rList.add(role);
	  	    	  }
	  	      }
	  	    }
	   	}
	   	
	   	//search this group
	    for (int i = 0; i < getRolesList().size(); i++) {
	      if (roleType.isInstance(roles.get(i))){
	    	  Role role = roles.get(i);
	    	  if(!rList.contains(role)){
	    		  rList.add(role);
	    	  }
	      }
	    }
	    
	    if(recursive){
		    //recursive search
		    for (int i = 0; i < getAgentsList().size(); i++ ) {
		      final Role[] p = ((AbstractAgent) agents.get(i)).getGroup().findRolesByType(roleType, false);
		      for (int j = 0; j < p.length; j++ ) {
		    	  Role role = p[j];
		    	  if(!rList.contains(role)){
		    		  rList.add(role);
		    	  }
		      }
		    }
	    } 
	    return (Role[]) rList.toArray (new Role[rList.size()]);
   }
   
   /**
    * Finds roles by passed intent type recursively throughout
    * all sub-groups (Alternative: findRolesByIntentNonRecursive())
    * @param intentType - Intent type to be looked up.
    * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
    */
   public Role[] findRolesByIntent(final Class intentType, boolean includeGroupOwnerInSearch){
	   return roleByIntentSearch(intentType, true, includeGroupOwnerInSearch);
   }
   
   /**
    * Looks up agents by their capabilities BUT ONLY in THIS GROUP.
    * findRolesByIntent() looks up recursively (throughout sub-groups, ...).
    * @param intentType Intent type to be looked up.
    * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
    */
   public Role[] findRolesByIntentNonRecursive(final Class intentType, boolean includeGroupOwnerInSearch){
	   return roleByIntentSearch(intentType, false, includeGroupOwnerInSearch);
   }
   
   private Role[] roleByIntentSearch(final Class intentType, boolean recursive, boolean includeGroupOwner){
	   	final List<Role> rList = new ArrayList<Role>();
	   	
	   	if(includeGroupOwner){
	   		Role[] ownerRoles = groupOwner.getRoles();
	   		for(int i = 0; i < ownerRoles.length; i++) {
	  	      if(ownerRoles[i].hasApplicableIntentType(intentType)){
	  	    	  Role role = ownerRoles[i];
	  	    	  if(!rList.contains(role)){
	  	    		  rList.add(role);
	  	    	  }
	  	      }
	  	    }
	   	}
	   	
	    // search this group
	    for(int i=0; i<getRolesList().size(); i++) {
	      if(roles.get(i).hasApplicableIntentType(intentType)){
	    	  Role role = roles.get(i);
	    	  if(!rList.contains(role)){
	    		  rList.add(role);
	    	  }
	      }
	    }
	    
	    if(recursive){
	    	for(int i = 0; i < getAgentsList().size(); i++){
	    		final Role[] p = ((AbstractAgent) agents.get(i)).getGroup().findRolesByIntent(intentType, false);
	    		for (int j = 0; j < p.length; j++ ){
	    			Role role = p[j];
	    			if(!rList.contains(role)){
	  	    		  rList.add(role);
	  	    	  	}
			    }
	    	}
	    }
	    return (Role[]) rList.toArray (new Role [rList.size()]);
   }
   
   /**
    * Finds agents by name inside this group and in sub-groups.
    * @param name - Agent name to be looked up
    * @return resolved agent
    */
   public Agent findAgentByName(String name){
	   for(int i = 0; i < getAgentsList().size(); i++){
		   if(agents.get(i).getAgentName().equals(name)){
			   return agents.get(i);
		   }
	   }
	   for(int i=0; i<agents.size(); i++){
		   return agents.get(i).getGroup().findAgentByName(name);
	   }
	   return null;
   }
   
  /**
   * Registers a role for an agent after its initialization
   * @param agent Agent the role to be registered for
   * @param role role instance to be registered
   */
  public synchronized void registerRoleForAgent(final Agent agent, Role role){
	  semaphore.acquire();
	  boolean roleNotExisting = true;
	  for(int l = 0; l < getRolesList().size(); l++){
		  if(roles.get(l).equals(role)){
			  roleNotExisting = false;
		  }
	  }
	  if(!roleNotExisting){
		  System.err.println(new StringBuffer(groupName).append(": Role update for already registered role has been attempted."));
	  } else {
		  boolean valid = false;
		  for(int u = 0; u < getAgentsList().size(); u++){
			  if(agents.get(u).equals(agent)){
				  roles.add(role);
				  Class[] gs = role.getApplicableIntentTypes();
			      for (int j = 0; j < gs.length; j++) {
			        addIntent(gs[j], agent);
			        addApplicableIntent(gs[j]);
			      }
			      valid = true;
			  }
		  }
		  if(!valid){
			  System.err.println(new StringBuffer(groupName).append(": Role update for non-registered agent has been attempted."));
		  }
	  }
	  semaphore.release();
  }
  
  /**
   * Updates the role intent assignments and re-indexes all roles for all agents. This
   * is necessary (in the unlikely case) of deleting goals from roles.
   */
  public synchronized void updateRoleIntentIndex(){
	  semaphore.acquire();
	  getRolesList().clear();
	  getIntentsList().clear();
	  getIntentAgentMap().clear();
	  for(int u=0; u<getAgentsList().size(); u++){
		  final Role[] r = ((AbstractAgent)agents.get(u)).getRoles();
	      for (int i=0; i<r.length; i++) {
	    	  roles.add (r[i]);
	    	  Class[] gs = r[i].getApplicableIntentTypes();
	    	  for (int j=0; j<gs.length; j++) {
	    		  addIntent(gs[j], ((AbstractAgent)agents.get(u)));
	    		  addApplicableIntent(gs[j]);
	    	  }
	      }
	  }
	  semaphore.release();
  }
  
  public synchronized void updateRoleIntentAssignments(final AbstractAgent agent, Role r){
	  semaphore.acquire();
	  boolean roleNotExisting = true;
	  for(int l=0; l<getRolesList().size(); l++){
		  if(roles.get(l).equals(r)){
			  roleNotExisting = false;
		  }
	  }
	  if(roleNotExisting){
		  System.err.println(new StringBuffer(groupName).append(": Role update for not registered role has been attempted."));
	  } else {
		  boolean valid = false;
		  for(int u=0; u<getAgentsList().size(); u++){
			  if(agents.get(u).equals(agent)){
				  roles.add(r);
				  Class[] gs = r.getApplicableIntentTypes();
			      for (int j=0; j<gs.length; j++) {
			        addIntent(gs[j], agent);
			        addApplicableIntent(gs[j]);
			      }
			      valid = true;
			  }
		  }
		  if(!valid){
			  System.err.println(new StringBuffer(groupName).append(": Role update for non-registered agent has been attempted."));
		  }
	  }
	  semaphore.release();
  }
  
  /**
   * Registers an agent with this group.
   * @param agent Agent to be registered with this group.
   */
  public synchronized void register(final Agent agent) {
	  if(agent.equals(groupOwner)){
		  throw new RegisteredOwnerInOwnGroupException(this, agent);
	  }
	  semaphore.acquire();
	  getAgentsList().add(agent);
	  ((AbstractAgent)agent).owner = (AbstractAgent)this.groupOwner;
	  final Role[] r = agent.getRoles();
	  for (int i=0; i<r.length; i++) {
		  getRolesList().add(r[i]);
		  Class[] gs = r[i].getApplicableIntentTypes();
		  for (int j=0; j<gs.length; j++) {
			  addIntent(gs[j], agent);
			  addApplicableIntent(gs[j]);
		  }
	  }
	  semaphore.release();
	  if(MTRuntime.getOutputLevel() > 2){
		  System.out.println(new StringBuffer(groupName).append(": Agent ")
				  .append(agent).append(" has been registered."));
	  }
  }

  /**
   * Deregisters an agent from this group.
   * @param agent Agent to be deregistered from this group. 
   */
  protected synchronized void deregister(final Agent agent) {
	  //System.out.println("Deregistering agent " + agent.getAgentName());
	  if(getAgentsList().contains(agent)){
		  semaphore.acquire();
		  final Role[] r = agent.getRoles();
		  ((AbstractAgent)agent).owner = null;
		  for (int i=0; i < r.length; i++) {
			  removeRole (r[i]);
			  Class[] gs = r[i].getApplicableIntentTypes();
			  for (int j=0; j<gs.length; j++) {
				  removeIntent(gs[j]);
				  removeApplicableIntent(gs[j]);
			  }
		  }
		  agents.remove(agent);
		  semaphore.release();
		  if(MTRuntime.getOutputLevel() > 2){
			  System.out.println(new StringBuffer(groupName).append(": Agent ")
					  .append(agent).append(" has been deregistered."));
		  }
	  }
  }
  
  /**
   * Registers agent with this group and deregisters it from old group.
   * @param agent - agent to be reregistered.
   */
  public synchronized void reregister(final Agent agent){
	  ((DefaultGroup)agent.getOwnerGroup()).deregister(agent);
	  register(agent);
	  if(MTRuntime.getOutputLevel() > 2){
		  System.out.println(new StringBuffer(groupName).append(": Agent ")
				  .append(agent).append(" has been reregistered to this group."));
	  }
  }

  /**
   * Returns all the agents from this group. 
   *@return An array with all the agents from this group. 
   */
  public synchronized Agent[] getAgents() {
    return (Agent[])getAgentsList().toArray(new Agent[getAgentsList().size()]);
  }

  /** 
   * Returns all sub-agents in a recursive manner.
   * @return
   */
  public synchronized Agent[] getAgentsRecursive(){
	  List<Agent> aList = new ArrayList<Agent>();
	  
	  Agent[] ags = getAgents();
	  for(int i=0; i<ags.length; i++){
		  aList.add(ags[i]);
		  aList = getRecursiveSubAgents(ags[i], aList);
	  }
	  return (Agent[])aList.toArray(new Agent[aList.size()]);
  }
	  
  private List<Agent> getRecursiveSubAgents(Agent agent, List<Agent> list){
	  if(agent.hasGroup()){
		  Agent[] subAgs = agent.getGroup().getAgents();
		  for(int i=0; i<subAgs.length; i++){
			  list.add(subAgs[i]);
			  list = getRecursiveSubAgents(subAgs[i], list);
		  }
	  }
	  return list;
  }
  
  private final void removeRole(final Role role) {
	  int i;
	  for(i=0; i < getRolesList().size(); i++){
		  if(roles.get(i) == role) {//pointer comparison
			  roles.remove(i);
			  i--;
		  }
	  }
  }
  
  private final void addIntent (final Class intentType, final Agent a) {
	  getIntentsList().add(intentType);
	  getIntentAgentMap().put(intentType, a);
  }
  
  private final void removeIntent (final Class intent) {
	  int i;
	  for(i=0; i<getIntentsList().size(); i++){
		  if(intents.get(i) == intent) {//pointer comparison
			  intents.remove(i);
			  i--;
		  }
	  }
	  getIntentAgentMap().remove(intent);
  }
  
  public String toString() {
	  final StringBuffer s = new StringBuffer();
	  s.append("Group '").append(groupName).append("' ");
	  s.append("(DefaultGroup). Owner: ");
	  if(this.groupOwner != null){
		  s.append(this.groupOwner.getAgentName());
	  } else {
		  s.append("State: ").append(RoleStates.getStateDescription(getState()));
	  }
	  s.append("\nRoles: ");
	  for (int i = 0; i < getRolesList().size(); i++) {
		  s.append (roles.get(i).toString());
		  if(i < (getRolesList().size() - 1)){
			  s.append(", ");
		  }
	  }
	  s.append("\nAgents: ");
	  for (int i = 0; i < getAgentsList().size(); i++) {
		  s.append (agents.get(i).toString()).append(" (").append(((AbstractAgent)agents.get(i)).getAgentName()).append (")");
		  if(i < (getRolesList().size() - 1)){
			  s.append(", ");
		  }
	  }
	  return s.toString();
  }

  /**
   * sends message to all agents within group.
   */
  @Override
  public void sendGroupcast(MicroMessage message) {
	  if(message.getSender().equals("")){
		  System.out.println(MTConnector.getPlatformPrefix() + "Groupcast not delivered as message not sent by agent.");
	  } else {
		  for(Agent agent: getAgentsList()){
			  message.setRecipient(agent.getAgentName());
			  final MicroMessage msg = (MicroMessage) message.clone();
			  MTConnector.send(msg);
		  }
	  }
  }

	@Override
	protected void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void release() {
		// TODO Auto-generated method stub
		
	}

}
