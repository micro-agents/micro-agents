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
package org.nzdis.micro.events;


import org.nzdis.micro.Event;

/**
 * Represents an AgentSuspended event. This event is risen by the agent to
 * all interested parties just before the agent switches to the SUSPENDED state. 
 * 
 *<br><br>
 * AgentSuspendedEvent.java<br>
 * Created: Wed Mar 28 17:05:04 2001<br>
 *
 * @author <a href="mariusz@rakiura.org">Mariusz Nowostawski</a> (original author)
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> (Revision author)
 * @version $Revision: 2.0 $ $Date: 2010/11/14 00:00:00 $
 */
public final class AgentSuspendedEvent extends Event {

  public AgentSuspendedEvent(String agent){
    super(agent);
  }

}
