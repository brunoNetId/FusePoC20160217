/**
 * 
 */
package com.inmarsat.uc1;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author developer
 *
 */
public class CamelContextXmlTest extends CamelSpringTestSupport {

	//endpoint to simulate SalesForce pushes new events
	@EndpointInject(uri = "activemq:topic:VirtualTopic.salesforce.newcustomer")
	protected Endpoint salesForcePublisher;
	
	
	private void startTestHarness() throws Exception
	{
		//Configures default MS Schema validation version
		System.setProperty("ms-stub-schema-version", "v1");
		
		context.addRoutes(new RouteBuilder()
		{
			@Override
			public void configure() throws Exception
			{
				
				//gets an XML sample v1 or v2
				from("direct:get-sample")
					.choice()
						.when().simple("${body} == 'v2'")
							.pollEnrich("file:src/data?noop=true&fileName=message1_v2.xml")
						.otherwise()
							.pollEnrich("file:src/data?noop=true&fileName=message1.xml")
					.end()
					.convertBodyTo(String.class);

				//helper to simulate a manual retry v1 or v2
				from("direct:simulate-manual-retry")
					.choice()
						.when().simple("${body} == 'v2'")
							.pollEnrich("file:src/data?noop=true&fileName=message1_v2.xml")
						.otherwise()
							.pollEnrich("file:src/data?noop=true&fileName=message1.xml")
					.end()
					.to("file:src/data/manual/ms/retry");
				
				//gets the node inside the XML
				from("direct:get-xml-node")
					.log("body to parse: ${body}")
					.setBody().xpath("string(//firstName)", String.class)
					.log("xpath result: ${body}");
			}
		});
	}
	
	@Test
	public void test01CamelRouteWhenMsCrmAndSapEccAreUp() throws Exception
	{	
		//start harness
		startTestHarness();
		
		//we simulate SalesForce publishes a 'new customer' event.
		String sample = (String)template.requestBody("direct:get-sample", "v1");
		template.sendBody(salesForcePublisher, sample);
		
		//set expectations
        MockEndpoint mockMs = getMockEndpoint("mock:ms-crm");
        mockMs.expectedMessageCount(1);
        
		//set expectations
        MockEndpoint mockSap = getMockEndpoint("mock:sap-ecc");
        mockSap.expectedMessageCount(1);
        
        //set expectations
        MockEndpoint mockMsError = getMockEndpoint("mock:ms-error");
        mockMsError.expectedMessageCount(0);
        
        //set expectations
        MockEndpoint mockSapError = getMockEndpoint("mock:sap-error");
        mockSapError.expectedMessageCount(0);
		
		//Validate our expectations
		assertMockEndpointsSatisfied();
	}

