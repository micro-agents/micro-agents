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

import java.util.HashMap;
import org.nzdis.micro.messaging.message.Message;

/**
 * UnsentMessagePool holds all unsent (not successfully sent) messages for later
 * analysis. To be enhanced with more specific strategies on automatic handling.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public class UnsentMessagePool extends HashMap<Message, String>{

	static UnsentMessagePool instance = null;
	
	private UnsentMessagePool(){
		
	}
	
	private static UnsentMessagePool getInstance(){
		if(instance == null){
			instance = new UnsentMessagePool();
		}
		return instance;
	}
	
	
	public synchronized static void putUnsentMessage(Message message, String address){
		System.err.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Message to ")
				.append(address).append(" was kept in unsent message backlog.").toString());
		getInstance().put(message, address);
	}
}
