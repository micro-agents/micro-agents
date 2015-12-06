package org.nzdis.micro.inspector;

public interface PlatformInspectorListener {

	/**
	 * Is called when the user clicks on an agent name in the Platform Inspector.
	 * It is also called when the user clicks onto the Platform node 
	 * (passing null as agentName) to indicate that no agent is selected.
	 * @param agentName Selected agent's name
	 */
	void agentSelected(String agentName);
	
}
