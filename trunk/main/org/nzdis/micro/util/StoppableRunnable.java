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
package org.nzdis.micro.util;

/**
 * The StoppableRunnable can be used to implement a Runnable
 * which is stoppable by called stop() on it, rather than 
 * implementing conditional loops around it.
 * It provides a constructor for an optional generic parameter 
 * (for any application developer use (e.g. to have a reference
 * to the class instantiating the StoppableRunnable(). 
 * The passed parameter can be accessed from inside the 
 * StoppableRunnable using getReferenceObject().
 *  
 * @author cfrantz
 *
 * @param <T>
 */
public abstract class StoppableRunnable<T> implements Runnable {

	private volatile boolean running = false;
	private volatile boolean stopped = true;
	private volatile T referenceObject = null;
	private String threadName = "StoppableRunnable";
	
	public StoppableRunnable(){
		
	}
	
	/**
	 * Instantiates StoppableRunnable and assigns a specified name to 
	 * the thread it runs on.
	 * @param threadName
	 */
	public StoppableRunnable(String threadName){
		this.threadName = threadName;
	}
	
	/**
	 * Creates a StoppableRunnable with a generic parameter
	 * which can used from inside (via getReferenceObject()).
	 * @param reference reference object of any type (type specified via generics)
	 */
	public StoppableRunnable(T reference){
		this.referenceObject = reference;
	}
	
	/**
	 * Creates StoppableRunnable with a generic parameter
	 * which can used from inside (via getReferenceObject()), 
	 * as well as a name for the thread it will run on.
	 * @param reference reference object of any type (type specified via generics)
	 * @param threadName thread name
	 */
	public StoppableRunnable(T reference, String threadName){
		this(reference);
		this.threadName = threadName;
	}
	
	/**
	 * Returns the reference object for internal use.
	 * @return
	 */
	public T getReferenceObject(){
		return referenceObject;
	}
	
	/**
	 * Allows to set the reference at runtime.
	 * @param object reference object to be operated on
	 */
	public void setReferenceObject(T object){
		this.referenceObject = object;
	}

	/**
	 * This method stops the execution of the StoppableRunnable.
	 */
	public void stop(){
		running = false;
	}
	
	public final void run(){
		Thread.currentThread().setName(threadName);
		running = true;
		stopped = false;
		while(running){
			stoppableRun();
		}
		stopped = true;
	}
	
	public boolean isRunning(){
		return !stopped;
	}
	
	abstract public void stoppableRun();
	
}
