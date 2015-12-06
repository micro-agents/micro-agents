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


import java.util.Iterator;
import java.util.UUID;
import org.nzdis.micro.gip.annotations.GenericIntent;
import org.nzdis.micro.messaging.message.Message;


/**
 * MicroMessage represents a wrapper around the hashmap used as data container for
 * the actual communication. Constants for fields and a basic performative set are included.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a>
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 */
public class MicroMessage extends Message{

	
	private static final long serialVersionUID = -2865790998817294561L;

	
	/** Constants useful for definition of execution environment (e.g. automatic redirection
	 * to Clojure based on value.
	 */
	public static final String JAVA = "JAVA";
	public static final String CLOJURE = "CLJ";
	
	/** Predefined Performatives for MicroMessages (but not exclusive) */
	//public enum Performative {REQUEST, REFUSE, COMMIT, ACHIEVED, FAILED, NOT_UNDERSTOOD, INFORM, SUBSCRIBE, UNSUBSCRIBE, CLEAR_SUBSCRIPTIONS};
	
	/** general purpose performatives for negotiations */
	public static final String REQUEST = "REQUEST";
	public static final String REFUSE = "REFUSE";
	public static final String COMMIT = "COMMIT";
	public static final String ACHIEVED = "ACHIEVED";
	public static final String FAILED = "FAILED";
	public static final String INFORM = "INFORM";
	
	/** default performative sent in case of not understood or invalid messages */
	public static final String NOT_UNDERSTOOD = "NOT_UNDERSTOOD";
	
	
	/** reserved message field keys */
	public static final String MSG_PARAM_SENDER = "MSG_SENDER";
	public static final String MSG_PARAM_RECIPIENT = "MSG_RECIPIENT";
	public static final String MSG_PARAM_PERFORMATIVE = "MSG_PERFORMATIVE";
	public static final String MSG_PARAM_EXEC_ENV = "MSG_EXEC_ENV";
	public static final String MSG_PARAM_CONTENT = "MSG_CONTENT";
	public static final String MSG_PARAM_CONV_ID = "MSG_CONV_ID";
	public static final String MSG_PARAM_MSG_ID = "MSG_ID";
	public static final String MSG_PARAM_INTENT = "MSG_INTENT";
	public static final String MSG_PARAM_EVENT = "MSG_EVENT";
	public static final String MSG_PARAM_MSG_TO_FILTER = "MESSAGE_TO_FILTER";
	
	
	/** indicates global validation of all messages on platform */
	public static boolean globalValidation = false;
	/** indicates validation for this message instance */
	public boolean validation = false;
	/** indicates validation strictness for this message instance (INFO or ERROR) */
	public boolean strictValidation = false;
	
	
	/** Deactivates the automatic setting of the sender field upon sending if set to true.
	 *  This can be useful in the context of message filters when forwarding a message 
	 *  without manipulating the content (i.e. sender information), although it can be
	 *  seen as a security risk; micro-agents need to be benevolent;-) */
	public boolean sendUnchanged = false;
	
	/**
	 * Instantiates a MicroMessage with a given intent.
	 */
	public MicroMessage(Intent intent){
		this.setIntent(intent);
	}
	
	/**
	 * Instantiates a MicroMessage with a given event.
	 */
	public MicroMessage(Event event){
		this.setEvent(event);
	}
	
	/**
	 * Default Constructor. Instantiates an empty MicroMessage.
	 */
	public MicroMessage(){
	}
	
	/**
	 * Initializes new MicroMessage with given performative
	 * @param performative
	 */
	public MicroMessage(String performative){
		this.setPerformative(performative);
	}
	
	/**
	 * Instantiates a MicroMessage from existing Message type and copies all fields.
	 * @param input
	 */
	public MicroMessage(Message input){
		this.putAll(input);
	}
	
	
	/**
	 * Sets the sender for the MicroMessage. Will be automatically determined by the
	 * message framework.
	 */
	public synchronized void setSender(String agent){
		this.put(MSG_PARAM_SENDER, agent);
	}
	
