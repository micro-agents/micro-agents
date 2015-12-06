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

import java.io.Serializable;

/**
 * Represents an abstract Event.
 * 
 *<br><br>
 * Event.java<br>
 * Created: Wed Mar 28 14:27:04 2001<br>
 *
 * @author <a href="mariusz@rakiura.org">Mariusz Nowostawski</a> (Original author)
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> (Revision author)
 * @version $Revision: 2.0 $ $Date: 2010/11/14 00:00:00 $
 */
public abstract class Event implements Serializable {  

	private static final long serialVersionUID = -8425938150133347805L;

	protected String source;
  
	/** indicate if event is raised on remote nodes as well */
	public boolean raiseRemoteEvent = false;
  
	public Event(){
		
	}
	
	/** 
	 * Creates a new event with the given agent as a source. 
	 *@param agent agent 
	 */
	public Event(String sendingAgentName){
		this.source = sendingAgentName;
	}

	/**
	 * Returns the agent who originated that event.
	 * @return this agent
	 */
	public String getSendingAgentName(){
		return source;
	}
}
