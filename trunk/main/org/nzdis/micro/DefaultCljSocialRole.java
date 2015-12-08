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


import clojure.lang.Var;

/**
 * The DefaultCljSocialRole is a specialization of the DefaultSocialRole and assumes
 * that message handling is done in Clojure. It expects a Clojure script in the application
 * or role script folder on instantiation.
 * This must include the function "receive-msg [message]" being able to handle incoming
 * MicroMessages. Given this, no Java code will need to written for agent implementation as
 * sending can be accomplished using the function (send-msg <MicroMessage instance>) in Clojure.
 * See commonAgentBase.clj for alternative message sending functions.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 *
 */
public abstract class DefaultCljSocialRole extends DefaultSocialRole {

	Var receive;
	String scriptName;
	
	public DefaultCljSocialRole(String scriptname){
		this.scriptName = scriptname;
	}
	
	@Override
	protected void initialize(){
		((AbstractAgent)getAgent()).getCljConnector().runRoleScript(scriptName);
		receive = ((AbstractAgent)getAgent()).getCljConnector().registerFunction("receive-msg");
		initializeClj();
	}

	/**
	 * Implements necessary commands for initialization of Clojure script managed
	 * by this role.
	 */
	public abstract void initializeClj();
	
	
	@Override
	protected void release(){
		releaseClj();
		((AbstractAgent)getAgent()).getCljConnector().execInClojure(
		new StringBuffer("(remove-ns '")
			.append(((AbstractAgent)getAgent()).getCljConnector().getAgentNamespace()).append(")"));
	}
	
	/**
	 * Implements necessary commands to clean up resources in Clojure by application 
	 * developer. After this call the namespace is deleted. Thus the application should
	 * only implement everything necessary to cleanly shut down his application logic.
	 */
	public abstract void releaseClj();
	
	@Override
	public synchronized void handleMessage(MicroMessage message) {
		try {
			receive.invoke(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

}
