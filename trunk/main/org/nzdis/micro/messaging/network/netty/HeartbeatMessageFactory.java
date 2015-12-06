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
package org.nzdis.micro.messaging.network.netty;

import org.nzdis.micro.MicroMessage;

public class HeartbeatMessageFactory {

	public static MicroMessage getHeartbeatMessage(String connection){
		MicroMessage message = new MicroMessage(HeartbeatAgent.heartbeatRequestPerformative);
		//message.setRecipient(new StringBuffer(HeartbeatAgent.heartbeatAgentName).append("@").append(host).append(":").append(port).toString());
		message.setRecipient(HeartbeatAgent.heartbeatAgentName);
		message.setCustomField(HeartbeatAgent.heartbeatConnectionKeyword, connection);
		return message;
	}
	
}
