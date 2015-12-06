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
import org.nzdis.micro.gip.annotations.SequentialProcess;

@GenericIntent
public class SquareMinusCircleIntent implements Intent {

	@Input
	public Double radius = 5.0;
	
	@Input
	public Double squareLength = 4.0;
	
	@Result
	public Double result;
	
	double circleSurface;
	double squareSurface;
	
	@SequentialProcess(order = 0)
	public void calcCircleSurface(){
		circleSurface = Math.PI * Math.PI * radius;
	}
	
	@SequentialProcess(order = 0)
	public void calcSquareSurface(){
		squareSurface = squareLength * squareLength;
	}
	
	@SequentialProcess(order = 1)
	public void calcOverlappingSquareCorner(){
		result = 0.25 * (squareSurface - circleSurface);
	}
	
	@Override
	public String toString(){
		return "SquareMinusCircleIntent result: " + result;
	}
	
}
