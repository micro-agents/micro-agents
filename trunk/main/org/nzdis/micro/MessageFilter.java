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

/**
 * The MessageFilter is a specialization of the social role provides the basis
 * to define own message filters in order to allow effective decomposition of
 * micro-agents by means of message filters.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a>
 * @version $Revision: 2.0 $ $Date: 2010/11/14 00:00:00 $
 *
 */
public abstract class MessageFilter extends DefaultSocialRole {

	public MessageFilter(MicroMessage pattern){
		this.pattern = pattern;
	}
	
	private MicroMessage pattern = null;
	private boolean passToNonMessageFilters = true;
	
	@Override
	protected void initialize() {
		printDebug(new StringBuffer("MessageFilter for pattern ").append(pattern)
				.append(" registered in group of owner ").append(getAgent().getOwner().getAgentName()));
		initializeMessageFilter();
	}
	
	@Override
	protected void release(){
		((AbstractAgent)getAgent()).semaphore.acquire();
		((AbstractAgent)getAgent()).getMessageFilterRoles().remove(this);
		if(((AbstractAgent)getAgent()).getMessageFilterRoles().isEmpty()){
			((AbstractAgent)getAgent()).messageFilterRolesRegistered = false;
		}
		((AbstractAgent)getAgent()).semaphore.release();
		releaseMessageFilter();
	}

	@Override
	public void handleMessage(MicroMessage message) {
		//message is encapsulated in container to avoid manipulation
		MicroMessage messageToCheck = (MicroMessage)message.getCustomField(MicroMessage.MSG_PARAM_MSG_TO_FILTER);
		if(messageToCheck != null){
			//received via super-agent (as registered message filter)
			//print("Received message to check: " + messageToCheck.toString());
			if(validateMessage(messageToCheck, pattern)){
				onMatchSuccess(messageToCheck);
			} else {
				onMatchFail(messageToCheck);
			}
		} else {
			//received by direct addressing (e.g. intent-based dynamic linking or direct addressing)
			handleDirectMessage(message);
		}
	}

	/**
	 * Sets MessageFilter pattern.
	 * @param message MicroMessage instance serving as pattern
	 */
	public void setPattern(MicroMessage message){
		pattern = message;
	}
	
	/**
	 * Returns MessageFilter pattern.
	 * @return MicroMessage registered as pattern
	 */
	public MicroMessage getPattern(){
		return pattern;
	}
	
	/**
	 * Defines if non-MessageFilter roles (the MessageFilter is a role) on an agent
	 * should receive the message independently from filter matching. If set to
	 * false the MessageFilter needs to handle eventual addressing of target roles.
	 * (Default: true) 
	 * @param allow Boolean indicating if the messages should be passed to other roles
	 */
	public void allowProcessingByNonMessageFilters(boolean allow){
		passToNonMessageFilters = allow;
	}
	
	/**
	 * Indicates if the messages caught by message filters are to be dispatched to other 
	 * (non-MessageFilter) roles on the platform.
	 * @return
	 */
	public boolean processingByNonMessageFiltersAllowed(){
		return passToNonMessageFilters;
	}

	/**
	 * is called upon pattern match in MessageFilter
	 * @param message Message received
	 */
	public abstract void onMatchSuccess(MicroMessage message);
	
	/**
	 * is called if pattern does not match in MessageFilter
	 * @param message Message received
	 */
	public abstract void onMatchFail(MicroMessage message);
	
	/**
	 * is called if messages are received via direct addressing or
	 * dynamic binding instead; not via MessageFilter functionality.
	 * Implementation should never be called if role only used as 
	 * message filter.
	 * @param message Message to be processed
	 */
	public abstract void handleDirectMessage(MicroMessage message);
	
	/**
	 * For customized implementation of pattern satisfaction of a received message.
	 * Default implementation would check if message fully equals pattern.
	 * @param message Message received by MessageFilter
	 * @param pattern Pattern to be checked against
	 * @return Boolean to indicate if message matched pattern.
	 */
	public abstract boolean validateMessage(MicroMessage messageToCheck, MicroMessage pattern);

	/**
	 * is called upon MessageFilter initialization. Developers can add further functionality
	 * using this method.
	 */
	protected abstract void initializeMessageFilter();
	
	/**
	 * is called upon MessageFilter release. Developers can add further release functionality
	 * using this method.
	 */
	protected abstract void releaseMessageFilter();
}
