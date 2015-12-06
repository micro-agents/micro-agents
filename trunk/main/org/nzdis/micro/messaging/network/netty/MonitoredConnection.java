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

import java.util.concurrent.ConcurrentHashMap;
import org.jboss.netty.channel.ChannelFuture;
import org.nzdis.micro.MTConnector;
import org.nzdis.micro.MicroMessage;
import org.nzdis.micro.SystemOwner;
import org.nzdis.micro.events.RemotePlatformConnectedEvent;
import org.nzdis.micro.events.RemotePlatformDisconnectedEvent;
import org.nzdis.micro.messaging.MTRuntime;

/**
 * MonitoredConnection holds all Netty network connections considered as active. 
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public class MonitoredConnection extends ConcurrentHashMap<String, ChannelFuture>{

	/**
	 * key must be the id derived from ConnectionID.getId() in order to match (String var used
	 * in order to ensure that put() is overridden.
	 */
	@Override
	public ChannelFuture put(String key, ChannelFuture future){
		MTConnector.send(new MicroMessage(new RemotePlatformConnectedEvent(SystemOwner.ownName, future.getChannel().getRemoteAddress().toString())));
		//System.out.println(NettyNetworkConnector.getInstance().getPrefix() + "Sent connection event.");
		return super.put(key, future);
	}
	
	/**
	 * key must be ConnectionID in order to enforce proper deletion of propagated remote nodes
	 */
	@Override
	public ChannelFuture remove(Object key){
		if(!key.getClass().equals(ConnectionID.class)){
			//System.err.println("Connection deletion must be initialized with ConnectionID object!");
			throw new RuntimeException("Connection deletion must be initialized with ConnectionID object!");
		} else {
			ConnectionID id = (ConnectionID) key;
			MTConnector.send(new MicroMessage(new RemotePlatformDisconnectedEvent(SystemOwner.ownName, id.getFullRemoteNodeAddress())));
			MTRuntime.purgeRemoteNodeEntries(id.getFullRemoteNodeAddress());
			//System.out.println(NettyNetworkConnector.getInstance().getPrefix() + "Sent disconnection event for remote address key " + id.getFullRemoteNodeAddress());
			//System.out.println(NettyNetworkConnector.getInstance().getPrefix() + "Sent disconnection event for remote address " + super.get(id.getId()).getChannel().getRemoteAddress().toString());
			return super.remove(id.getId());
		}
	}
	
}
