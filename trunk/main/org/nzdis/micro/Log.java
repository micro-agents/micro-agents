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

import org.nzdis.micro.messaging.MTRuntime;

/**
 * Log class for use by developer.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a>
 * @version $Revision: 2.0 $ $Date: 2010/16/24 00:00:00 $
 */

public class Log {

	public static final String DATETIME = "DATETIME";
	public static final String NANOTIME = "NANOTIME";
	private AbstractAgent owningAgent;
	private StringBuffer log = new StringBuffer();
	private static boolean logging = true;
	private static StringBuffer prefix = new StringBuffer(MTRuntime.LINE_DELIMITER).append("|Log for agent ");
	private static StringBuffer midfix = new StringBuffer(MTRuntime.LINE_DELIMITER).append("-------------------------");
	private String timeType = NANOTIME;
	
	
	public Log(AbstractAgent agent){
		this.owningAgent = agent;
	}
	
	public synchronized void append(String message){
		if(this.owningAgent.logActive == true && logging == true){
			this.log.append(MTRuntime.LINE_DELIMITER);
			this.log.append(owningAgent.getAgentName()).append(" - ");
			//this.log.append(System.nanoTime()).append(": ").append(message);
			if(timeType.equals(NANOTIME)){
				this.log.append(System.nanoTime());
			} else {
				if(timeType.equals(DATETIME)){
					this.log.append(MTConnector.getCurrentTimeString(true));
				}
			}
			this.log.append(": ").append(message);
		}
	}
	
	public String getLog(){
		return prefix.append(owningAgent.getAgentName()).append(": ").append(midfix).append(log).append(midfix).toString();
	}
	
	public void setTimeInfoType(String timeType){
		boolean set = false;
		if(timeType.equals(DATETIME)){
			this.timeType = DATETIME;
			set = true;
		}
		if(timeType.equals(NANOTIME)){
			this.timeType = NANOTIME;
			set = true;
		}
		if(!set){
			System.out.println(new StringBuffer(owningAgent.getAgentName())
				.append(" Log: Time type '").append(timeType)
				.append("' could not be identified.")
				.append(MTRuntime.LINE_DELIMITER).append("Current time type settings: ")
				.append(this.timeType));
		}
	}
	
	public void printLog(){
		System.out.println(getLog());
	}
	
}
