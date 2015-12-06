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
package org.nzdis.micro.messaging.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import org.nzdis.micro.MTConnector;
import org.nzdis.micro.messaging.network.netty.NettyNetworkConnector;

/**
 * Raw Message class for further extension.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 *
 */
public class Message extends HashMap<String, Object> implements Serializable {

	private static final long serialVersionUID = -4450300730003772027L;

	/** network-related constants in messages */
	public static final String MSG_PARAM_SENDER_NODE = NettyNetworkConnector.SENDER_LOCAL_ADDRESS;
	public static final String MSG_PARAM_SENDER_PORT = "MSG_SENDER_PORT";
	public static final String MSG_PARAM_SENDER_NODE_LOG = "MSG_SENDER_NODE_LOG";
	
	public Message(){
		
	}
	
	public Message(HashMap input){
		this.putAll(input);
	}
	
	/**
	 * Returns the sending node of this message
	 * @return
	 */
	public String getSenderNode(){
		if(this.containsKey(MSG_PARAM_SENDER_NODE)){
			return this.get(MSG_PARAM_SENDER_NODE).toString();
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the tcp port of the sender node's network configuration.
	 * @return
	 */
	public Integer getSenderPort(){
		if(this.containsKey(MSG_PARAM_SENDER_PORT)){
			return Integer.parseInt(this.get(MSG_PARAM_SENDER_PORT).toString());
		} else {
			return null;
		}
	}
	
	/**
	 * Checks if this message has already been sent to another 
	 * node via this host (only relevant in distributed mode).
	 * This avoids message loops via connected platforms (e.g. events).
	 * @return
	 */
	public boolean messageAlreadyHandledbyLocalNode(){
		if(this.containsKey(MSG_PARAM_SENDER_NODE_LOG)){
			if(((HashSet<String>)this.get(MSG_PARAM_SENDER_NODE_LOG)).contains(MTConnector.getPlatformID())){
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Adds local node to list of nodes which have passed this message
	 * via network.
	 */
	public void addLocalNodeToSenderLog(){
		if(this.containsKey(MSG_PARAM_SENDER_NODE_LOG)){
			HashSet<String> messageHandledLog = ((HashSet<String>)this.get(MSG_PARAM_SENDER_NODE_LOG));
			messageHandledLog.add(MTConnector.getPlatformID());
			this.put(MSG_PARAM_SENDER_NODE_LOG, messageHandledLog);
		} else {
			HashSet<String> messageHandledLog = new HashSet<String>();
			messageHandledLog.add(MTConnector.getPlatformID());
			this.put(MSG_PARAM_SENDER_NODE_LOG, messageHandledLog);
		}
	}
	
	/** returns the sender log - all nodes the message has been passed over */
	public HashSet<String> getSenderLog(){
		return (HashSet<String>)this.get(MSG_PARAM_SENDER_NODE_LOG);
	}
	
}
