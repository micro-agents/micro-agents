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
package org.nzdis.micro.constants;

/**
 * Describes constants describing the debugging levels.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2011/11/12 00:00:00 $
 * 
 */

public class PlatformOutputLevels {

	public static final Integer NO_OUTPUT = 0;
	public static final Integer INITIALIZATION_INFO = 1;
	public static final Integer WARNING = 2;
	public static final Integer DEBUG = 3;
	
	public static String getOutputLevelAsString(Integer outputLevel){
		switch(outputLevel){
			case 0:
				return "NO_OUTPUT";
			case 1:
				return "INITIALIZATION_INFO";
			case 2:
				return "WARNING";
			case 3:
				return "DEBUG";
		}
		return null;
	}
	
	/** TIME PREFIX-RELATED CONSTANTS */
	
	/** Indicates no time printing on console output */
	public static final String NO_TIME_PREFIX = "NO_TIME";
	/** Indicates time printing on console output */
	public static final String TIME_PREFIX = "TIME";
	/** Indicates date and time printing on console output */
	public static final String DATE_TIME_PREFIX = "DATE_TIME";
	/** Indicates nano time printing on console output */
	public static final String NANO_TIME_PREFIX = "NANO_TIME";
	
}
