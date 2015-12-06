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

/**
 * Represents a generic group-wise agent loader. One should use 
 * {@link SystemAgentLoader SystemAgentLoader} for system-level 
 * agent instantiations. For local group-wise agent loading 
 * {@link Group Group} provides local AgentLoader.
 * 
 *<br><br>
 * AgentLoader.java<br>
 * Created: Thu Mar 15 13:37:42 2001<br>
 *
 * @author <a href="mariusz@rakiura.org">Mariusz Nowostawski</a> (Original author)
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> (Revision author)
 * @version $Revision: 2.0 $ $Date: 2011/06/14 00:00:00 $
 */
public class AgentLoader {

  /** Owner agent. */
  private Agent owner;

  /** default owner */
  AgentLoader () {
	  this.owner = SystemOwner.getInstance();
  }
  
  /**
   * Creates a new <code>AgentLoader</code> instance.
   * @param anOwner an <code>Agent</code> value
   */
  AgentLoader (final Agent anOwner) {
	  this.owner = anOwner;
  }

  /**
   * Instantiate an Agent for a given Role. This method will create an
   * Agent object with the given role capability. 
   *@param aRole Role implementation to be played by the instantiated
   * agent
   *@return AgentController for newly created agent
   */
  public AgentController newAgent (final Role aRole) {
	  return newAgent(aRole, "", "");
  }
  
  /**
   * Variant of newAgent() with customized name for agent.
   * @param aRole
   * @return
   */
  public AgentController newAgent (final Role aRole, String name) {
	  return newAgent(aRole, name, "");
  }
  
  /**
   * Variant of newAgent() with agent name as further parameter, and option
   * to use the passed agent name parameter as prefix for the actual agent name
   * which is then determined by the platform at runtime.
   * @param aRole - Role to be instantiated
   * @param name - Agent name to be assigned to new agent (if useNameAsPrefix is false)
   * @param useNameAsPrefix - indicates that agent name will only be used as prefix, actual name (appended ID) 
   * will be determined by platform at runtime.
   * @return
   */
  public AgentController newAgent (final Role aRole, String name, boolean useNameAsPrefix){
	  Role[] roleArray = new Role[1];
	  roleArray[0] = aRole;
	  return newAgent(roleArray, name, useNameAsPrefix, "");
  }

  /**
   * Variant of newAgent() with role to be instantiated, agent name, indicator
   * if agent name is only to be used as prefix (rest of name determined by platform)
   * and a Clojure script holding the agent implementation
   * @param aRole
   * @param name
   * @param useNameAsPrefix
   * @param cljScript
   * @return
   */
  public AgentController newAgent (final Role aRole, String name, boolean useNameAsPrefix, String cljScript){
	  Role[] roleArray = new Role[1];
	  roleArray[0] = aRole;
	  return newAgent(roleArray, name, useNameAsPrefix, cljScript);
  }
  
  /**
   * Variant of newAgent() with agent name as further parameter and optional
   * Clojure script for agent implementation
   * @param aRole
   * @param name
   * @param cljScript
   * @return
   */
  public AgentController newAgent (final Role aRole, String name, String cljScript){
	  Role[] roleArray = new Role[1];
	  roleArray[0] = aRole;
	  return newAgent(roleArray, name, false, cljScript);
  }
  
  /**
   * Variant of newAgent() with array of roles to be instantiated on new agent.
   * @param aRoleArray - array of roles
   * @return
   */
  public AgentController newAgent (final Role[] aRoleArray) {
	  return newAgent(aRoleArray, "", false, "");
  }
  
  /**
   * Variant of newAgent() with array of roles to be instantiated on new
   * agent, and option to declare agent name
   * @param aRoleArray
   * @param name
   * @return
   */
  public AgentController newAgent (final Role[] aRoleArray, String name) {
	  return newAgent(aRoleArray, name, false, "");
  }
  
  /**
   * Variant of newAgent() with role array and agent name as further parameter, 
   * and option to use the passed agent name parameter as prefix for the actual agent name
   * which is then determined by the platform at runtime.
   * @param aRoleArray
   * @param name
   * @param useNameAsPrefix
   * @return
   */
  public AgentController newAgent (final Role[] aRoleArray, String name, boolean useNameAsPrefix) {
	  return newAgent(aRoleArray, name, useNameAsPrefix, "");
  }
  
