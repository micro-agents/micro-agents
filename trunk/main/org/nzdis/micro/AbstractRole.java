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
package org.nzdis.micro;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import org.nzdis.micro.constants.PlatformConstants;
import org.nzdis.micro.constants.RoleStates;
import org.nzdis.micro.exceptions.InvalidAccessOfDisposedRole;
import org.nzdis.micro.exceptions.InvalidDisposalOfDisposedRole;
import org.nzdis.micro.exceptions.InvalidDisposalOfNonInitializedRole;
import org.nzdis.micro.exceptions.InvalidInitializationOfDisposedRole;
import org.nzdis.micro.exceptions.RoleAlreadyInitializedException;
import org.nzdis.micro.exceptions.RoleNotInitializedException;
import org.nzdis.micro.inspector.annotations.Inspect;
import org.nzdis.micro.messaging.MTRuntime;


/**
 * Represents a base role. This class can be used as a base
 * implementation class for any role implementation. Because this role
 * does not really declare or implement any functionality, it can be
 * treat as an abstract role to specific role declaration
 * implementations.
 *
 * Created: Fri Mar 30 12:39:43 2001<br>
 *
 * @author <a href="mariusz@rakiura.org">Mariusz Nowostawski</a> (Original author)
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> (Revision author)
 * @version $Revision: 2.0 $ $Date: 2013/06/17 00:00:00 $
 */
public abstract class AbstractRole implements Role, Serializable {

	private HashSet applicableIntentTypes = null;

	private HashSet getApplicableIntentTypesSet(){
		if(this.applicableIntentTypes == null){
			this.applicableIntentTypes = new HashSet();
		}
		return this.applicableIntentTypes;
	}
  
	protected Agent owner;
	/**
	 * Holds former owner's name (e.g. if role is disposed).
	 * Just kept for debugging reasons in order to retrace agent's name.
	 */
	private String formerOwner = null;
	private int state = RoleStates.CREATED;
	@Inspect
	protected String roleName = "";
	/** indicates if messages passed to printDebug() will be printed. */
	protected boolean debugPrint = false;
  

	/** Public constructor. */
	public AbstractRole() {
		//fully qualified class name (without .class suffix)
		roleName = this.getClass().getName();
	}
	
	/**
	 * Instantiates role with given name.
	 * @param name
	 */
	public AbstractRole(String name){
		roleName = name;
	}

	/**
	 * Initializes the role in a context of a given agent.
	 */
	public void init(Agent owner){  
		if(this.state == RoleStates.INITIALIZED){
			if(owner.equals(this.owner)){
				return;
			}
			throw new RoleAlreadyInitializedException(this, this.owner, owner);
		}
		if(this.state == RoleStates.DISPOSED){
			throw new InvalidInitializationOfDisposedRole(this, owner);
		}
		this.owner = owner;
		//just assign to keep role history
		this.formerOwner = owner.getAgentName();
		this.state = RoleStates.INITIALIZED;
		//call after setting up agent for role implementation
		initialize();
	}
  
	/**
	 * Indicates if this role is initialized (played by an agent).
	 * @return
	 */
	public boolean isInitialized(){
		return this.state == RoleStates.INITIALIZED;
	}
	
	@Override
	public int getState(){
		return this.state;
	}
	
	/**
	 * Returns this role's former owner. If the role is 
	 * still active (i.e. INITIALIZED), it returns the 
	 * current owner.
	 * @return
	 */
	public String getFormerOwner(){
		return this.formerOwner;
	}

	/**
	 * Utility method for killing the owner agent. This method will kill the owner
	 * agent and dispose this and all other roles of this agent. If the owner agent is of
	 * type generated by KEA, this method will be equivalent to the call
	 * {@link AgentController#die() AgentController.die()}, and <code>true</code>
	 * value will be return. <code>false</code> is returned if the owner type
	 * is of different kind and the killing process was unsuccessfull.
	 * @return <code>true</code> if the killing process was successfull, 
	 * <code>false</code> otherwise.
     */
	protected boolean killOwnerAgent () {
		this.owner.die();
		return true;
	}

