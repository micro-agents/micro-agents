package org.nzdis.micro.inspector.listeners;

import org.nzdis.micro.MTConnector;
import org.nzdis.micro.inspector.PlatformInspectorListener;

/**
 * Listener for PlatformInspectorGui that (if registered) is called when the user selects 
 * an agent in the Agent directory. This listener implementation activates the collection 
 * and redirection of all agent output (from print() and printError()) into the detail 
 * text area of that particular agent. It deactivates collection when the user navigates 
 * to another agent, the platform node, or if the PlatformInspectorGui is closed. 
 * 
 * @author cfrantz
 *
 */
public class ShowSelectedAgentsOutputInInspectorGuiListener implements PlatformInspectorListener{

	private String oldAgent = null;
	
	@Override
	public void agentSelected(String agentName) {
		//deactivate and reset on eventual old agent (previous selection)
		if(oldAgent != null){
			MTConnector.activateSelectivePrintingForAgent(oldAgent, false);
			MTConnector.getAgentForName(oldAgent).clearCollectedOutput();
		}
		if(agentName != null){
			//activate on current agent and save agent name
			MTConnector.activateSelectivePrintingForAgent(agentName, true);
			System.out.println("Activated output collection for agent " + agentName);
		}
		//update agent name in any case (may also be null, i.e. no agent selected)
		oldAgent = agentName;
		if(agentName == null){
			System.out.println("Deactivated all output collection.");
		}
	}

	
	
}