	/**
	 * Retrieves the sender of a this message.
	 * @return String name of sender
	 */
	public synchronized String getSender(){
		if(this.containsKey(MSG_PARAM_SENDER)){
			return this.get(MSG_PARAM_SENDER).toString();
		} else {
			return "";
		}
	}

	/**
	 * Sets recipient for a specified message.
	 * @param agent
	 */
	public synchronized void setRecipient(String agent){
		this.put(MSG_PARAM_RECIPIENT, agent);	
	}
	
	/**
	 * Returns recipient of message. Returns empty String if 
	 * no recipient contained.
	 * @return
	 */
	public synchronized String getRecipient(){
		if(this.containsKey(MSG_PARAM_RECIPIENT)){
			return this.get(MSG_PARAM_RECIPIENT).toString();
		} else {
			return "";
		}
	}
	
	/**
	 * Checks if message contains recipient.
	 * @return
	 */
	public boolean containsRecipient(){
		return this.containsKey(MSG_PARAM_RECIPIENT);
	}
	
	/**
	 * Sets performative for this message.
	 * @param performative
	 */
	public synchronized void setPerformative(String performative){
		this.put(MSG_PARAM_PERFORMATIVE, performative);
	}
	
	/**
	 * Returns performative contained in message. Returns empty String
	 * if no performative contained.
	 * @return
	 */
	public synchronized String getPerformative(){
		if(this.containsKey(MSG_PARAM_PERFORMATIVE)){
			return this.get(MSG_PARAM_PERFORMATIVE).toString();
		} else {
			return "";
		}
	}
	
	/**
	 * Checks for existence of performative in message.
	 * @return
	 */
	public boolean containsPerformative(){
		return this.containsKey(MSG_PARAM_PERFORMATIVE);
	}
	
	/**
	 * Checks for existence of specified performative in message.
	 * @param performative
	 * @return
	 */
	public boolean containsPerformative(String performative){
		try{
			return this.getPerformative().equals(performative);
		} catch (NullPointerException e){
			return false;
		}
	}
	
	public synchronized void setContent(String content){
		this.put(MSG_PARAM_CONTENT, content);
	}
	
	public synchronized String getContent(){
		if(this.containsKey(MSG_PARAM_CONTENT)){
			return this.get(MSG_PARAM_CONTENT).toString();
		} else {
			return "";
		}
	}
	
	public synchronized void setIntent(Intent intent){
		this.put(MSG_PARAM_INTENT, intent);
	}
	
	public synchronized <T> T getIntent(){
		if(this.containsKey(MSG_PARAM_INTENT)){
			return (T) this.get(MSG_PARAM_INTENT);
		} else {
			return null;
		}
	}
	

	public boolean containsIntent(){
		return this.containsKey(MSG_PARAM_INTENT);
	}
	
	
	/**
	 * Indicates if a message contains an Intent of a particular type.
	 * @param clazz - Class to be checked against.
	 * @return boolean - result
	 */
	public boolean containsIntentType(Class clazz){
		if(this.containsIntent()){
			if(this.getIntent().getClass().equals(clazz)){
				return true;
			}
			return false;
		}
		return false;
	}
	
	/**
	 * Indicates if this message contains a generic intent (to 
	 * be processed by GenericIntentProcessor.
	 * @return boolean - result
	 */
	public boolean containsGenericIntent(){
		if(this.containsIntent()){
			Intent intent = this.getIntent();
			if(intent.getClass().isAnnotationPresent(GenericIntent.class)){
				return true;
			}
			return false;
		}
		return false;
	}
	
	
	public synchronized void setEvent(Event event){
		this.put(MSG_PARAM_EVENT, event);
	}
	
	public synchronized <T> T getEvent(){
		if(this.containsKey(MSG_PARAM_EVENT)){ 
			return (T) this.get(MSG_PARAM_EVENT);
		} else {
			return (T) new NullEvent(SystemOwner.ownName);
		}
	}
	
	public boolean containsEvent(){
		return this.containsKey(MSG_PARAM_EVENT);
	}
	
