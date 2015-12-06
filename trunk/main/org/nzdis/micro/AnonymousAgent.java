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
 * The AnonymousAgent is the simplest agent implementation and passes received
 * messages directly to its roles handleMessage() method (via AbstractAgent).
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a>
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 */
public class AnonymousAgent extends AbstractAgent{

	
	public AnonymousAgent() {
		super("", "");
	}
	
	public AnonymousAgent(String name){
		super(name, "");
	}
	
	public AnonymousAgent(String name, String cljScript){
		super(name, cljScript);
	}
	
	public AnonymousAgent(Agent owner){
		super("", false, "", owner);
	}
	
	public AnonymousAgent(Agent owner, String name, boolean useNameAsPrefix){
		super(name, useNameAsPrefix, "", owner);
	}
	
	public AnonymousAgent(Agent owner, String name, boolean useNameAsPrefix, String cljScript){
		super(name, useNameAsPrefix, cljScript, owner);
	}
	
	public AnonymousAgent(Agent owner, String name, String cljScript){
		super(name, false, cljScript, owner);
	}
	

}
