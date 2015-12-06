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
import org.nzdis.micro.Group;

public class GroupDeregistrationOfNonRegisteredAgentException extends RuntimeException {

	public final String message;
	
	public GroupDeregistrationOfNonRegisteredAgentException(Group group, Agent agent){
		message = new StringBuffer("Cannot deregister agent ").append(agent.getAgentName())
		.append(" from group ").append(group.getGroupName())
		.append(" as it is not member of this group.").toString();
	}
	
	@Override
	public String toString(){
		return message;
	}
	
}
