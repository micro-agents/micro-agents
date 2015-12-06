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
 * The Scheduler manages its queue and dispatches messages onto the according 
 * worker thread queue.
 */
public class Scheduler extends Thread {

	boolean running = true;
	private static BlockingQueue<AbstractMicroFiber> microFiberQueue = new LinkedBlockingQueue<AbstractMicroFiber>();
	
	public void run(){
		while (running){
			AbstractMicroFiber process = getProcess();
			if(process != null){
				Worker worker = MTRuntime.getNextWorker();
				worker.setProcess(process);
			}
		}
	}

	/**
	 * Stops a running scheduler.
	 */
	public synchronized void stopScheduler(){
		running = false;
		this.interrupt();
	}
	
	/**
	 * Adds a MicroFiber to the scheduler's queue.
	 * @param microFiber MicroFiber scheduled
	 */
	public static void setAgent(AbstractMicroFiber microFiber){
		try {
			microFiberQueue.put(microFiber);
		} catch (InterruptedException e) {
			e.printStackTrace();
			//e.getCause();
		}
	}

	/**
	 * Returns the first MicroFiber in the queue.
	 * @return
	 */
	public static AbstractMicroFiber getProcess(){
		try {
			return microFiberQueue.take();
		} catch (InterruptedException e) {
			//e.printStackTrace();
			return null;
		}
	}
}
