package org.nzdis.micro.listeners;

/**
 * Action Listener that can be passed along with Platform-related 
 * methods and is called to indicate the completing of actions. 
 * 
 * @author cfrantz
 *
 */
public interface PlatformActionListener {

	/**
	 * Method will be executed once called action is completed.
	 */
	public void actionCompleted();
	
}
