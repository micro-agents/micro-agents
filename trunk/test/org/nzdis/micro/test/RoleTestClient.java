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
package org.nzdis.micro.test;

import java.util.ArrayList;
import org.nzdis.micro.DefaultSocialRole;
import org.nzdis.micro.MicroMessage;
import org.nzdis.micro.SystemAgentLoader;

public class RoleTestClient extends DefaultSocialRole {

	public String test1 = "TestData1";
	public String test2 = "TestData2";
	public boolean receivedData = false;
	
	@Override
	protected void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleMessage(MicroMessage message) {
		receivedData = true;
	}
	
	public void initRoleCast(){
		MicroMessage msg = new MicroMessage();
		msg.put(RoleTestAgent.resName, test1);
		sendRolecast(msg, new RoleTestAgent(), false);
	}
	
	public void initRandomCast(){
		MicroMessage msg = new MicroMessage();
		msg.put(RoleTestAgent.resName, test2);
		sendRandomcast(msg, 4, false, true, null);
	}
	
	public void initFuzzyCast(float quota, ArrayList<String> excludedAgents){
		MicroMessage msg = new MicroMessage();
		msg.put(RoleTestAgent.resName, test2);
		sendFuzzycast(msg, quota, false, false, excludedAgents);
	}

	public void initGroupCast(){
		MicroMessage msg = new MicroMessage();
		msg.put(RoleTestAgent.resName, test1);
		sendGroupCast(msg, SystemAgentLoader.findAgent("MotherAgent").getGroup());
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}
}