	/**
	 * Dispose all resources used by this role.
	 */
	public void dispose(){
		if(this.state == RoleStates.DISPOSED){
			throw new InvalidDisposalOfDisposedRole(this);
		}
		if(this.state != RoleStates.INITIALIZED){
			throw new InvalidDisposalOfNonInitializedRole(this, this.owner);
		}
		//checks if role is still running on agent --> then dispose from there
		if(owner.playsRole(this)){
			owner.disposeRole(this);
		} else {
			release();
			this.owner = null;
			this.state = RoleStates.DISPOSED;
		}
	}


	/**
	 * Returns the actual Agent handle for this role.
	 */
	public Agent getAgent(){
		if(this.state == RoleStates.INITIALIZED){
			return this.owner;
		} else {
			throw new RoleNotInitializedException(this);
		}
	}
  
	/**
	 * Returns reference to agent log.
	 * @return
	 */
	public Log getLog(){
		return ((AbstractAgent)getAgent()).getLog();
	}

	/**
	 * Indicates if role can satisfy particular intent type.
	 * @param intentType Intent type to be checked for applicability
	 * @return
	 */
	public boolean hasApplicableIntentType(Class<Intent> intentType){
		return getApplicableIntentTypesSet().contains(intentType);
	}
  
	/**
	 * Returns all the intent types applicable for this role.
	 */
	public Class[] getApplicableIntentTypes(){
		return (Class[])this.getApplicableIntentTypesSet().toArray(
    		new Class[getApplicableIntentTypesSet().size()]);
	}  
  
	/**
	 * Adds an applicable intent type to the role.
	 * @param intent Intent type
	 */
	public void addApplicableIntent(Class intent) {
		if(this.state == RoleStates.DISPOSED){
			throw new InvalidAccessOfDisposedRole(this);
		}
		this.getApplicableIntentTypesSet().add(intent);
		if(this.state == RoleStates.INITIALIZED){
			((AbstractAgent)this.owner).addApplicableIntentForRole(intent, this);
		}
	}
  
	/**
	 * Removes an applicable intent type from this role.
	 * @param intent Intent type to be removed.
	 */
	public void removeApplicableIntent(Class intent) {
		if(this.state == RoleStates.DISPOSED){
			throw new InvalidAccessOfDisposedRole(this);
		}
		this.getApplicableIntentTypesSet().remove(intent);
		if(this.state == RoleStates.INITIALIZED){
			((AbstractAgent)this.owner).removeApplicableIntentForRole(intent, this);
		}
	}

	/**
	 * Clears all applicable intents for the role.
	 */
	public void clearApplicableIntents() {
		if(this.state == RoleStates.DISPOSED){
			throw new InvalidAccessOfDisposedRole(this);
		}
		this.getApplicableIntentTypesSet().clear();
		if(this.state == RoleStates.INITIALIZED){
			((AbstractAgent)this.owner).clearApplicableIntentsForRole(this);
		}
	}

	/**
	 * This method is called when a role is initialized on an agent. Applicable goals
	 * should be registered using this method implementation.
	 */
	protected abstract void initialize();
  
  
	/**
	 * Releases all resources of this role (e.g. close files, threads, ....). 
	 * In consequence the role might be disposed or the system shutdown. 
	 * However, it should not perform the actual dispose().
	 */
	protected abstract void release();
  
	/**
	 * syncCheck indicates whether a thread safety check will be performed
	 * upon initialization of the role.
	 */
	private boolean syncCheck = false;

	protected void setSyncCheck(boolean check){
		syncCheck = check;
	}

	public boolean getSyncCheck(){
		return syncCheck;
	}

