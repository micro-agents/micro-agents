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
package org.nzdis.micro.test.gip;

import org.nzdis.micro.Intent;
import org.nzdis.micro.gip.annotations.GenericIntent;
import org.nzdis.micro.gip.annotations.Input;
import org.nzdis.micro.gip.annotations.Result;
import org.nzdis.micro.gip.annotations.SingleProcess;

@GenericIntent
public class SimpleGenericIntent implements Intent{

	@Input
	public int input1 = 5;
	@Input
	public int input2 = 6;
	@Result
	public int result;
	
	@SingleProcess
	public void addition(){
		result = input1 + input2;
	}
	
	@Override
	public String toString(){
		return "Result: " + result;
	}
	
}
