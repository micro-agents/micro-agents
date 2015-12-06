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
package org.nzdis.micro.constants;

public abstract class AgentStates extends States {

	/** 
	 * Status constant. Represents a newly created agent state;
	 * agent was not initialized and activated yet.
	 */
	public static final int CREATED = 0;
	
	/** 
	 * Status constant. Represents an active working agent. 
	 */
	public static final int ACTIVE = 1;
	
	/** 
	 * Status constant. Represents an agent in a suspended state. Suspended agent 
	 * can be reactivated and will continue its course of operations. 
	 */
	public static final int SUSPENDED = 2;
	
	/** 
	 * Status constant. Represents a dying agent. Agent will not commit to any new goals 
	 * and will not provide any roles to other agents. 
	 */
	public static final int DYING = 3;
	
	/** 
	 * Status constant. Agent is not active anymore, and is subject to garbage collection. 
	 * Dead agent cannot be reactivated.
	 */
	public static final int DEAD = 4;
	
	/**
	 * Resolves state value to human-readable representation.
	 * @param state int value of role state
	 * @return String representation for state
	 */
	public static String getStateDescription(int state){
		switch(state){
			case CREATED:
				return "CREATED";
			case ACTIVE:
				return "ACTIVE";
			case SUSPENDED:
				return "SUSPENDED";
			case DYING:
				return "DYING";
			case DEAD:
				return "DEAD";
		}
		return UNDEFINED_STATE;
	}
	
}
