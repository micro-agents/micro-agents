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

import org.nzdis.micro.inspector.annotations.CollectiveView;

/**
 * This interface describes the contract for agent implementations on the
 * OPAL micro-agent framework.
 * 
 * Created: 26/06/2010
 * 
 * @author <a href="mariusz@rakiura.org">Mariusz Nowostawski</a> (Original author)
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> (Revision author)
 * @version $Revision: 2.0 $ $Date: 2011/06/15 00:00:00 $
 *
 */
public interface Agent {

	/**
	 * Returns agent's name.
	 * @return
	 */
	public String getAgentName();
	
	/**
	 * Returns agent status (see AgentStates for details)
	 * @return
	 */
	public int getStatus();
	
	/**
	 * Returns this agent's owner.
	 * @return
	 */
	public Agent getOwner();
	
	/**
	 * Kills the agent.
	 */
	public void die();
	
	/**
	 * Suspends the agent from execution.
	 * @return
	 */
	public boolean suspend();
	
	/**
	 * Resumes the agent's execution.
	 * @return
	 */
	public boolean resume();
	
	/**
	 * configures the sub-agent handling strategy upon death of agent
	 * (true --> kill sub-agents on death
	 * false --> reassign sub-agents to owner agent upon death)
	 * @param strategy (boolean with values above)
	 */
	public void killSubHierarchyUponDeath(boolean strategy);
	
	/**
	 * Returns this agent's owner's group.
	 * @return
	 */
	public Group getOwnerGroup();
	
	/**
	 * Returns this agent's group.
	 * @return
	 */
	public Group getGroup();
	
	/**
	 * Indicates if agent owns a group (and potentially sub-agents).
	 * @return
	 */
	public boolean hasGroup();
	
	/**
	 * Adds a role instance to the agent and registers its role's intents.
	 * If message transport support is not enabled and the role to be 
	 * played is a social role it will be enabled.
	 * @param role Role to be initialized on agent.
	 */
	public void addRole(Role role);
	
	/**
	 * Disposes role from this agent.
	 * @param role
	 */
	public void disposeRole(Role role);
	
	/**
	 * Returns all roles played by this agent.
	 * @return
	 */
	@CollectiveView
	public Role[] getRoles();
	
	/**
	 * Indicates if agent plays passed role instance.
	 * @param role Role instance
	 * @return
	 */
	public boolean playsRole(Role role);
	
	/**
	 * Returns all roles of a given role type played
	 * by this agent.
	 * @param aRoleType
	 * @return
	 */
	public Role[] getRoles(final Class aRoleType);
	
	/**
	 * Returns all intents applicable for this agent.
	 * @return
	 */
	public Class[] getApplicableIntents();
	
	/**
	 * Returns the role instance played by this agent for a given role name.
	 * @param roleName Role name
	 * @return Role instance
	 */
	public Role getRole(String roleName);
	
	/**
	 * Indicates if agent has message filters (in its group).
	 * The agent does not need to be message filter itself.
	 * @return
	 */
	public boolean hasMessageFilter();
	
	/**
	 * Indicates if agent plays a message filter role itself.
	 * @return
	 */
	public boolean isMessageFilter();
	
	/**
	 * Initialize a message filter role as a new sub-agent in this agent's group. 
	 * @param filter Message filter instance
	 * @param messageFilterAgentName Name for the sub-agent playing the message filter
	 * @return
	 */
	public AgentController addMessageFilter(MessageFilter filter, String messageFilterAgentName);
	
	/**
	 * Initialize a message filter role as a new sub-agent in this agent's group.
	 * Automatically assigns agent name.
	 * @param filter Message filter instance
	 * @return
	 */
	public AgentController addMessageFilter(MessageFilter filter);
	
	/**
	 * Returns all message filters registered for this agent (as sub-agents in its group).
	 * @return
	 */
	public Role[] getMessageFilters();
	
	/**
	 * Removes message filter of this agent 
	 * (i.e. this agent's sub-agent playing the role).
	 * @param filter Message filter instance
	 */
	public void removeMessageFilter(MessageFilter filter);
	
	/**
	 * Removes message filter of this agent, identified by sub-agent's name.
	 * @param messageFilterAgentName Name of sub-agent playing message filter role
	 */
	public void removeMessageFilter(String messageFilterAgentName);
	
	/**
	 * Removes all sub-agents acting as message filters registered in this agent's group.
	 */
	public void clearMessageFilters();
}
