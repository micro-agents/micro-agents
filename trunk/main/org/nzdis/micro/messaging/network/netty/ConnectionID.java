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

import java.util.StringTokenizer;

/**
 * Helper class to generate unique connection IDs.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public class ConnectionID {

	public String remoteAddress = "";
	public String remoteServerPort = "";
	public String localAddress = "localhost";
	public String localServerPort = NettyNetworkConnector.getInstance().getPort().toString();
	private static String delimiter = "|";
	
	public ConnectionID(){
		
	}
	
	public ConnectionID(String remoteAddress, String remoteServerPort){
		this.remoteAddress = remoteAddress;
		this.remoteServerPort = remoteServerPort;
	}
	
	public String getId(){
		return new StringBuffer(remoteAddress).append(delimiter).append(remoteServerPort).append(delimiter).append(localAddress).append(delimiter).append(localServerPort).toString();
	}
	
	public String getFullRemoteNodeAddress(){
		return new StringBuffer(remoteAddress).append(":").append(remoteServerPort).toString();
	}
	
	public static ConnectionID inflate(String stringId){
		StringTokenizer tokenizer = new StringTokenizer(stringId, delimiter);
		ConnectionID connId = new ConnectionID();
		if(tokenizer.countTokens() != 4){
			System.err.println("ConnectionID Inflation: Wrong token count for input connection id " + stringId);
			return null;
		}
		connId.remoteAddress = tokenizer.nextToken();
		connId.remoteServerPort = tokenizer.nextToken();
		if(!connId.localAddress.equals(tokenizer.nextToken())){
			System.err.println("Inconsistent local Connection ID " + stringId);
		}
		if(!connId.localServerPort.equals(tokenizer.nextToken())){
			System.err.println("Inconsistent local Connection ID " + stringId);
		}
		return connId;
	}
	
	public String toString(){
		return getId();
	}
	
}
