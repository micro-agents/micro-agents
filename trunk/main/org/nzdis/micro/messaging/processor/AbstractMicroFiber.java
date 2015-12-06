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
package org.nzdis.micro.messaging.processor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.nzdis.micro.messaging.MessageCommunicator;
import org.nzdis.micro.messaging.message.Message;


public abstract class AbstractMicroFiber{
	
	protected MessageCommunicator agent;
	private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>();
	
	public AbstractMicroFiber(MessageCommunicator agent){
		this.agent = agent;
	}
	
	public MessageCommunicator getAgent(){
		return agent;
	}
	
	/**
	 * The service(rawMessage) method is to be overridden by inheriting class.
	 * @param rawMessage
	 */
	public abstract void service(Message rawMessage);
	
	/**
	 * Retrieves the message from message queue
	 * @return top message from message queue (and removes it)
	 */
	public Message getMessage(){
		try {
			return messageQueue.take();
		} catch (InterruptedException e){
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Checks on message availability in the message queue.
	 * @return top message from message queue (but does not remove it)
	 */
	public Message peekMessage(){
		return messageQueue.peek();
	}

	/**
	 * Assigns the message to the message queue.
	 * @param message Message
	 */
	public void putMessage(Message message){
		try{
			if (message != null){
				messageQueue.put(message);
			}
		} catch (InterruptedException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Indicates that the MicroFiber has an empty message queue (all messages delivered).
	 * @return boolean indicating if message queue is empty
	 */
	public boolean hasEmptyMessageQueue(){
		return messageQueue.isEmpty();
	}
}
