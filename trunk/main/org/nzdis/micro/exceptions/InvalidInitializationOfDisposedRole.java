package org.nzdis.micro.exceptions;

import org.nzdis.micro.Agent;
import org.nzdis.micro.Role;

public class InvalidInitializationOfDisposedRole extends RuntimeException {

	public final String message;
	
	public InvalidInitializationOfDisposedRole(Role role, Agent attemptingInitializer){
		message = new StringBuffer("Tried to initialize an already disposed role instance (")
			.append(role.getRoleName()).append(") - Initialization attempt by ")
			.append(attemptingInitializer.getAgentName()).toString();
	}
	
	@Override
	public String toString(){
		return message;
	}

}
