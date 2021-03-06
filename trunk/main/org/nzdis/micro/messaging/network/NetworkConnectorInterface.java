/*******************************************************************************
 * �� - Micro-Agent Platform, core of the Otago Agent Platform (OPAL),
 * developed at the Information Science Department, 
 * University of Otago, Dunedin, New Zealand.
 * 
 * This file is part of the aforementioned software.
 * 
 * �� is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * �� is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Micro-Agents Framework.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.nzdis.micro.messaging.network;

import org.nzdis.micro.messaging.message.Message;

/**
 * The NetworkConnectorInterface describes necessary functionality
 * to be supported by network transport mechanisms.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public interface NetworkConnectorInterface {

	public void setSerialization(String serialization);
	
	public void startNetwork();
	
	public void sendMessage(Message message, String host, int port);
	
	/**
	 * Indicates a prefix to augment eventual debug output with 
	 * meaningful source reference.
	 * @return
	 */
	public String getPrefix();
	
	/**
	 * Indicator if network is started.
	 * @return
	 */
	public boolean networkStarted();
	
	/**
	 * Returns the bound (server) port.
	 * @return
	 */
	public Integer getPort();
	
	/**
	 * Shuts down the network transport.
	 */
	public void shutdown();
	
}
