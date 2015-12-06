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

import java.util.HashSet;
/**
 * All message fields for use with .toString functionality in MicroMessage.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a>
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 */

public class MessageFields extends HashSet<String>{

	private static MessageFields instance = null;
	
	public static MessageFields getInstance(){
		if(instance == null){
			instance = new MessageFields();
		}
		return instance;
	}
	
	private MessageFields(){
		this.add(MicroMessage.MSG_PARAM_SENDER);
		this.add(MicroMessage.MSG_PARAM_RECIPIENT);
		this.add(MicroMessage.MSG_PARAM_PERFORMATIVE);
		this.add(MicroMessage.MSG_PARAM_CONTENT);
		this.add(MicroMessage.MSG_PARAM_INTENT);
		this.add(MicroMessage.MSG_PARAM_EVENT);
		this.add(MicroMessage.MSG_PARAM_EXEC_ENV);
		this.add(MicroMessage.MSG_PARAM_CONV_ID);
		this.add(MicroMessage.MSG_PARAM_MSG_ID);
		this.add(MicroMessage.MSG_PARAM_SENDER_NODE);
		this.add(MicroMessage.MSG_PARAM_SENDER_PORT);
	}
	
	/** 
	 * Custom message fields holding original message values in case of error (used by platform) 
	 */
	
	/** Identifies message key that holds original recipient of a (probably undelivered) message as a value  ("PLATFORM" as sender). */
	public static final String ORIG_RECIPIENT = "ORIG_RECIPIENT";
	/** Identifies message key that holds original performative of a (probably undelivered) message as a value  ("PLATFORM" as sender). */
	public static final String ORIG_PERFORMATIVE = "ORIG_PERFORMATIVE";
	/** Identifies message key that holds original content of a (probably undelivered) message as a value  ("PLATFORM" as sender). */
	public static final String ORIG_CONTENT = "ORIG_CONTENT";
	/** Identifies message key that holds original sender of a (probably undelivered) message as a value  ("PLATFORM" as sender). */
	public static final String ORIG_SENDER = "ORIG_SENDER";
	
	/** 
	 * Performative constants for messages returned to sender in case of certain events. 
	 */
	
	/** performative used by platform ("PLATFORM" as sender) to inform sender on some process/issue (e.g. sending of message to remote node) */
	public static final String INFORM_MESSAGE = "INFORM";
	/** performative used by platform ("PLATFORM" as sender) for any messaging-related error (e.g. not-deliverable message) */
	public static final String ERROR_MESSAGE = "NOT_UNDERSTOOD";
	
}
