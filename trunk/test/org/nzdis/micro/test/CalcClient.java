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

public class CalcClient extends DefaultSocialRole {

	private boolean resultReceived = false;
	private Object result = null;
	
	@Override
	protected void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleMessage(MicroMessage message) {
		if(message.containsIntentType(CalcIntent.class)){
			CalcIntent intent = message.getIntent();
			result = intent.getResult();
			resultReceived = true;
		}
		/*
		if(message.getIntent().getClass().equals(CalcIntent.class)){
			result = ((CalcIntent)message.getIntent()).getResult();
			resultReceived = true;
		}*/
	}
	
	public boolean resultsReceived(){
		return resultReceived;
	}
	
	public Object getResults(){
		return result;
	}
	
	public void start(CalcIntent goal){
		MicroMessage msg = new MicroMessage();
		msg.setIntent(goal);
		send(msg);
	}

	@Override
	protected void release() {
		// TODO Auto-generated method stub
		
	}

}
