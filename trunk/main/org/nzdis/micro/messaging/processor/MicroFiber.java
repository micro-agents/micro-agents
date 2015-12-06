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
package org.nzdis.micro.messaging.processor;

import org.nzdis.micro.messaging.AbstractCommunicator;
import org.nzdis.micro.messaging.MessageCommunicator;
import org.nzdis.micro.messaging.message.Message;

/**
 * Specialization of AbstractMicroFiber.
 *
 */
public class MicroFiber extends AbstractMicroFiber
{
	public MicroFiber(MessageCommunicator agent) {
		super(agent);
	}

	private int accessCount = 0;
	
	public synchronized void service(Message message){
		if(accessCount >= 2){
			System.err.println(new StringBuffer("MicroFiber Message Passing: Discovered error when processing message ")
				.append(message.toString())
				.append("\nIt is likely that processing of a previous message failed! Access counter: ").append(accessCount));
		}
		accessCount++;
		try{
			agent.serveMessage(message);
		} catch(Exception e){
			System.err.println(new StringBuffer("MicroFiber Message Passing: Error during message processing by recipient ")
				.append(((AbstractCommunicator)agent).getAgentName()).append("!\nPlease check the application code."));
			e.printStackTrace();
		}
		accessCount--;
	}
}
