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

	
	@EndpointInject(uri = "activemq:topic:VirtualTopic.salesforce.newcustomer")
	protected Endpoint salesForcePublisher;

/*	@EndpointInject(uri = "netty-http:http://localhost:19999/ms-dyn")
	protected Endpoint microsoftDynamicsStub;
*/	
	
	private void startStubMsCrm() throws Exception
	{
		context.addRoutes(new RouteBuilder()
		{
			@Override
			public void configure() throws Exception
			{
				from("netty-http:http://localhost:19999/ms-dyn")
					.to("direct:stub-recorder-ms");
			}
		});
	}
	
	private void startStubSapEcc() throws Exception
	{
		context.addRoutes(new RouteBuilder()
		{
			@Override
			public void configure() throws Exception
			{
				from("netty-http:http://localhost:29999/sap-ecc")
					.to("direct:stub-recorder-sap");
			}
		});
	}
	
	private void startTestHarness() throws Exception
	{
		context.addRoutes(new RouteBuilder()
		{
			@Override
			public void configure() throws Exception
			{			
				from("direct:stub-recorder-ms")
					.to("mock:ms-crm");
				
				from("direct:stub-recorder-sap")
					.to("mock:sap-ecc");
				
				from("direct:get-sample")
					.pollEnrich("file:src/data?noop=true&fileName=${body}")
					.convertBodyTo(String.class);

				from("direct:simulate-manual-fix")
					.pollEnrich("file:src/data?noop=true&fileName=${body}")
					.to("file:src/data/manual/ms/fixed");
				
				from("direct:get-xml-node")
					.setBody().xpath("string(//firstName)", String.class);
			}
		});
	}
	
	@Test
	public void test01CamelRouteWhenMsCrmAndSapEccAreUp() throws Exception
	{
		//start stub Microsoft Dynamics
		startStubMsCrm();
		
		//start stub SAP ECC
		startStubSapEcc();
		
		//start harness
		startTestHarness();
		
		//we simulate SalesForce publishes a 'new customer' event.
		String sample = (String)template.requestBody("direct:get-sample", "message1.xml");
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
		
		// Validate our expectations
		assertMockEndpointsSatisfied();
	}

	@Test
	public void test02CamelRouteWhenMsIsDown() throws Exception
	{
		//we simulate there is no MS Dynamics server running
		//startStubMsCrm();
		
		//start stub SAP ECC
		startStubSapEcc();
		
		//start harness
		startTestHarness();
   
		//we simulate SalesForce publishes a 'new customer' event.
		String sample = (String)template.requestBody("direct:get-sample", "message1.xml");
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
        
		// Validate our expectations
		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void test03CamelRouteWhenMsIsDownAndComesBackUp() throws Exception
	{	
		//we simulate there is no MS Dynamics server running
		//startStubMsCrm();
		
		//start stub SAP ECC
		startStubSapEcc();
		
		//start harness
		startTestHarness();

		//we simulate SalesForce publishes a 'new customer' event.
		String sample = (String)template.requestBody("direct:get-sample", "message1.xml");
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
		startStubMsCrm();
        
		// Validate our expectations
		assertMockEndpointsSatisfied();
	}
	
	
	@Test
	public void test04OrderIsMaintained() throws Exception
	{
		//start stub Microsoft Dynamics
		startStubMsCrm();
		
		//start stub SAP ECC
		startStubSapEcc();

		//start harness
		startTestHarness();
		
		//We create a send a series of SalesForce events
		List<String> bodies = new ArrayList<String>();
		for(int i=0; i<10 ; i++)
		{
			String newBody = "<person><firstName>customer-"+i+"</firstName><lastName>Smith</lastName><city>New York</city></person>";
			bodies.add(newBody);
			
			template.sendBody(salesForcePublisher, newBody);
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
	public void test05CamelRouteWhenMsTransformationFails() throws Exception
	{
		//start stub Microsoft Dynamics
		startStubMsCrm();
		
		//start stub SAP ECC
		startStubSapEcc();
		
		//start harness
		startTestHarness();
		
		//we simulate SalesForce publishes a 'new customer' event.
		//String sample = (String)template.requestBody("direct:get-sample", "message1.xml");
		//template.sendBody(salesForcePublisher, sample);
		template.sendBody(salesForcePublisher, "invalid");
		
		//set expectations
        MockEndpoint mockMs = getMockEndpoint("mock:ms-crm");
        mockMs.expectedMessageCount(0);
        
		//set expectations
        MockEndpoint mockSap = getMockEndpoint("mock:sap-ecc");
        mockSap.expectedMessageCount(1);
        
        //set expectations
        MockEndpoint mockMsError = getMockEndpoint("mock:ms-error");
        mockMsError.expectedMessageCount(1);
        
        //set expectations
        MockEndpoint mockSapError = getMockEndpoint("mock:sap-error");
        mockSapError.expectedMessageCount(0);
		
		// Validate our expectations
		assertMockEndpointsSatisfied();
	}

	@Test
	public void test06CamelRouteWhenAdminFixesMsMessage() throws Exception
	{
		//start stub Microsoft Dynamics
		startStubMsCrm();
		
		//start stub SAP ECC
		startStubSapEcc();
		
		//start harness
		startTestHarness();
		
		//we simulate an Admin has manually submitted a fixed file.
		template.sendBody("direct:simulate-manual-fix", "message1.xml");
		
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
		
		// Validate our expectations
		assertMockEndpointsSatisfied();
	}
	
	
	@Override
	protected ClassPathXmlApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext(
				"META-INF/spring/camel-context.xml");
	}

}
