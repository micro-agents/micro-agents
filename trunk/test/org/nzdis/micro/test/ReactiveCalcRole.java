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
package org.nzdis.micro.test;

import org.nzdis.micro.DefaultPassiveRole;

public class ReactiveCalcRole extends DefaultPassiveRole {

	@Override
	protected void initialize() {
		addApplicableIntent(AdderIntent.class);
		addApplicableIntent(SubstractionIntent.class);
	}

	public int add(int a, int b){
		return a+b;
	}
	
	public int substract(int a, int b){
		return a-b;
	}

	@Override
	protected void release() {
		// TODO Auto-generated method stub
		
	}
	
}
