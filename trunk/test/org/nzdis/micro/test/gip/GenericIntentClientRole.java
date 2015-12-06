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

import org.nzdis.micro.DefaultSocialRole;
import org.nzdis.micro.MicroMessage;

public class GenericIntentClientRole extends DefaultSocialRole {

	public int simpleResult = 0;
	public double complexResult = 0.0;
	public int inputAndResultResult = 0;
	
	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void handleMessage(MicroMessage message){
		if(message.containsGenericIntent()){
			if(message.containsIntentType(SimpleGenericIntent.class)){
				SimpleGenericIntent intent = message.getIntent();
				simpleResult = intent.result;
			}
			if(message.containsIntentType(SquareMinusCircleIntent.class)){
				SquareMinusCircleIntent intent = message.getIntent();
				complexResult = intent.result;
			}
			if(message.containsIntentType(SimpleInputResultIntent.class)){
				SimpleInputResultIntent intent = message.getIntent();
				inputAndResultResult = intent.inputAndResult;
			}
		}
	}

	@Override
	protected void initialize() {
		// TODO Auto-generated method stub
		
	}
	
	public void startSimpleIntent(){
		SimpleGenericIntent intent = new SimpleGenericIntent();
		intent.input1 = 5;
		intent.input2 = 6;
		send(intent);
	}
	
	public void startComplexIntent(){
		SquareMinusCircleIntent intent = new SquareMinusCircleIntent();
		intent.radius = 15.0;
		intent.squareLength = 30.0;
		send(intent);
	}
	
	public void startInputAndResultIntent(){
		SimpleInputResultIntent intent = new SimpleInputResultIntent();
		send(intent);
	}

}
