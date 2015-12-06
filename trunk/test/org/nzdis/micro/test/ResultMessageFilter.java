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

import org.nzdis.micro.DefaultMessageFilter;
import org.nzdis.micro.MicroMessage;

public class ResultMessageFilter extends DefaultMessageFilter {

	public boolean matched = false;
	
	public ResultMessageFilter(MicroMessage pattern) {
		super(pattern);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMatchSuccess(MicroMessage message) {
		print("Matched message.");
		matched = true;
		
	}

	@Override
	public void onMatchFail(MicroMessage message) {
		print("Did not match message.");
		matched = false;
	}

	@Override
	public void handleDirectMessage(MicroMessage message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void initializeMessageFilter() {
		allowProcessingByNonMessageFilters(false);
	}

	@Override
	protected void releaseMessageFilter() {
		// TODO Auto-generated method stub
		
	}

}