	@Test
	public void test02CamelRouteWhenMsIsDown() throws Exception
	{
		//we stop the MS Dynamics server
		context.stopRoute("stub-ms-dynamics");
		
		//start harness
		startTestHarness();
   
		//we simulate SalesForce publishes a 'new customer' event.
		String sample = (String)template.requestBody("direct:get-sample", "v1");
		template.sendBody(salesForcePublisher, sample);
		
		//set expectations
        MockEndpoint mockMs = getMockEndpoint("mock:ms-crm");
        mockMs.expectedMessageCount(0);
        
		//set expectations
        MockEndpoint mockSap = getMockEndpoint("mock:sap-ecc");
        mockSap.expectedMessageCount(1);
        
        //set expectations
        MockEndpoint mockMsError = getMockEndpoint("mock:ms-error");
        mockMsError.expectedMinimumMessageCount(1);
        
        //set expectations
        MockEndpoint mockSapError = getMockEndpoint("mock:sap-error");
        mockSapError.expectedMessageCount(0);
		
        //Thread.sleep(5000);
        
		//Validate our expectations
		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void test03CamelRouteWhenMsIsDownAndComesBackUp() throws Exception
	{
		//we stop the MS Dynamics server
		context.stopRoute("stub-ms-dynamics");
		
		//start harness
		startTestHarness();

		//we simulate SalesForce publishes a 'new customer' event.
		String sample = (String)template.requestBody("direct:get-sample", "v1");
		template.sendBody(salesForcePublisher, sample);
		
		//set expectations
        MockEndpoint mockMs = getMockEndpoint("mock:ms-crm");
        mockMs.expectedMessageCount(1);
        
		//set expectations
        MockEndpoint mockSap = getMockEndpoint("mock:sap-ecc");
        mockSap.expectedMessageCount(1);
        
        //set expectations
        MockEndpoint mockMsError = getMockEndpoint("mock:ms-error");
        mockMsError.expectedMinimumMessageCount(1);
        
        //set expectations
        MockEndpoint mockSapError = getMockEndpoint("mock:sap-error");
        mockSapError.expectedMessageCount(0);
		
        //We sleep for a bit to allow some failures to occur
        Thread.sleep(5000);
        
		//we now kick off MS Dynamics to simulate it comes back online
		context.startRoute("stub-ms-dynamics");
        
		//Validate our expectations
		assertMockEndpointsSatisfied();
	}
	
	
	@Test
	public void test04OrderIsMaintained() throws Exception
	{
		//start harness
		startTestHarness();
		
		//We create a send a series of SalesForce events
		List<String> bodies = new ArrayList<String>();
		for(int i=0; i<10 ; i++)
		{
			String newBodyV1 = "<person><firstName>customer-"+i+"</firstName><lastName>Smith</lastName><city>New York</city></person>";
			bodies.add(newBodyV1);
			
			template.sendBody(salesForcePublisher, newBodyV1);
		}
		
		MockEndpoint mockMs = getMockEndpoint("mock:ms-crm");
		mockMs.expectedMessageCount(bodies.size());
		
		//Validate our expectations
		assertMockEndpointsSatisfied();
		
		//We obtain the messages from the MS Stub
		List<Exchange> listExchanges = mockMs.getExchanges();
		
		//Check count of sent and received matches
		assertTrue(bodies.size() == listExchanges.size());
		
		//we check the order sent/received is maintained
		for(int i=0; i<bodies.size(); i++)
		{
			String sent = (String)template.requestBody("direct:get-xml-node", bodies.get(i));
			String got	= (String)template.requestBody("direct:get-xml-node", listExchanges.get(i).getIn().getBody(String.class));
			
			//System.out.println("sent/got: "+sent+"/"+got);
			assertEquals(sent, got);
		}
	}


	@Test
	public void test05CamelRouteWhenMsReturnsError() throws Exception
	{
		//start harness
		startTestHarness();
   
		//we simulate SalesForce publishes an event with a new field (version 2).
		String sample = (String)template.requestBody("direct:get-sample", "v2");
		template.sendBody(salesForcePublisher, sample);
		
		//set expectations
        MockEndpoint mockMs = getMockEndpoint("mock:ms-crm");
        mockMs.expectedMessageCount(1);
        
		//set expectations
        MockEndpoint mockSap = getMockEndpoint("mock:sap-ecc");
        mockSap.expectedMessageCount(1);
        
        //set expectations
        MockEndpoint mockMsError = getMockEndpoint("mock:ms-error");
        mockMsError.expectedMessageCount(1);
        
        //set expectations
        MockEndpoint mockSapError = getMockEndpoint("mock:sap-error");
        mockSapError.expectedMessageCount(0);
        
        //set expectations
        MockEndpoint mockAlertAdmin = getMockEndpoint("mock:alert-admin");
        mockAlertAdmin.expectedMessageCount(1);
		
        //Thread.sleep(5000);
        
		//Validate our expectations
		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void test06CamelRouteManualRetryMechanism() throws Exception
	{
		//start harness
		startTestHarness();
		
		//we simulate an Admin has manually triggered a retry action.
		template.sendBody("direct:simulate-manual-retry", "v1");
		
		//set expectations
        MockEndpoint mockMs = getMockEndpoint("mock:ms-crm");
        mockMs.expectedMessageCount(1);
        
		//set expectations
        MockEndpoint mockSap = getMockEndpoint("mock:sap-ecc");
        mockSap.expectedMessageCount(0);
        
        //set expectations
        MockEndpoint mockMsError = getMockEndpoint("mock:ms-error");
        mockMsError.expectedMessageCount(0);
        
        //set expectations
        MockEndpoint mockSapError = getMockEndpoint("mock:sap-error");
        mockSapError.expectedMessageCount(0);
        
        //We allow some time for the processing to finish
        Thread.sleep(2000);
		
		//Validate our expectations
		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void test07CamelRouteDeliveryFailsAndAdminRetriesMsMessage() throws Exception
	{
		//start harness
		startTestHarness();
		
		//we simulate SalesForce publishes an event with a new field (version 2).
		String sampleV2 = (String)template.requestBody("direct:get-sample", "v2");
		template.sendBody(salesForcePublisher, sampleV2);
		
		//set expectations
        MockEndpoint mockMs = getMockEndpoint("mock:ms-crm");
        mockMs.expectedMessageCount(1);
        
		//set expectations
        MockEndpoint mockSap = getMockEndpoint("mock:sap-ecc");
        mockSap.expectedMessageCount(1);
        
        //set expectations
        MockEndpoint mockMsError = getMockEndpoint("mock:ms-error");
        mockMsError.expectedMessageCount(1);
        
        //set expectations
        MockEndpoint mockSapError = getMockEndpoint("mock:sap-error");
        mockSapError.expectedMessageCount(0);
        
        //set expectations
        MockEndpoint mockAlertAdmin = getMockEndpoint("mock:alert-admin");
        mockAlertAdmin.expectedMessageCount(1);
        
		//Validate our expectations
		assertMockEndpointsSatisfied();
		
		//We reconfigure the MS Stub to validate against Schema v2
		//This simulates the Admin received an alert, reviewed the failed message
		//then added the new attribute in the CRM system which will now understand v2
		System.setProperty("ms-stub-schema-version", "v2");
		
		//we reset expectations
		mockMs.reset();
		mockMs.expectedMessageCount(1);
		
		//we reset expectations
		mockMsError.reset();
		mockMsError.expectedMessageCount(0);
		
		//we simulate the Admin manually retries the same message that previously failed
		template.sendBody("file:src/data/manual/ms/retry", sampleV2);
		
		//we wait a bit to allow processing to finish
		Thread.sleep(2000);
		
		//Validate our expectations
		assertMockEndpointsSatisfied();
	}
	
	@Override
	protected ClassPathXmlApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext(
				"META-INF/spring/camel-context.xml","META-INF/spring/local/broker.xml");
	}

}
