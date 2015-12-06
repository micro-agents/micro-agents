package org.nzdis.micro.test;

import org.nzdis.micro.DefaultPassiveRole;

public class ReactiveCalcRole2 extends DefaultPassiveRole {

	@Override
	protected void initialize() {
		addApplicableIntent(AdderIntent.class);
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
