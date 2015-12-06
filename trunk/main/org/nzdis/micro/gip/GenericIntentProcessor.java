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
package org.nzdis.micro.gip;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.nzdis.micro.DefaultSocialRole;
import org.nzdis.micro.Intent;
import org.nzdis.micro.MTConnector;
import org.nzdis.micro.MessageFields;
import org.nzdis.micro.MicroMessage;
import org.nzdis.micro.gip.annotations.GenericIntent;
import org.nzdis.micro.gip.annotations.GenericIntentInterface;
import org.nzdis.micro.gip.annotations.Input;
import org.nzdis.micro.gip.annotations.InputAndResult;
import org.nzdis.micro.gip.annotations.Result;
import org.nzdis.micro.gip.annotations.SequentialProcess;
import org.nzdis.micro.gip.annotations.SingleProcess;
import org.nzdis.micro.messaging.MTRuntime;

/**
 * The GenericIntentProcessor allows the execution of generic intents (annotated self-
 * contained intents) by the platform without instantiating an additional agent.
 * 
 * @author Christopher Frantz
 *
 */
public class GenericIntentProcessor extends DefaultSocialRole {

	private static final String prefix = new StringBuilder(MTRuntime.GENERIC_INTENT_PROCESSOR_NAME).append(": ").toString();
	
	@Override
	protected void initialize() {
		addApplicableIntent(GenericIntentInterface.class);
	}
	
	@Override
	public synchronized void handleMessage(MicroMessage message){
		if(message.containsGenericIntent()){
			processGenericIntent(message, (Intent)message.getIntent());
		}
	}
	