	/**
	 * Indicates if a message contains an Event of a particular type.
	 * @param clazz - Class to be checked against.
	 * @return boolean result
	 */
	public boolean containsEventType(Class clazz){
		if(this.containsEvent()){
			if(this.getEvent().getClass().equals(clazz)){
				return true;
			} else {
				return false;
			}
		}
		return false;
	}
	
	private Object objectCheck(Object objectToTest){
		if(objectToTest.getClass().equals(Integer.class)){
			objectToTest = objectToTest.toString();
		}
		if(!objectToTest.getClass().equals(String.class)){
			//this.containsJavaObjects = true;
		}
		return objectToTest;
	}
	
	public synchronized Object getCustomField(String customField){
		if(this.containsKey(customField)){
			return this.get(customField);
		} else {
			return null;
		}
	}
	
	public synchronized void setCustomField(String customField, Object customValue){
		customValue = objectCheck(customValue);
		//here a check on reserved field names could be added
		this.put(customField, customValue);
	}
	
	public void removeCustomField(String customField){
		this.remove(customField);
	}
	
	public synchronized void setExecutionEnvironment(String val){
		this.put(MSG_PARAM_EXEC_ENV, val);
	}

	public synchronized String getExecutionEnvironment(){
		if(this.containsKey(MSG_PARAM_EXEC_ENV)){
			return this.get(MSG_PARAM_EXEC_ENV).toString();
		} else {
			return null;
		}
	}
	
	public void setConversationID(String convId){
		this.put(MSG_PARAM_CONV_ID, convId);
	}
	
	public void setMessageID(Integer msgId){
		this.put(MSG_PARAM_MSG_ID, msgId);
	}
	
	public String getConversationID(){
		if(this.containsKey(MSG_PARAM_CONV_ID)){
			return this.get(MSG_PARAM_CONV_ID).toString();
		} else {
			return null;
			//return "";
		}
	}
	
	public boolean containsConversationID(){
		return this.containsKey(MSG_PARAM_CONV_ID);
	}
	
	public Integer getMessageID(){
		if(this.containsKey(MSG_PARAM_MSG_ID)){
			return Integer.parseInt(this.get(MSG_PARAM_MSG_ID).toString());
		} else {
			return null;
		}
	}
	
	/**
	 * Alternative to put().
	 * 
	 * @param key
	 * @param value
	 */
	public void set(String key, Object value){
		this.put(key, value);
	}

	/**
	 * Creates a reply message to a previously sent message.
	 * This is especially useful if in distributed mode to 
	 * ensure the message is returned to the according sender 
	 * (overriding dynamic linking).
	 * Contents are copied and should be overridden if not wanted.
	 * @return MicroMessage - message to send
	 */
	public MicroMessage createReply(){
		MicroMessage reply = new MicroMessage(this);
		if(this.containsKey(MSG_PARAM_SENDER_NODE)){
			if(this.containsKey(MSG_PARAM_SENDER_PORT)){
				reply.setRecipient(this.getSender() + "@" + this.get(MSG_PARAM_SENDER_NODE) + ":" + this.get(MSG_PARAM_SENDER_PORT));
				reply.remove(MSG_PARAM_SENDER_PORT);
			} else {
				reply.setRecipient(this.getSender() + "@" + this.get(MSG_PARAM_SENDER_NODE));
			}
			reply.remove(MSG_PARAM_SENDER);
			reply.remove(MSG_PARAM_SENDER_NODE);
			reply.remove(MSG_PARAM_SENDER_NODE_LOG);
		} else {
			reply.setRecipient(this.getSender());
		}
		//Increment message id if conversation id + message id are present
		if(this.containsKey(MSG_PARAM_CONV_ID) && this.containsKey(MSG_PARAM_MSG_ID)){
			//copy conversation id
			reply.put(MSG_PARAM_CONV_ID, this.get(MSG_PARAM_CONV_ID));
			//increment message id
			int nextId = Integer.parseInt(this.get(MSG_PARAM_MSG_ID).toString());
			//System.err.println("Current msg ID in conv. "+ getConversationID() +": "+ nextId);
			reply.put(MSG_PARAM_MSG_ID, String.valueOf(nextId+1));
			//System.err.println("new Message ID: "+ this.getMessageID());
		} else if(this.containsKey(MSG_PARAM_CONV_ID)){
			//copy conversation id
			reply.put(MSG_PARAM_CONV_ID, this.get(MSG_PARAM_CONV_ID));
		}
		return reply;
	} 
	
