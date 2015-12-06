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

public class SocialEventClient extends DefaultSocialRole{

	private boolean eventCalled1 = false;
	private boolean eventCalled2 = false;
	private boolean subscribed1 = false;
	private boolean subscribed2 = false;
	private String sender;
	
	@Override
	protected void initialize() {
		subscribe1();
	}

	@Override
	public void handleMessage(MicroMessage message) {
		if(message.getEvent().getClass().equals(TestEvent1.class)){
			eventCalled1 = true;
			//print("Event1 called.");
			MicroMessage msg = new MicroMessage();
			msg.setRecipient(message.getSender());
			msg.setContent("RECEIVED1");
			sender = message.getSender();
			send(msg);
		}
		if(message.getEvent().getClass().equals(TestEvent2.class)){
			eventCalled2 = true;
			//print("Event2 called.");
			MicroMessage msg = new MicroMessage();
			msg.setRecipient(message.getSender());
			msg.setContent("RECEIVED2");
			sender = message.getSender();
			send(msg);
		}
	}
	
	public boolean eventCalled1(){
		return eventCalled1;
	}
	
	public boolean eventCalled2(){
		return eventCalled2;
	}
	
	public void resetCalls(){
		eventCalled1 = false;
		eventCalled2 = false;
	}
	
	public boolean getSubscriptionStatus1(){
		return subscribed1;
	}
	
	public boolean getSubscriptionStatus2(){
		return subscribed2;
	}
	
	public void subscribe1(){
		//getAgent().subscribe(new TestEvent1(getAgent().getAgentName()));
		subscribe(TestEvent1.class);
		subscribed1 = true;
	}
	
	public void subscribe2(){
		//getAgent().subscribe(new TestEvent2(getAgent().getAgentName()));
		subscribe(TestEvent2.class);
		subscribed2 = true;
	}
	
	public void unsubscribe1(){
		//getAgent().unsubscribe(new TestEvent1(getAgent().getAgentName()));
		unsubscribe(TestEvent1.class);
		subscribed1 = false;
	}
	
	public void unsubscribe2(){
		//getAgent().unsubscribe(new TestEvent2(getAgent().getAgentName()));
		unsubscribe(TestEvent2.class);
		subscribed2 = false;
	}
	
	public void resend(){
		MicroMessage msg = new MicroMessage();
		msg.setRecipient(sender);
		msg.setContent("RESEND");
		send(msg);
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}
	
}
