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
 * Represents an abstract Role. Role is a type of behaviour which can be
 * played by an active participant: agent/actor. This is an abstract
 * tagging interface for Roles. This interface is the basis for 
 * implementations. For asynchronously interacting agents see
 * @link SocialRole, for purely reactive roles see @link PassiveRole.
 * 
 *<br><br>
 * Role.java<br>
 * Created: Wed Mar 14 11:47:59 2001<br>
 *
 * @author <a href="mariusz@rakiura.org">Mariusz Nowostawski</a> (Original author)
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> (Revision author)
 * @version $Revision: 2.0 $ $Date: 2011/06/15 00:00:00 $
 */
public interface Role {

	/**
	 * Initialization request to this role. A role can be initialized
	 * only once with single agent. When the agent is dead, the role
	 * implementation can be reused after being disposed from the
	 * previous ownership.<br> This method will throw 
	 * RuntimeException when attempting to initialize the same
	 * role implementation with more than one agent.
	 * @param anActor an agent who plays this role, an owner of this role.
	 */
	void init(final Agent anActor);
  
	/**
	 * Releases this role. The owner is dropped, and the state of the
	 * role implementation should be disposed. After this call the same
	 * role implementation can be reused in another agent. <br>
	 * This method can throw RuntimeException when attempting to dispose the role on
	 * behalf of not DEAD agent. Agent who plays this role needs to be
	 * DEAD before this role implementation can be disposed.
	 */
	void dispose();

	/**
	 * Adds an applicable intent for role.
	 * @param intent - intent class to be added.
	 */
	void addApplicableIntent(Class intent);
  
	/**
	 * Removes an applicable intent from role.
	 * @param intent - intent class to be removed.
	 */
	void removeApplicableIntent(Class intent);
  
	/**
	 * Clears all applicable intents from role.
	 */
	void clearApplicableIntents();
  
	/**
	 * Indicates if role can process passed intent type.
	 * @param intentType - intent type to be checked
	 * @return
	 */
	boolean hasApplicableIntentType(Class<Intent> intentType);
  
	/** 
	 * Returns all the intent types this role can deal with. 
	 * @return all applicable intent types.
	 */
	Class<Intent>[] getApplicableIntentTypes();
 
	/** 
	 * Returns the actor who currently plays this particular role.
	 * @return the actor who plays this role.
	 */
	Agent getAgent();

	/**
	 * Returns the role name.
	 * @return role name
	 */
	String getRoleName();
	
	/**
	 * Returns the role state (see org.nzdis.micro.constants.RoleStates).
	 * @return
	 */
	int getState();
  
	/**
	 * Performs safety check for Passive roles. Checks that each method has
	 * 'synchronized' keyword to avoid concurrent access to synchronously interacting
	 * roles.
	 */
	void performThreadSafetyCheck();

}
