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

public class CljClient extends DefaultSocialRole {

	private boolean resultArrived = false;
	private Object result = null;
	
	@Override
	protected void initialize() {
				
	}

	@Override
	public void handleMessage(MicroMessage message) {
		//print(message.toString());
		if(message.containsIntentType(CljExecutionIntent.class)){
			CljExecutionIntent cljIntent = message.getIntent();
			if(cljIntent.getResult() != null){
				this.result = cljIntent.getResult();
				this.resultArrived = true;
			}
		}
		/*
		//original intent handling
		if(message.getIntent().getClass().equals(CljExecutionIntent.class)){
			CljExecutionIntent cljIntent = (CljExecutionIntent)message.getIntent();
			if(cljIntent.getResult() != null){
				this.result = cljIntent.getResult();
				this.resultArrived = true;
			}
		}*/
	}
	
	public void start(CljExecutionIntent goal){
		MicroMessage msg = new MicroMessage();
		msg.setIntent(goal);
		send(msg);
		//print("Sent message " + msg.toString());
	}

	public Object getResult(){
		return result;
	}
	
	public boolean resultArrived(){
		return resultArrived;
	}
	
	public void reset(){
		resultArrived = false;
	}

	@Override
	protected void release() {
		// TODO Auto-generated method stub
		
	}
	
}
