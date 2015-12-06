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
 * Publicly available agent controlling mechanism. This is to be used
 * by the group owners to manipulate the state, capabilities and life 
 * cycle of underlying agents.
 * 
 *<br><br>
 * AgentController.java<br>
 * Created: Mon Jul 23 12:57:08 2001<br>
 * @see SystemAgentLoader
 * @see AgentLoader
 * 
 * @author <a href="mariusz@rakiura.org">Mariusz Nowostawski</a> (Original author)
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> (Revision author)
 * @version $Revision: 2.0 $ $Date: 2010/06/10 00:00:00 $
 */
public final class AgentController {

	AbstractAgent agent;

	/** 
	 * Creates a new agent controller for a given agent.
	 * @param anAgent an agent.
	 */
	public AgentController(final AbstractAgent anAgent){
		this.agent = anAgent;
	}

	/**
	 * Suspends the agent.
	 */
	public void suspend() {
		this.agent.suspend();
	}
  
	/**
	 * Resumes the agent.
	 */
	public void resume(){
		this.agent.die();
	}
  
	/**
	 * Kills the agent.
	 */
	public void die(){
		this.agent.die();
	}
  
	/**
	 * Prohibits the given role from the agent.
	 * @param aRole a role to be prohibited.
	 */
	public void prohibit(final Class aRole){
		this.agent.prohibit(aRole);
	}
  
	/**
	 * Permits the role (removes it from the prohibited roles set). 
	 * @param aRole a Role to be permitted.
	 */
	public void permit(final Class aRole){
		this.agent.permit(aRole);
	}
  
	/**
	 * returns the agent for this agent controller.
	 */
	public AbstractAgent getAgent(){
		return this.agent;
	}

}
