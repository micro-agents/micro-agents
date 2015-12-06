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
 * Role representing a Group. This role is used to locate Roles and
 * Agents based on the appropriate types of Roles and Intents.
 * 
 *<br><br>
 * Group.java<br>
 * Created: Wed Mar 14 12:07:01 2001<br>
 *
 * @author <a href="mariusz@rakiura.org">Mariusz Nowostawski</a> (Original author)
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> (Revision author)
 * @version $Revision: 2.0 $ $Date: 2011/06/14 00:00:00 $
 */
public interface Group extends Role {
  
	/**
	 * Lookup method for locating agents. This method will prepare a list
	 * of all valid agents' names who implement a specific role type. Note, that
	 * if an actual role implements a subrole of a specified role, it
	 * will be returned as well via the default group implementation
	 * mechanism. However, the developer can implement this behaviour
	 * otherwise. 
	 * @param aRoleType specification of the requested role
	 * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
	 * @return Array of all valid agent names for a given specification */
	String[] findAgentNamesByRoleType(final Class roleType, boolean includeGroupOwnerInSearch);
	
	/**
	 * Lookup method for locating roles. Same as findAgentNamesByRoleType() but does not
	 * search recursively through sub-groups (levels).
	 * @param aRoleType specification of the requested role (in the given group)
	 * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
	 * @return Array of all valid agent names for a given specification
	 */
	String[] findAgentNamesByRoleTypeNonRecursive (final Class roleType, boolean includeGroupOwnerInSearch);
	
	/**
	 * Lookup method for locating agents. This method will prepare a list
	 * of all valid agents who implement a specific role type. Note, that
	 * if an actual role implements a subrole of a specified role, it
	 * will be returned as well via the default group implementation
	 * mechanism. However, the developer can implement this behaviour
	 * otherwise. 
	 * @param aRoleType specification of the requested role
	 * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
	 * @return Array of all valid agents for a given specification 
	 */
	Agent[] findAgentsByRoleType(final Class roleType, boolean includeGroupOwnerInSearch);
	
	/**
	 * Lookup method for locating roles. Same as findAgentsByRoleType() but does not
	 * search recursively through sub-groups (levels).
	 * @param aRoleType specification of the requested role (in the given group)
	 * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
	 * @return Array of all valid agent names for a given specification
	 */
	Agent[] findAgentsByRoleTypeNonRecursive (final Class roleType, boolean includeGroupOwnerInSearch);
	
	/**
	 * Lookup method for locating roles. This method will prepare a list
	 * of all valid roles implementing a specific role type. Note, that
	 * if an actual role implements a subrole of a specified role, it
	 * will be returned as well via the default group implementation
	 * mechanism. However, the developer can implement this behaviour
	 * otherwise. 
	 * @param aRoleType specification of the requested role
	 * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
	 * @return an array of all valid roles for a given specification */ 
	Role[] findRolesByType(final Class aRoleType, boolean includeGroupOwnerInSearch);
  
	/**
	 * Lookup method for locating roles. Same as findRolesByType() but does not
	 * search recursively through sub-groups (levels).
	 * @param aRoleType specification of the requested role (in the given group)
	 * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
	 * @return Array of all valid roles for a given specification
	 */
	Role[] findRolesByTypeNonRecursive(final Class aRoleType, boolean includeGroupOwnerInSearch);

	/**
	 * Lookup method for locating agents. This method will prepare a list
	 * of all valid agents which can achieve a specified intent recursively 
	 * throughout all sub-groups of this group.
	 * @param anIntentType specification of the requested intent
	 * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
	 * @return Array of all valid agents for a given intent specification 
	 */ 
	Agent[] findAgentsByIntent(final Class anIntentType, boolean includeGroupOwnerInSearch);

	/**
	 * Lookup method for locating agents. This method will prepare a list
	 * of all valid agents which can achieve a specified intent - BUT ONLY
	 * in this group, not in subgroups (Alternative: findAgentsByIntent()).
	 * @param anIntentType specification of the requested intent
	 * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
	 * @return an array of all valid agents for a given intent specification 
	 */
	Agent[] findAgentsByIntentNonRecursive(final Class anIntentType, boolean includeGroupOwnerInSearch);
  
	/**
	 * Lookup method for locating roles. This method will prepare a list
	 * of all working roles which can process a specified intent recursively 
	 * throughout all sub-groups of this group.
	 * @param anIntentType specification of the requested intent
	 * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
	 * @return Array of all roles that can process a given intent specification 
	 */
	Role[] findRolesByIntent(final Class intentType, boolean includeGroupOwnerInSearch);
  
	/**
	 * Lookup method for locating roles. This method will prepare a list	
	 * of all working roles which can process a specified intent - BUT ONLY
	 * in this group, not in subgroups (Alternative: findRolesByIntent()).
	 * @param anIntentType specification of the requested intent
	 * @param includeGroupOwnerInSearch indicates if group owner is to be included in lookup
	 * @return Array of all roles that can process a given intent specification 
	 */
	Role[] findRolesByIntentNonRecursive(final Class intentType, boolean includeGroupOwnerInSearch);
  
	/**
	 * Finds agent by name in this group (not across other groups).
	 * @param name Agent name to be looked up
	 * @return
	 */
	Agent findAgentByName(String name);
  
	/**
	 * Indicates if group is initialized with agent.
	 * @return
	 */
	boolean isInitialized();
	
	/**
	 * Registers given agent with this group and
	 * deregisters it from old group.
	 * @param anAgent Agent to be reregistered
	 */
	void reregister(final Agent anAgent);

	/**
	 * Registers role for agents after their initialization.
	 * @param agent Sgent to register role for
	 * @param role Role to be registered for agent
     */
	void registerRoleForAgent(final Agent agent, Role role);
  
	/**
	 * Reindexes all role-intent assignments for agents. This might be necessary
	 * when deleting intents from roles (or introducing the ability to remove roles 
	 * from agents).
	 */
	void updateRoleIntentIndex();
  
	/** 
	 * Returns all the agents registered within this group. 
	 * @return Array of all registered agents
	 */
	Agent[] getAgents();
	
	/**
	 * Returns all sub-agents in a recursive manner down to 
	 * the leaves of the hierarchy.
	 * @return Array of all registered agents
	 */
	Agent[] getAgentsRecursive();

	/**
	 * Returns an agent loader for this group (to add further agents 
	 * to this group).
	 * @return AgentLoader for this group.
	 */
	AgentLoader getAgentLoader();

	/**
	 * Sends a given message to all members of the group.
	 * @param message Message to be passed to all agents
	 */
	void sendGroupcast(MicroMessage message);
  
	/**
	 * Returns group's name.
	 * @return Group name
	 */
	String getGroupName();

}
