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
import org.nzdis.micro.messaging.MTRuntime;

public class RegisteredOwnerInOwnGroupException extends RuntimeException {

	public final String message;
	
	public RegisteredOwnerInOwnGroupException(Group group, Agent owner){
		message = new StringBuffer("Cannot register agent as member of its own group. ")
			.append(MTRuntime.LINE_DELIMITER)
			.append("Agent ").append(owner.getAgentName()).append(" is owner of group ")
			.append(group.getGroupName()).toString();
	}
	
	@Override
	public String toString(){
		return message;
	}
	
}
