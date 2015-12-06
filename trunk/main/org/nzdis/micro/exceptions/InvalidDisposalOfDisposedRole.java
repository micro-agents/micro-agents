package org.nzdis.micro.exceptions;

import org.nzdis.micro.Role;

public class InvalidDisposalOfDisposedRole extends RuntimeException {

public final String message;
	
	public InvalidDisposalOfDisposedRole(Role role){
		message = new StringBuffer("Tried to dispose an already disposed role instance (")
			.append(role.getRoleName()).append(").").toString();
	}
	
	@Override
	public String toString(){
		return message;
	}

}