	private void processGenericIntent(MicroMessage message, Intent intent){
		if(!validateGenericIntent(intent)){
			//send error message to sender
			MTConnector.notifySender(message, MessageFields.ERROR_MESSAGE, 8, true, true);
			return;
		}
		if(processingDebug){
			System.out.println(new StringBuffer(prefix).append("Processing GenericIntent ").append(intent.getClass().getSimpleName()).append(" ..."));
		}
		Method[] methods = intent.getClass().getMethods();
		ArrayList<Method>[] executionSequence = new ArrayList[methods.length];
		boolean executed = false;
		for(int x=0; x<methods.length; x++){
			//check on primitive GenericIntent (only containing a method annotated with @Process)
			if(methods[x].isAnnotationPresent(SingleProcess.class)){
				if(processingDebug){
					System.out.println(new StringBuffer(prefix).append("Executing primitive GenericIntent ").append(intent.getClass().getSimpleName()));
				}
				try {
					methods[x].invoke(intent, null);
					executed = true;
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
			if(!executed && methods[x].isAnnotationPresent(SequentialProcess.class)){
				int order = methods[x].getAnnotation(SequentialProcess.class).order();
				ArrayList<Method> internalList = null;
				if(executionSequence.length == 0 || executionSequence[order] == null){
					internalList = new ArrayList<Method>();
				} else {
					internalList = executionSequence[order];
				}
				internalList.add(methods[x]);
				executionSequence[order] = internalList;
			}
		}
		if(!executed){
			//execute methods in executionSequence and save results to intent
			for(int i=0; i<executionSequence.length; i++){
				ArrayList<Method> internalList = executionSequence[i];
				if(internalList != null){
					//check on parallel execution
					if(internalList.size() > 1){
						if(processingDebug){
							System.out.println(new StringBuffer(prefix).append("Executing parallel operation set of GenericIntent ").append(intent.getClass().getSimpleName()));
						}
						while(!internalList.isEmpty()){
							if(processingDebug){
								System.out.println(new StringBuffer(prefix).append("Executing partial parallel operation ").append(internalList.get(0).getName()).append(" of GenericIntent ").append(intent.getClass().getSimpleName()));
							}
							try {
								internalList.remove(0).invoke(intent, null);
								executed = true;
							} catch (IllegalArgumentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} else {
						if(!internalList.isEmpty()){
							//check on execution of single method
							if(processingDebug){
								System.out.println(new StringBuffer(prefix).append("Executing sequential operation ").append(internalList.get(0).getName()).append(" of GenericIntent ").append(intent.getClass().getSimpleName()));
							}
							try {
								internalList.remove(0).invoke(intent, null);
								executed = true;
							} catch (IllegalArgumentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}	
					}
				}
			}
		}
		if(!executed){
			MTConnector.notifySender(message, MessageFields.ERROR_MESSAGE, 9, true, true);
		} else {
			if(processingDebug){
				System.out.println(new StringBuffer(prefix).append("Processing of GenericIntent ").append(intent.getClass().getSimpleName()).append(" completed."));
			}
			//reply to message after processing
			MicroMessage reply = message.createReply();
			reply.setIntent(intent);
			send(reply);
		}
	}
	public static boolean processingDebug = true;
	public static boolean validationDebug = false;
	
	/**
	 * Static method to validate GenericIntents on all necessary
	 * annotations.
	 * @param intent - intent to be validated
	 * @return boolean - true if intent is valid, else false
	 */
	public static boolean validateGenericIntent(Intent intent){
		boolean classAnnotated = false;
		boolean inputField = false;
		boolean resultField = false;
		boolean processMethod = false;
		if(intent.getClass().isAnnotationPresent(GenericIntent.class)){
			classAnnotated = true;
		}
		//check fields
		Field[] fields = intent.getClass().getFields();
		for(int i=0; i<fields.length; i++){
			//fields[i].setAccessible(true);
			//relevant fields need to be accessible
			if(validationDebug){
				System.out.println("Checking field "+ fields[i]);
				System.out.println("Annotations: ");
				for(int m=0; m<fields[i].getAnnotations().length; m++){
					System.out.println("Annotation: " + fields[i].getAnnotations()[m].toString());
				}
			}
			if(fields[i].isAnnotationPresent(Input.class)){
				if(validationDebug){
					System.out.println("Input field present");
				}
				inputField = true;
			}
			if(fields[i].isAnnotationPresent(InputAndResult.class)){
				if(validationDebug){
					System.out.println("InputAndResult field present");
				}
				inputField = true;
				resultField = true;
			}
			if(fields[i].isAnnotationPresent(Result.class)){
				if(validationDebug){
					System.out.println("Result field present");
				}
				resultField = true;
			}
		}
		//check methods
		Method[] methods = intent.getClass().getMethods();
		for(int l=0; l<methods.length; l++){
			//methods need to be accessible
			if(methods[l].isAnnotationPresent(SingleProcess.class)){
				if(validationDebug){
					System.out.println("Process method present");
				}
				processMethod = true;
			}
			if(methods[l].isAnnotationPresent(SequentialProcess.class)){
				if(validationDebug){
					System.out.println("Sequence method present");
				}
				processMethod = true;
			}
		}
		if(classAnnotated && inputField && resultField && processMethod){
			
			return true;
		} else {
			StringBuffer error = new StringBuffer("GenericIntentProcessor: Missing requirement for valid GenericIntent: ");
			boolean first = true;
			if(!classAnnotated){
				error.append("Class not annotated (or is not publicly accessible)");
				first = false;
			}
			if(!inputField){
				if(!first){
					error.append(", ");
				}
				error.append("Input field not annotated (or is not publicly accessible)");
			}
			if(!resultField){
				if(!first){
					error.append(", ");
				}
				error.append("Result field not annotated (or is not publicly accessible)");
			}
			if(!processMethod){
				if(!first){
					error.append(", ");
				}
				error.append("Method for processing of intent not annotated (or is not publicly accessible)");
			}
			error.append("!");
			System.err.println(error);
			return false;
		}
		
	}

	@Override
	protected void release() {
		// TODO Auto-generated method stub
		
	}
	
}
