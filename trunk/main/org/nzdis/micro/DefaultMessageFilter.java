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

import java.util.Iterator;

/**
 * Default message filter implementation for micro-agent decomposition.
 * Filters on specified MicroMessage pattern by full String comparison.  
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a>
 * @version $Revision: 2.0 $ $Date: 2010/11/14 00:00:00 $
 */

public abstract class DefaultMessageFilter extends MessageFilter {


	public DefaultMessageFilter(MicroMessage pattern) {
		super(pattern);
	}

	@Override
	public boolean validateMessage(MicroMessage messageToCheck, MicroMessage pattern) {
		Iterator<String> it = pattern.keySet().iterator();
		boolean success = true;
		String key;
		while(it.hasNext()){
			key = it.next();
			if(messageToCheck.containsKey(key)){
				//check value similarity only if value is not null. (null is wildcard in message filter pattern.)
				if(pattern.get(key) != null){
					if(!messageToCheck.get(key).equals(pattern.get(key))){
						success = false;
						break;
					}
				}
			} else {
				success = false;
				break;
			}
		}
		return success;
	}

}
