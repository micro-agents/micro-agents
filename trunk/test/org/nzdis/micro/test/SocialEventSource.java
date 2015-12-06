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
package org.nzdis.micro.test;

import org.nzdis.micro.DefaultSocialRole;
import org.nzdis.micro.MicroMessage;

public class SocialEventSource extends DefaultSocialRole{

	private int counter1 = 0;
	private int counter2 = 0;
	
	@Override
	protected void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleMessage(MicroMessage message) {
		if(message.getContent().equals("RECEIVED1")){
			counter1++;
		}
		if(message.getContent().equals("RECEIVED2")){
			counter2++;
		}
	}
	
	public void raiseEvent1(){
		counter1 = 0;
		MicroMessage msg = new MicroMessage();
		msg.setEvent(new TestEvent1(getAgent().getAgentName()));
		send(msg);
	}
	
	public void raiseEvent2(){
		counter2 = 0;
		MicroMessage msg = new MicroMessage();
		msg.setEvent(new TestEvent2(getAgent().getAgentName()));
		send(msg);
	}
	
	public int getCounter1(){
		return counter1;
	}
	
	public int getCounter2(){
		return counter2;
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}

}