	/**
	 * Initializes new conversation in this message object.
	 */
	public void initializeConversation(){
		this.put(MSG_PARAM_CONV_ID, generateConversationId());
		this.put(MSG_PARAM_MSG_ID, "0");
	}
	
	/**
	 * Starts a new conversation but copies all contents of an 
	 * existing message (conversation ID and msg ID are overwritten).
	 * @param MicroMessage - message to be copied
	 * @return new MicroMessage object
	 */
	public static MicroMessage startNewConversation(MicroMessage msg){
		MicroMessage reply = new MicroMessage(msg);
		reply.put(MSG_PARAM_CONV_ID, generateConversationId());
		reply.put(MSG_PARAM_MSG_ID, "0");
		reply.remove(MSG_PARAM_RECIPIENT);
		return reply;
	}
	
	/**
	 * Starts a new conversation and returns a new message object.
	 * @return
	 */
	public static MicroMessage startNewConversation(){
		MicroMessage reply = new MicroMessage();
		reply.put(MSG_PARAM_CONV_ID, generateConversationId());
		reply.put(MSG_PARAM_MSG_ID, "0");
		return reply;
	}
	
	/**
	 * Returns a UUID as a basis for a unique conversation id.
	 * @return unique id as String
	 */
	public static String generateConversationId(){
		return UUID.randomUUID().toString();
	}
	
	/*
	public synchronized void resetConversation(){
		this.put(MSG_PARAM_CONV_ID, System.nanoTime());
		this.put(MSG_PARAM_MSG_ID, "0");
	}*/
	
	public String toString(){
		StringBuffer messageAsString = new StringBuffer();
		messageAsString.append("\n------------------------------------");  
		messageAsString.append("\nMessage:\n--------"); 
		if(this.containsKey(MSG_PARAM_SENDER)){
			messageAsString.append("\nSender: " + this.getSender());
		}
		if(this.containsKey(MSG_PARAM_RECIPIENT)){
			messageAsString.append("\nRecipient: " + this.getRecipient());
		}
		if(this.containsKey(MSG_PARAM_PERFORMATIVE)){
			messageAsString.append("\nPerformative: " + this.getPerformative());
		}
		if(this.containsKey(MSG_PARAM_EXEC_ENV)){
			messageAsString.append("\nExecution environment: " + this.getExecutionEnvironment().toString());
		}
		if(this.containsKey(MSG_PARAM_CONTENT)){
			messageAsString.append("\nContent: " + this.getContent());
		}
		if(this.containsKey(MSG_PARAM_INTENT)){
			messageAsString.append("\nIntent: " + this.getIntent());
		}
		if(this.containsKey(MSG_PARAM_EVENT)){
			messageAsString.append("\nEvent: " + this.getEvent());
		}
		if(this.containsKey(MSG_PARAM_CONV_ID)){
			messageAsString.append("\nConversation ID: " + this.get(MSG_PARAM_CONV_ID));
		}
		if(this.containsKey(MSG_PARAM_MSG_ID)){
			messageAsString.append("\nMessage ID: " + this.getMessageID());
		}
		if(this.containsKey(MSG_PARAM_SENDER_NODE)){
			messageAsString.append("\nSender node: " + this.get(MSG_PARAM_SENDER_NODE));
		}
		if(this.containsKey(MSG_PARAM_SENDER_PORT)){
			messageAsString.append("\nSender port: " + this.get(MSG_PARAM_SENDER_PORT));
		}
		Iterator<String> it = this.keySet().iterator();
		String tempEntry;
		
		while(it.hasNext()){
			tempEntry = it.next();
			if(!MessageFields.getInstance().contains(tempEntry)){
				messageAsString.append("\nAdditional field '"+ tempEntry +"': "+ this.getCustomField(tempEntry));
			}
		}
		messageAsString.append("\n------------------------------------");
		return messageAsString.toString();
	}
	
}
