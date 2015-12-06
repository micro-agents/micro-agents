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
package org.nzdis.micro.msgvalidator;

import org.nzdis.micro.MicroMessage;

/**
 * This interface is the building block for a MicroMessage validator. It can
 * do custom checks on message fields and can make use of the field 
 * MicroMessage.strictValidation, which if set to true suggest to stop execution 
 * (e.g. using a RuntimeException) upon validation error. If set to false, only an 
 * error should be thrown. See @link DefaultMicroMessageValidator as an example.
 * Important: The implementation of the validator itself is responsible for 
 * producing messages or throwing exceptions!
 *
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2012/03/12 00:00:00 $
 * 
 */
public interface MicroMessageValidator {

	/**
	 * indicates if a sent message is valid.
	 * @param message - Message to be validated
	 * @return validation result (true --> valid)
	 */
	public boolean validate(MicroMessage message); 
	
}
