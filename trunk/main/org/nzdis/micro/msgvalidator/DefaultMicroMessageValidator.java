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
package org.nzdis.micro.msgvalidator;

import org.nzdis.micro.MicroMessage;

/**
 * The DefaultMicroMessageValidator is the default implementation for message 
 * validation. Its checks the availability of a sender field along with either
 * recipient, intent or event. 
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public class DefaultMicroMessageValidator implements MicroMessageValidator {

	@Override
	public boolean validate(MicroMessage message) {
		boolean testRes = true;
		String errorMsg = "Error when validating message " + message.getMessageID() + " of conversation "
			+ message.getConversationID() + "!\nConsider field(s) ";
		String errorMsg2 = "";
		
		
		if(message.containsKey(MicroMessage.MSG_PARAM_SENDER)){
			if(message.get(MicroMessage.MSG_PARAM_SENDER) == null){
				testRes = false;
				errorMsg2 += "\n- 'sender' (Sender)";
			}
		} else {
			testRes = false;
			errorMsg2 += "\n- 'sender' (Sender)";
		}
		if(message.containsKey(MicroMessage.MSG_PARAM_RECIPIENT)){
			if(message.get(MicroMessage.MSG_PARAM_RECIPIENT) == null){
			testRes = false;
			errorMsg2 += "\n- 'recipient' (Recipient)";
			}
		} else {
			testRes = false;
			errorMsg2 += "\n- 'recipient' (Recipient)";
		}
		if(message.containsKey(MicroMessage.MSG_PARAM_PERFORMATIVE)){
			if(message.get(MicroMessage.MSG_PARAM_PERFORMATIVE) == null){
			testRes = false;
			errorMsg2 += "\n- 'performative' (Performative)";
			}
		} else {
			if(message.containsKey(MicroMessage.MSG_PARAM_EXEC_ENV)){
				if(message.get(MicroMessage.MSG_PARAM_EXEC_ENV).equals(MicroMessage.JAVA)){
					testRes = false;
					errorMsg2 += "\n- 'performative' (Performative)";
				}
				//else must be CLJ --> no performative necessary
			} else {
				//missing execEnv (execution Environment)
				testRes = false;
				errorMsg2 += "\n- 'execEnv' (Target execution environment)";
			}
		}
		if(message.containsKey(MicroMessage.MSG_PARAM_CONTENT)){
			if(message.get(MicroMessage.MSG_PARAM_CONTENT) == null){
			testRes = false;
			errorMsg2 += "\n- 'content' (Content)";
			}
		} else {
			testRes = false;
			errorMsg2 += "\n- 'content' (Content)";
		}
		if(message.containsKey(MicroMessage.MSG_PARAM_CONV_ID)){
			if(message.get(MicroMessage.MSG_PARAM_CONV_ID) == null){
			testRes = false;
			errorMsg2 += "\n- 'convID' (Conversation ID)";
			}
		} else {
			testRes = false;
			errorMsg2 += "\n- 'convID' (Conversation ID)";
		}
		if(message.containsKey(MicroMessage.MSG_PARAM_MSG_ID)){
			if(message.get(MicroMessage.MSG_PARAM_MSG_ID) == null){
			testRes = false;
			errorMsg2 += "\n- 'msgID' (Message ID)";
			}
		} else {
			testRes = false;
			errorMsg2 += "\n- 'msgID' (Message ID)";
		}
		if(testRes == false){
			System.err.println(errorMsg + errorMsg2 + " when reviewing." + this.toString());
			if(message.strictValidation == true){
				throw new RuntimeException("System execution stopped as of message error.");
			}
			return false;
		}
		return true;
	}

}
