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
 * Represents a system owner. This is a top-level agent which 
 * is used as an owner by the SystemAgentLoader, i.e. all agents
 * created by the SystemAgentLoader have this SystemOwner instance 
 * as the owner. 
 * 
 *<br><br>
 * SystemOwner.java<br>
 * Created: Thu Aug 16 15:42:34 2001<br>
 *
 * @author <a href="mariusz@rakiura.org">Mariusz Nowostawski</a> (Original author)
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> (Revision author)
 * @version $Revision: 2.0 $ $Date: 2013/04/08 00:00:00 $
 */
public class SystemOwner extends AbstractAgent {

	
	private SystemOwner() {
		agentName = ownName;
	}

	public static final String ownName = "SystemOwner";

    private static AbstractAgent instance = new SystemOwner();
	

    /**
    * Returns the instance of System Owner agent.
    */
    public static AbstractAgent getInstance () {
	   if(instance == null){
		  instance = new SystemOwner();
	   }
	  return instance; 
    }

    /**
    * Returns the group managed by this agent. 
    */
    /*
    @Override
    public Group getGroup () { 
    	if(systemGroup == null){
    		systemGroup = new DefaultGroup ( this ); 
    	}
    	return systemGroup; 
    }*/

    /**
     * Returns name of system owner agent.
     * @return
     */
    public static String getSystemOwnerName(){
    	return ownName;
    }

	@Override
	public void receive(MicroMessage message) {
		if(message.getSender().equals(MTConnector.platformProcess) && message.getPerformative().equals(MTRuntime.shutdownPerformative)){
			print("Shutting down Micro-Agent platform.");
			shutdownMicroAgentHierarchy();
		} else {
			printError("SystemOwner is not supposed to be addressed with messages.");
		}
	}
	
	private void shutdownMicroAgentHierarchy(){
		killSubAgentsOnDeath = true;
		die();
	}
}
