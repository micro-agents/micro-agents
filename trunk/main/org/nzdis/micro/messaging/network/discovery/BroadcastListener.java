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
package org.nzdis.micro.messaging.network.discovery;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;

/**
 * BroadcastListener is the listener implementation for the broadcast version of the
 * network discovery mechanism.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public class BroadcastListener extends DiscoveryListener {

	public BroadcastListener(int port){
		//this.address = "255.255.255.255";
		this.port = port;
		this.setName("BroadcastListener");
		readLocalAddresses();
		try {
			socket = new DatagramSocket(port);
			socket.setBroadcast(true);
			socket.setSoTimeout(socketTimeout);
			//System.out.println("Started socket on interface: " + socket.getInterface());
			//targetAddress = InetAddress.getByName(address);
			//socket.joinGroup(targetAddress);
			//System.out.println("Joined Multicast group " + address);
		} catch (IOException e) {
			if(e.getClass().equals(BindException.class)){
				System.err.println(DiscoveryService.discoveryServicePrefix + "BroadcastListener for port " + port + " could not be bound!\n"
							+ "If another platform instance is running on this host, consider to switch to MULTICAST discovery mode (or disable discovery).\n"
							+ "Discovery will not be activated!");
							//+ "Platform will be shut down.");
				//System.exit(0);
			} else {
				e.printStackTrace();
			}
		}
	}
	
}
