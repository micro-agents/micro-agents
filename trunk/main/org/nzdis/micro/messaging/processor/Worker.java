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
import org.nzdis.micro.messaging.MTRuntime;

/**
 * Worker thread representation
 */
public class Worker extends Thread{
	
	boolean running = true;
	private BlockingQueue<AbstractMicroFiber> entityQueue = new LinkedBlockingQueue<AbstractMicroFiber>();
	
	public void run(){
		while (running){
			AbstractMicroFiber entity = getEntity();
			if(entity != null){
				entity.service(entity.getMessage());
			}
		}
	}
	
	/**
	 * Stops running worker.
	 */
	public synchronized void stopWorker(){
		running = false;
		this.interrupt();
	}

	/**
	 * Set a entity to the worker's queue
	 * @param process
	 *            process is the smallest part of any execution.
	 */
	public void setProcess(AbstractMicroFiber process){
		try {
			entityQueue.put(process);
		} catch (InterruptedException e){
			e.printStackTrace();
		}
	}

	/**
	 * Get an entity from the worker's queue.
	 * @return entity.
	 */
	public AbstractMicroFiber getEntity(){
		try{
			return entityQueue.take();
		} catch (InterruptedException e){
			if(entityQueue.isEmpty()){
				return null;
			} else {
				System.err.println(MTRuntime.getPlatformPrefix() + "Worker thread shutdown although message queue not empty.");
				return null;
			}
			//e.printStackTrace();
		}
	}
}