  /**
   * Variant of newAgent() with role array to be instantiated on new agent
   * with specified agent name. Optionally the passed agent name parameter 
   * can be used as prefix for the actual agent name (useNameAsPrefix true)
   * which is then determined by the platform at runtime. Additionally a 
   * Clojure script for the agent implementation can be passed.
   * @param aRoleArray
   * @param name
   * @param useNameAsPrefix
   * @param cljScript
   * @return
   */
  public AgentController newAgent (final Role[] aRoleArray, String name, boolean useNameAsPrefix, String cljScript){
	  final AnonymousAgent agent = new AnonymousAgent(this.owner, name, useNameAsPrefix, cljScript);
	  for(int i=0; i<aRoleArray.length; i++){
		  agent.addRole(aRoleArray[i]);
	  }
	  return new AgentController(agent);
  }

  /**
   * Instantiate an Agent for a given Agent class. This method will create an
   * Agent instance directly via the default agent constructor. 
   * @param anAgentClass Class of the agent to be instantiated
   * @return newly created agent instance or null if the agent cannot
   * be instantiated
   */
  public Agent newAgent (final Class anAgentClass) {
    try {
      final AbstractAgent agent = (AbstractAgent)anAgentClass.newInstance();
      ((DefaultGroup)this.owner.getGroup()).register(agent);
      return agent;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Registers an existing agent instance with the platform.
   * @param anAgent - agent instance
   * @return agent instance
   */
  public Agent newAgent (final Agent anAgent) {
	    try {
	      final AbstractAgent agent = (AbstractAgent) anAgent;
	      ((DefaultGroup)this.owner.getGroup()).register(agent);
	      return agent;
	    } catch (Exception e) {
	      e.printStackTrace();
	      return null;
	    }
	  }

  /** 
   * Finds all working agents for a given role type recursively throughout
   * all sub-groups.
   * @param aRoleType - Class of the role to be found
   * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
   * @return - array of agents which play the exact type or subtypes of
   * a specified role type
   */
  public Agent[] findAgentsByRoles(final Class aRoleType, boolean includeGroupOwnerInSearch){
	  return this.owner.getGroup().findAgentsByRoleType(aRoleType, includeGroupOwnerInSearch);
  }
  
  /** 
   * Finds all working agents' names for a given role type recursively throughout
   * all sub-groups.
   * @param aRoleType - Class of the role to be found
   * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
   * @return - array of agents' names who play of the exact type or subtypes of
   * a specified role type
   */
  public String[] findAgentNamesByRoles(final Class aRoleType, boolean includeGroupOwnerInSearch){
	  return this.owner.getGroup().findAgentNamesByRoleType(aRoleType, includeGroupOwnerInSearch);
  }
  
  /** 
   * Finds all working roles for a given role type recursively throughout
   * all sub-groups.
   * @param aRoleType - Class of the role to be found
   * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
   * @return - array of roles which are of the exact type or subtypes of
   * a specified role type
   */
  public Role[] findRoles (final Class aRoleType, boolean includeGroupOwnerInSearch) {
	  return this.owner.getGroup().findRolesByType(aRoleType, includeGroupOwnerInSearch);
  }
  
  /**
   * Finds all working roles for a given intent type recursively throughout
   * all sub-groups.
   * @param intentType - Intent type to be looked up.
   * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
   * @return - array of roles which can process the intent
   */
  public Role[] findRolesByIntent(final Class<Intent> intentType, boolean includeGroupOwnerInSearch){
	  return this.owner.getGroup().findRolesByIntent(intentType, includeGroupOwnerInSearch); 
  }
  
  /** 
   * Finds all existing Agents for a given intent type recursively throughout
   * all sub-groups.
   * @param intentType - Class for the intents to be found
   * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
   * @return array of Agents providing roles for a specified intent
   * type 
   */
  public Agent[] findAgentsByIntent (final Class<Intent> intentType, boolean includeGroupOwnerInSearch) {
	  return this.owner.getGroup().findAgentsByIntent(intentType, includeGroupOwnerInSearch);
  }

  /**
   * Finds an agent by its name across the whole hierarchy.
   * @param agentName
   * @return AbstractAgent holding name
   */
  public Agent findAgent(String agentName) {
	  AbstractAgent ag = MTConnector.getAgentForName(agentName);
	  if(ag != null){
		  return ag;
	  } else {
		  //do search across hierarchy
		  return SystemOwner.getInstance().getGroup().findAgentByName(agentName);
	  }
  }
  
  /**
   * Returns all agents registered in the group of this AgentLoader's owner.
   * @return
   */
  public Agent[] getAgents(){
	  return this.owner.getGroup().getAgents();
  }
  
  /**
   * Returns all agents of this group and all sub-agents of those.
   * @return
   */
  public Agent[] getAgentsRecursive(){
	  return this.owner.getGroup().getAgentsRecursive();
  }

  @Override
  public String toString() {
    return "AgentLoader: " + this.owner.getGroup();
  }
  
}
