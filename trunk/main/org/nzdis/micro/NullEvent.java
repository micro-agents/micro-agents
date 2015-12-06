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

/**
 * Predefined NullEvent. Represents nil in event subscription mechanism.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a>
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 */

public class NullEvent extends Event {

	public NullEvent(String sender) {
		super(sender);
	}
	
	public String toString(){
		return "This event is returned if no event could be found!";
	}

}
