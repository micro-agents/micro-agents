package org.nzdis.micro.constants;

public class AgentConsoleOutputLevels {

	public static final Integer NO_OUTPUT = 0;
	public static final Integer ERROR = 1;
	public static final Integer ALL = 2;
	
	public static String getOutputLevelAsString(Integer outputLevel){
		switch(outputLevel){
			case 0:
				return "NO_OUTPUT";
			case 1:
				return "ERROR";
			case 2:
				return "ALL";
		}
		return null;
	}
	
}
