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
package org.nzdis.micro;

import org.nzdis.micro.listeners.PlatformActionListener;


public class PlatformController {

	/**
	 * Starts platform and all related services 
	 * as configured with MicroBootProperties or
	 * configuration file (platform.xml).
	 */
	public static void startPlatform(){
		MTConnector.initializePlatform();
	}
	
	/**
	 * Shuts the platform down asynchronously and 
	 * notifies the listener (if not null) once completed.
	 * @param listener PlatformActionListener implementing necessary caller action. 
	 * If null, it just shuts down the platform (in the background) without notification.
	 */
	public static void shutdownPlatformInBackground(final PlatformActionListener listener){
		new Thread(new Runnable(){

			@Override
			public void run() {
				shutdownPlatform();
				if(listener != null){
					listener.actionCompleted();
				}
			}
			
		}).start();
	}
	
	/**
	 * Shuts down platform.
	 */
	public static void shutdownPlatform(){
		MTConnector.shutdown();
	}
	
	/**
	 * Indicates if the platform is running.
	 * @return true indicates running platform, false indicates non-running
	 */
	public static boolean platformRunning(){
		return MTConnector.platformInitialized();
	}
	
	/**
	 * This method shuts down the platform as well as the JVM instance.
	 * Use this with care as it will harden debugging.
	 * Alternative: {@link shutdownPlatform()}
	 */
	public static void shutdownPlatformAndJvm(){
		shutdownPlatform();
		System.exit(0);
	}
	
}
