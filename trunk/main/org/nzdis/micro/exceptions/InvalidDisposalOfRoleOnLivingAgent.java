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
package org.nzdis.micro.exceptions;

import org.nzdis.micro.Agent;
import org.nzdis.micro.Role;
import org.nzdis.micro.constants.AgentStates;
import org.nzdis.micro.messaging.MTRuntime;

public class InvalidDisposalOfRoleOnLivingAgent extends RuntimeException {

	public final String message;
	
	public InvalidDisposalOfRoleOnLivingAgent(Role role, Agent agent){
		message = new StringBuffer("Attempt to dispose the last role (")
			.append(role.getRoleName()).append(") of a non-DEAD agent (")
			.append(agent.getAgentName()).append(", State: ")
			.append(AgentStates.getStateDescription(agent.getStatus()))
			.append(")").append(MTRuntime.LINE_DELIMITER)
			.append("The agent who plays that role needs to be DEAD before the role can be disposed.").toString();
	}
	
	@Override
	public String toString(){
		return message;
	}
}