	/**
	 * This method is called by agents before role addition.
	 * Methods of reactive roles holding state (non-final fields)
	 * need to be declared synchronized.
	 * For social agents the handleMessage method needs to be syn-
	 * chronized (especially in the context of long-running message
	 * processing).
	 * The check can be overridden by setting syncCheck to 'false'
	 * (using setSyncCheck(false) in the role constructor).
	 */
	public void performThreadSafetyCheck(){
		if(syncCheck){
			if(this instanceof SocialRole){
				//only check on synchronized for handleMessage() method
				Method[] methodArray = this.getClass().getMethods();
				for(int u=0; u<methodArray.length; u++){
					if(methodArray[u].toString().lastIndexOf(PlatformConstants.SOCIAL_ROLE_MSG_METHOD_NAME) != -1){
						if(!Modifier.isSynchronized(methodArray[u].getModifiers())){
							StringBuffer errorString = new StringBuffer();
							errorString.append(MTRuntime.LINE_DELIMITER).append("Found unsynchronized method handleMessage in social role ");
							errorString.append(this).append(" (Class ").append(this.getClass());
							errorString.append("). ").append("Either ensure to synchronize this method or disable the synchronization check for the according role ");
							errorString.append(MTRuntime.LINE_DELIMITER).append(" (by using setSyncCheck(false) in the role's constructor).");
							throw new RuntimeException(errorString.toString());
						}
					}
				}
			} else {		  
				boolean foundUnsyncMethod = false;
				//per default: check if methods are unsynchronized when role is holding state.
			  
				boolean foundNonFinalField = false;
			  
				Field[] fieldArray = this.getClass().getDeclaredFields();
				for(int i=0; i<fieldArray.length; i++){
					if(!Modifier.isFinal(fieldArray[i].getModifiers())){
						foundNonFinalField = true;
					}
				}
				if(foundNonFinalField){
					Method[] methodArray = this.getClass().getDeclaredMethods();
					for(int u=0; u<methodArray.length; u++){
						if(!Modifier.isSynchronized(methodArray[u].getModifiers())){
							foundUnsyncMethod = true;
						}					  
					}
				}  

				if(foundUnsyncMethod){
					StringBuffer errorString = new StringBuffer();
					errorString.append(MTRuntime.LINE_DELIMITER).append("Found unsynchronized method(s) in role ");
					errorString.append(this).append(" (Class ").append(this.getClass());
					errorString.append(") holding (changeable) state. ").append(MTRuntime.LINE_DELIMITER).append("Either ensure to synchronize all enclosed methods, declare all fields as final or disable the synchronization check for the according role ");
					errorString.append(MTRuntime.LINE_DELIMITER).append(" (by using setSyncCheck(false) in the role's constructor).");
					throw new RuntimeException(errorString.toString());
				}
			}	
		}
	} 
  
	@Override
	public String toString(){
		if(this.state == RoleStates.INITIALIZED){
			return new StringBuffer("Role '").append(roleName).append("' on agent '")
				.append(getAgent().getAgentName()).append("'").toString();
		} else {
			return new StringBuffer("Role '").append(roleName).append("' - State ")
					.append(RoleStates.getStateDescription(this.state)).toString();
		}
	}
  
	@Override
	public String getRoleName(){
		return roleName;
	}
  
	/**
	 * Shortcut for {@link #collectAndPrint(StringBuffer, boolean)}. Only collects output but
	 * does not print the collected output.
	 * @param output
	 */
	public void collectAndPrint(StringBuffer output){
		collectAndPrint(output, false);
	}
	
	/**
	 * Shortcut for {@link #collectAndPrint(Object)}. 
	 * Only collects output but does not print the collected output.
	 * @param output
	 */
	public void collectAndPrint(Object output){
		collectAndPrint(new StringBuffer(output.toString()), false);
	}
  
	/**
	 * Collects and prints output on request. Allows buffer output 
	 * for prettier printing. Upon request, output is printed.
	 * @param output Output to be collected
	 * @param print Print output as well as collected messages immediately
	 */
	public void collectAndPrint(StringBuffer output, boolean print){
		collectAndPrint(output, print, false);
	}
	
