package org.nzdis.micro.constants;

public abstract class RoleStates extends States {

	/**
	 * Role state indicating that role has been created, but not yet initialized.
	 */
	public static final int CREATED = 0;

	/**
	 * Role state indicating that role is played by owning agent.
	 */
	public static final int INITIALIZED = 1;
	
	/**
	 * Role state indicating that role has been disposed.
	 */
	public static final int DISPOSED = 2;
	
	/**
	 * Resolves state value to human-readable representation.
	 * @param state int value of role state
	 * @return String representation for state
	 */
	public static String getStateDescription(int state){
		switch(state){
			case CREATED:
				return "CREATED";
			case INITIALIZED:
				return "INITIALIZED";
			case DISPOSED: 
				return "DISPOSED";
		}
		return UNDEFINED_STATE;
	}
	
}
