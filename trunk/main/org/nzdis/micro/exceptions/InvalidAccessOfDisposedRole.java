package org.nzdis.micro.exceptions;

import org.nzdis.micro.Role;

public class InvalidAccessOfDisposedRole extends RuntimeException {

	public final String message;
	
	public InvalidAccessOfDisposedRole(Role role){
		message = new StringBuffer("Invalid access of an already disposed role instance (")
			.append(role.getRoleName()).append(")").toString();
	}
	
	@Override
	public String toString(){
		return message;
	}

}