	/**
	 * Collects and prints output on request (Object version). 
	 * Allows buffer output for prettier printing. Upon request, 
	 * output is printed.
	 * @param output Output to be collected
	 * @param print Print output as well as collected messages immediately
	 */
	public void collectAndPrint(Object output, boolean print){
		collectAndPrint(new StringBuffer(output.toString()), print, false);
	}
  
	/**
	 * Collects and prints output on request. Allows buffer output 
	 * for prettier printing. Upon request, output is printed
	 * and StringBuffer for collected messages reset. This version 
	 * of the method allows a reset of the StringBuffer without printing it.
	 * @param output Output to be added to message collection
	 * @param print Print collected messages
	 * @param reset Delete collected messages
	 */
	public void collectAndPrint(StringBuffer output, boolean print, boolean reset){
		try{
			((AbstractAgent)getAgent()).collectAndPrint(output, print, reset);
		} catch(RoleNotInitializedException e){
			printErrorMessageForPrintingToUninitializedRoles(output.toString(), "collectAndPrint()");
		}
	}
  
	/**
	 * Prints given output with agent name as prefix.
	 * Note: Use only if printing single statement. For combined String use {@link #print(String...)}.
	 * @param output Output to be printed
	 */
	public void print(String output){
		try{
			((AbstractAgent)getAgent()).print(output);
		} catch(RoleNotInitializedException e){
			printErrorMessageForPrintingToUninitializedRoles(output.toString(), "print()");
		}
	}
	
	/**
	 * Prints given output with agent name as prefix.
	 * @param output Output to be printed
	 */
	public void print(String... output){
		try{
			((AbstractAgent)getAgent()).print(output);
		} catch(RoleNotInitializedException e){
			printErrorMessageForPrintingToUninitializedRoles(output.toString(), "print()");
		}
	}
  
	/**
	 * StringBuffer version of {@link #print(String)}.
	 * @param output Output to be printed
	 */
	public void print(StringBuffer output){
		try{
			((AbstractAgent)getAgent()).print(output);
		} catch(RoleNotInitializedException e){
			printErrorMessageForPrintingToUninitializedRoles(output.toString(), "print()");
		}
	}
	
	/**
	 * StringBuffer version of {@link #print(String)}.
	 * Note: Use only if printing single statement. For combined String use {@link #print(StringBuffer...)}.
	 * @param output Output to be printed
	 */
	public void print(StringBuffer... output){
		try{
			((AbstractAgent)getAgent()).print(output);
		} catch(RoleNotInitializedException e){
			printErrorMessageForPrintingToUninitializedRoles(output.toString(), "print()");
		}
	}
	
	/**
	 * Object version of {@link #print(String)}.
	 * @param output Output to be printed
	 */
	public void print(Object output){
		try{
			((AbstractAgent)getAgent()).print(output.toString());
		} catch(RoleNotInitializedException e){
			printErrorMessageForPrintingToUninitializedRoles(output.toString(), "print()");
		}
	}

	/**
	 * Prints a given error message with agent name as prefix.
	 * Note: Use only if printing single statement. For combined String use {@link #printError(String...)}.
	 * @param output Error message to be printed
	 */
	public void printError(String output){
		try{
			((AbstractAgent)getAgent()).printError(output);
		} catch(RoleNotInitializedException e){
			printErrorMessageForPrintingToUninitializedRoles(output.toString(), "printError()");
		}
	}
	
	/**
	 * Prints a given error message with agent name as prefix.
	 * @param output Error message to be printed
	 */
	public void printError(String... output){
		try{
			((AbstractAgent)getAgent()).printError(output);
		} catch(RoleNotInitializedException e){
			printErrorMessageForPrintingToUninitializedRoles(output.toString(), "printError()");
		}
	}
	
