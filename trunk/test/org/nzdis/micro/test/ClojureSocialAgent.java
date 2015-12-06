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

import org.nzdis.micro.AbstractAgent;
import org.nzdis.micro.DefaultSocialRole;
import org.nzdis.micro.MicroMessage;

import clojure.lang.Var;

public class ClojureSocialAgent extends DefaultSocialRole {

	Var receive;
	
	@Override
	protected void initialize() {
		addApplicableIntent(CljExecutionIntent.class);
		((AbstractAgent)getAgent()).getCljConnector().runRoleScript("ClojureSocialAgent.clj");
		receive = ((AbstractAgent)getAgent()).getCljConnector().registerFunction("receive-msg");
	}

	@Override
	public void handleMessage(MicroMessage message) {
		//print("Received message: " + message.toString());
		try {
			receive.invoke(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void release() {
		// TODO Auto-generated method stub
		
	}

}
