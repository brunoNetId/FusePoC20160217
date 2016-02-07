/**
 * 
 */
package com.inmarsat.uc3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author developer
 *
 */
public class CamelContextXmlTest extends CamelSpringTestSupport
{
	
	//ESB URI
	String esb = "netty-http:http://localhost:10000/esb";
	

	private void startTestHarness() throws Exception
	{
		context.addRoutes(new RouteBuilder()
		{
			@Override
			public void configure() throws Exception
			{
				//This stub simulates SYSTEM 1
				from("netty-http:http://localhost:19999/submit")
					.log("System 1 got request...")
					.to("mock:stub-system-1")
					.to("direct:json-validator")
					.setBody().simple("ack");

				//This stub simulates SYSTEM 2
				from("netty-http:http://localhost:29999/submit")
					.log("System 2 got request...")
					.to("mock:stub-system-2")
					.to("direct:json-validator")
					.setBody().simple("ack");
				
				//helper route to validate JSON content
				from("direct:json-validator")
					.log("Validating JSON content...")
					.doTry()
						.unmarshal("xmljson")
						.to("mock:json-is-valid")
					.doCatch(net.sf.json.JSONException.class)
						.log("${exception.message}")
					.end();
			}
		});
	}

	//Helper method to generate XML events from devices
	private Exchange createDeviceEvent(String deviceId, String uuid)
	{
		//random generator
		Random random = new Random();
		
		//create event with random bandwidth
		return createDeviceEvent(deviceId, uuid, random.nextInt(10000));
	}
	
	//Helper method to generate XML events from devices
	private Exchange createDeviceEvent(String deviceId, String uuid, int bandwidth)
	{
		//create event
		DefaultExchange event = new DefaultExchange(context);
		
		//we inject the unique ID as a header
		event.getIn().setHeader("unique-id", uuid);
		
		//XML body
		String body =	"<event device-id=\""+deviceId+"\">\n"+
				 		"	<timestamp>"+System.currentTimeMillis()+"</timestamp>\n"+
				 		"	<status>on</status>\n"+
				 		"	<bandwidth>"+bandwidth+"</bandwidth>\n"+
				 		"</event>";
		
		//set payload
		event.getIn().setBody(body);
		
		return event;
	}
	
	@Test
	public void test01HarnessJsonValidator() throws Exception
	{
		//start harness
		startTestHarness();
		
		//we send a valid JSON payload
		template.sendBody("direct:json-validator", "{\"firstName\":\"John\", \"lastName\":\"Doe\"}");
		
		//and we send an invalid JSON payload
		template.sendBody("direct:json-validator", "corrupt JSON payload");
		
		//set expectations
        MockEndpoint mockJson = getMockEndpoint("mock:json-is-valid");
        mockJson.expectedMessageCount(1);
      
		//Validate our expectations
		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void test02CamelRouteDirectsEventToSystem1() throws Exception
	{
		//start harness
		startTestHarness();
		
		//we simulate the device sends an event with LOW bandwidth
		template.send(esb, createDeviceEvent("Device1", "1", 500));
		
		//set expectations
        MockEndpoint mock1 = getMockEndpoint("mock:stub-system-1");
        mock1.expectedMessageCount(1);
        
		//set expectations
        MockEndpoint mock2 = getMockEndpoint("mock:stub-system-2");
        mock2.expectedMessageCount(0);
		
		//set expectations
        MockEndpoint mockJson = getMockEndpoint("mock:json-is-valid");
        mockJson.expectedMessageCount(1);
        
		//Validate our expectations
		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void test03CamelRouteDirectsEventToSystem2() throws Exception
	{
		//start harness
		startTestHarness();
		
		//we simulate the device sends an event with HIGH bandwidth
		template.send(esb, createDeviceEvent("Device1", "1", 5000));
		
		//set expectations
        MockEndpoint mock1 = getMockEndpoint("mock:stub-system-1");
        mock1.expectedMessageCount(0);
		
		//set expectations
        MockEndpoint mock2 = getMockEndpoint("mock:stub-system-2");
        mock2.expectedMessageCount(1);
		
		//set expectations
        MockEndpoint mockJson = getMockEndpoint("mock:json-is-valid");
        mockJson.expectedMessageCount(1);
        
		//Validate our expectations
		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void test04CamelRouteWithDuplicates() throws Exception
	{
		//start harness
		startTestHarness();
		
		//iterations
		int numMessages = 10;
		
		//we send multiple messages and duplicates
		for(int i=0; i<numMessages ;i++)
		{
			//prepare message with unique ID
			Exchange original  = createDeviceEvent("Device1", Integer.toString(i));
			
			//prepare duplicate
			Exchange duplicate = original.copy();
			
			//send original and copy
			template.send(esb, original);
			template.send(esb, duplicate);
		}
		
		//set expectations
        MockEndpoint mockJson = getMockEndpoint("mock:json-is-valid");
        mockJson.expectedMessageCount(numMessages);
        
		//Validate our expectations
		assertMockEndpointsSatisfied();
	}
	
	@Override
	protected ClassPathXmlApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext(
				"META-INF/spring/camel-context.xml");
	}

}
