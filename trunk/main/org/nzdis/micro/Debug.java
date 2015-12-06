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
package org.nzdis.micro;

/**
 * This class provides debug output facilities.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 */
public class Debug {

	//Debug levels --> 1 (only errors are printed), 2 (additional messages), 3 (development internals)
	static int debugLevel = 3;
	//Tell level --> 1 (basic prefixes to differentiate agent outputs
	static int tellLevel = 1;
	
	public static void processDebug(int level, String source, String message){
		StringBuffer outString = new StringBuffer("");
		boolean printed = false;
		if(level <= debugLevel){
			switch(level){
				case 1:
				outString.append("ERROR by");
				System.err.println(outString.append(" ").append(source).append(": ").append(message));
				printed = true;
				break;
				case 2:
				outString.append("INFO by");
				break;
				case 3:
				outString.append("DEBUG by");
				break;
				case 4:
				outString.append("DEBUG by");
				break;
			}
			if(!printed){
				System.out.println(outString.append(" ").append(source).append(": ").append(message));
			}
		}
	}
	
	public static void tell(int level, String source, String message){
		StringBuffer outString = new StringBuffer("");
		if(level <= tellLevel){
			switch(level){
				case 1:
				outString.append(source).append(" says ");
				System.err.print(outString.append(message).append("\n"));
				break;
			}
		}
	}	
}
