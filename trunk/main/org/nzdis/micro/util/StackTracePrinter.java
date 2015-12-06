package org.nzdis.micro.util;

import org.nzdis.micro.MTConnector;
import org.nzdis.micro.constants.OperatingSystems;

public class StackTracePrinter {

	/**
	 * Prints current stack trace but cuts off first three entries 
	 * (which mostly refer to this method itself).
	 */
	public static void printStackTrace(){
		System.out.println(getStackTrace());
	}
	
	/**
	 * Prints current stack trace but cuts off first number of lines as specified in parameter. 
	 * @param numberOfLinesToOmit Number of lines to be cut off
	 */
	public static void printStackTrace(int numberOfLinesToOmit){
		System.out.println(getStackTrace(numberOfLinesToOmit));
	}
	
	/**
	 * Returns the StackTrace at the point it is called but omits a 
	 * specified number of entries. Pass 0 (zero) to receive full trace.
	 * @param numberOfLinesToOmit Number of entries to be omitted
	 * @return String containing StackTrace
	 */
	public static String getStackTrace(int numberOfLinesToOmit){
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		StringBuffer output = new StringBuffer();
		output.append(" Current StackTrace:");
		int firstElement = numberOfLinesToOmit;
		for(int i = firstElement; i<elements.length; i++){
			output.append(System.getProperty("line.separator")).append(elements[i]);
		}
		return output.toString();
	}
	
	/**
	 * Returns the StackTrace as String but cutting off the first three 
	 * entries to derive the caller and the remaining trace.
	 * @return String containing StackTrace
	 */
	public static String getStackTrace(){
		int firstElement = 0;
		//cut beginning of stack trace depending on operating system
		if(MTConnector.getOperatingSystem().equals(OperatingSystems.WINDOWS)){
			firstElement = 3;
		}
		if(MTConnector.getOperatingSystem().equals(OperatingSystems.ANDROID)){
			firstElement = 3;
		}
		return getStackTrace(firstElement);
	}
	
	/**
	 * Prints the full stack trace on the position where this method
	 * is called.
	 */
	public static void printFullStackTrace(){
		System.out.println(getStackTrace(0));
	}
	
	/**
	 * Returns caller of method calling the code that calls this method. 
	 * Confusing, isn't it ;)
	 * @return
	 */
	public static StackTraceElement getCaller(){
		return Thread.currentThread().getStackTrace()[3];
	}
	
}
