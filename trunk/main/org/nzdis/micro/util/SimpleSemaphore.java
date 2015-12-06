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

public class SimpleSemaphore {

	private String name = "";
	private volatile boolean semaphoreAcquired = false;
	private StackTraceElement[] acquiringStack = null;
	private boolean printOutput = true;
	
	public SimpleSemaphore(){
	}
	
	/**
	 * Instantiates Semaphore object with object's class name as semaphore 
	 * name; wait output is activated. 
	 * @param name
	 */
	public SimpleSemaphore(String name){
		this(name, true);
	}
	
	/**
	 * Instantiates Semaphore object with a given String as semaphore 
	 * name and allows specifying if wait output is produced. 
	 * @param name
	 * @param printWaitOutput
	 */
	public SimpleSemaphore(String name, boolean printWaitOutput){
		this.name = name;
		printWaitInformationOutput(printWaitOutput);
	}
	
	/**
	 * Instantiates Semaphore object with object's class name as semaphore 
	 * name; wait output is activated.
	 * @param objectInstantiating
	 */
	public SimpleSemaphore(Object objectInstantiating){
		this(objectInstantiating, true);
	}
	
	/**
	 * Instantiates Semaphore object withclass name as semaphore 
	 * name; wait output is activated.
	 * @param instantiatedClass
	 */
	public SimpleSemaphore(Class instantiatedClass){
		this(instantiatedClass, true);
	}
	
	/**
	 * Instantiates Semaphore object with object's class name as semaphore 
	 * name and allows specifying if wait output is produced. 
	 * @param objectInstantiating
	 * @param printWaitOutput
	 */
	public SimpleSemaphore(Object objectInstantiating, boolean printWaitOutput){
		this(objectInstantiating.getClass().getSimpleName(), printWaitOutput);
	}
	
	/**
	 * Instantiates Semaphore object with class name as semaphore 
	 * name and allows specifying if wait output is produced. 
	 * @param instantiatedClass
	 * @param printWaitOutput
	 */
	public SimpleSemaphore(Class instantiatedClass, boolean printWaitOutput){
		this(instantiatedClass.getSimpleName(), printWaitOutput);
	}
	
	/**
	 * Sets the semaphore's name.
	 * @param name
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Returns the name of the semaphore.
	 * @return
	 */
	public String getName(){
		return this.name;
	}
	
	private int iteration = 0;
	
	/**
	 * Acquires semaphore or waits if semaphore is already acquired 
	 * by another thread.
	 */
	public synchronized void acquire(){
		while(semaphoreAcquired){
			try {
				if(printOutput){
					StringBuffer buf = new StringBuffer(Thread.currentThread().getName())
						.append(" (ID: ").append(Thread.currentThread().getId()).append(")")
						.append(": Awaiting semaphore");
					if(!name.equals("")){
						System.out.println(buf.append(" ").append(this.name));
					} else {
						System.out.println(buf.append("."));
					}
					iteration++;
					if(iteration >= 5){
						StackTraceElement[] elements = Thread.currentThread().getStackTrace();
						StringBuffer output = new StringBuffer("Semaphore ");
						if(!name.equals("")){
							output.append(this.name);
						}
						output.append(" Current StackTrace:");
						for(int i=0; i<elements.length; i++){
							output.append("\n").append(elements[i]);
						}
						if(acquiringStack != null){
							output.append("\n==========================");
							output.append("\nStackTrace when semaphore had been acquired ");
							output.append("(ID: ").append(Thread.currentThread().getId());
							output.append("): ");
							for(int i=0; i<acquiringStack.length; i++){
								output.append("\n").append(acquiringStack[i]);
							}
						}
						System.err.println(output);
						iteration = 0;
					}
				}
				Thread.sleep(5);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		semaphoreAcquired = true;
		acquiringStack = Thread.currentThread().getStackTrace();
	}
	
	/**
	 * Releases a previously acquired semaphore.
	 */
	public void release(){
		if(semaphoreAcquired){
			iteration = 0;
			semaphoreAcquired = false;
			acquiringStack = null;
		} else {
			StringBuffer output = new StringBuffer("Semaphore ");
			if(!name.equals("")){
				output.append(this.name);
			}
			System.err.println(output.append(" can only be released if previously acquired."));
		}
	}
	
	/**
	 * Allows to de/activate printing of semaphore wait output.
	 * @param print
	 */
	public void printWaitInformationOutput(boolean print){
		printOutput = print;
	}
	
	/**
	 * Checks if a semaphore can be acquired.
	 * @return
	 */
	public boolean isFree(){
		return !semaphoreAcquired;
	}

	@Override
	public String toString() {
		return "SimpleSemaphore [name=" + name + ", semaphoreAcquired="
				+ semaphoreAcquired + ", printOutput=" + printOutput + "]";
	}
	
}
