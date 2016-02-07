package com.inmarsat.uc3;

import org.apache.camel.CamelContext;
import org.apache.camel.processor.ErrorHandler;

public class Debugger {

	public Debugger()
	{
		// TODO Auto-generated constructor stub
	}
	
	public void debug(CamelContext context)
	{
		System.out.println("context: "+ context.getName());
		
	}
}
