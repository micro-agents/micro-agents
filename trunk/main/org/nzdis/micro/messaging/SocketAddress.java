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
package org.nzdis.micro.messaging;

import java.net.InetSocketAddress;
import java.util.StringTokenizer;

/**
 * SocketAddress serves as a mechanism to produces a consistent String-based 
 * socket address representation for internal platform purposes as well as 
 * its conversion to a Java SocketAddress.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 */
public class SocketAddress {

	private String name = "";
	private String hostAddress = "";
	private int port = 0;
	
	public SocketAddress(InetSocketAddress address){
		this.hostAddress = address.getAddress().getHostAddress();
		this.port = address.getPort();
		this.name = address.getHostName();
	}
	
	/**
	 * This constructor instantiates SocketAddress only with 
	 * host, but not port. Should not be generally used.
	 * @param host
	 */
	public SocketAddress(String host){
		this.hostAddress = host;
	}
	
	public SocketAddress(String host, int port){
		this.hostAddress = host;
		this.port = port;
	}
	
	public SocketAddress(String host, int port, String name){
		this.hostAddress = host;
		this.port = port;
		this.name = name;
	}
	
	public String getHostname(){
		return name;
	}
	
	public void setHostname(String hostname){
		this.name = hostname;
	}
	
	public void setHostAddress(String hostAddress){
		this.hostAddress = hostAddress;
	}
	
	public String getHostAddress(){
		return hostAddress;
	}
	
	public int getPort(){
		return port;
	}
	
	public void setPort(int port){
		this.port = port;
	}
	
	public String toString(){
		return new StringBuffer(hostAddress).append(":").append(port).toString();
	}
	
	public boolean equals(SocketAddress address){
		if(this.hostAddress.equals(address.getHostAddress()) && this.port == address.getPort()){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Converts a String-based representation in to a Java SocketAddress.
	 * @param input
	 * @return
	 */
	public static SocketAddress inflate(String input){
		StringTokenizer tok = new StringTokenizer(input, ":");
		SocketAddress sAddress = null;
		if(tok.countTokens() == 2){
			sAddress = new SocketAddress(tok.nextToken(), Integer.parseInt(tok.nextToken()));
		} else {
			System.err.println("Error when trying to deserialize address " + input);
		}
		return sAddress;
	}
}