	/**
	 * StringBuffer version of {@link printError()}.
	 * Note: Use only if printing single statement. For combined String use {@link #printError(StringBuffer...)}.
	 * @param output Error message to be printed
	 */
	public void printError(StringBuffer output){
		try{
			((AbstractAgent)getAgent()).printError(output);
		} catch(RoleNotInitializedException e){
			printErrorMessageForPrintingToUninitializedRoles(output.toString(), "printError()");
		}
	}
	
	/**
	 * StringBuffer version of {@link printError()}.
	 * @param output Error message to be printed
	 */
	public void printError(StringBuffer... output){
		try{
			((AbstractAgent)getAgent()).printError(output);
		} catch(RoleNotInitializedException e){
			printErrorMessageForPrintingToUninitializedRoles(output.toString(), "printError()");
		}
	}
	
	/**
	 * Object version of {@link printError()}.
	 * @param output Error message to be printed
	 */
	public void printError(Object output){
		try{
			((AbstractAgent)getAgent()).printError(output);
		} catch(RoleNotInitializedException e){
			printErrorMessageForPrintingToUninitializedRoles(output.toString(), "printError()");
		}
	}
  
	/**
	 * Prints debug output messages of an agent if debugPrint
	 * switch is activated in role.
	 * @param output Output to be printed
	 */
	public void printDebug(String output){
		if(debugPrint){
			try{
				((AbstractAgent)getAgent()).print(output);
			} catch(RoleNotInitializedException e){
				printErrorMessageForPrintingToUninitializedRoles(output.toString(), "printDebug()");
			}
		}
	}
	
	/**
	 * Prints debug output messages of an agent if debugPrint
	 * switch is activated in role. Varargs version for multiple lazily concatenated strings.
	 * @param output Output to be printed
	 */
	public void printDebug(String... output){
		if(debugPrint){
			try{
				((AbstractAgent)getAgent()).print(output);
			} catch(RoleNotInitializedException e){
				printErrorMessageForPrintingToUninitializedRoles(output.toString(), "printDebug()");
			}
		}
	}
  
	/**
	 * StringBuffer version of {@link printDebug()}.
	 * @param output Output to be printed
	 */
	public void printDebug(StringBuffer output){
		if(debugPrint){
			try{
				((AbstractAgent)getAgent()).print(output);
			} catch(RoleNotInitializedException e){
				printErrorMessageForPrintingToUninitializedRoles(output.toString(), "printDebug()");
			}
		}
	}
	
	/**
	 * StringBuffer varargs version of {@link printDebug()}.
	 * @param output Output to be printed
	 */
	public void printDebug(StringBuffer... output){
		if(debugPrint){
			try{
				((AbstractAgent)getAgent()).print(output);
			} catch(RoleNotInitializedException e){
				printErrorMessageForPrintingToUninitializedRoles(output.toString(), "printDebug()");
			}
		}
	}
	
	/**
	 * Object version of {@link printDebug()}.
	 * @param output Output to be printed.
	 */
	public void printDebug(Object output){
		if(debugPrint){
			try{
				((AbstractAgent)getAgent()).print(output.toString());
			} catch(RoleNotInitializedException e){
				printErrorMessageForPrintingToUninitializedRoles(output.toString(), "printDebug()");
			}
		}
	}
	
	/**
	 * Generates and prints an error message for invalid calls to print() methods 
	 * on an uninitialized role.
	 * @param message Message to be printed
	 * @param methodCall Method call called from simulation with message as parameter
	 */
	private void printErrorMessageForPrintingToUninitializedRoles(String message, String methodCall){
		StringBuilder builder = new StringBuilder();
		System.err.println(builder.append("Call to ").append(methodCall)
				.append(" on role ").append(roleName).append(" in state ") 
				.append(RoleStates.getStateDescription(getState()))
				.append(",").append(System.getProperty("line.separator"))
				.append("previously played by agent ").append(formerOwner)
				.append(": ").append(message).toString());
	}
	
	/**
	 * Returns this agent's name.
	 * @return Agent name
	 */
	public String me(){
		return getAgent().getAgentName();
	}

}
