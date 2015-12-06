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

import org.nzdis.micro.DefaultSocialRole;
import org.nzdis.micro.MicroMessage;
import org.nzdis.micro.SystemAgentLoader;

public class StorageCalcRole extends DefaultSocialRole {

	@Override
	protected void initialize() {
		addApplicableIntent(CalcIntent.class);		
	}

	@Override
	public void handleMessage(MicroMessage message) {
		if(message.getIntent().getClass().equals(CalcIntent.class)){
			CalcIntent cg = (CalcIntent) message.getIntent();
			if(cg.getOperation().equals("ADD")){
				Object objResult = ((ReactiveCalcRole)SystemAgentLoader.
						findRoles(ReactiveCalcRole.class)[0]).
						add(Integer.parseInt(cg.getLeftData().toString()), 
								Integer.parseInt(cg.getRightData().toString()));
				cg.setResult(objResult);
				MicroMessage msg = new MicroMessage();
				msg.setRecipient(message.getSender());
				msg.setIntent(cg);
				send(msg);
			}
			
		} else {
			print("Message " + message.toString() + " could not be handled.");
		}
		
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}

	
	
}
