/**
 * 
 */
package com.inmarsat.uc2;

import java.util.ArrayList;
import java.util.List;

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
	
/*	@EndpointInject(uri = "netty-http:http://localhost:10000/esb")
	protected Endpoint esb;
	*/
	String esb = "netty-http:http://localhost:10000/esb";
	
	private void startTestHarness() throws Exception
	{
		context.addRoutes(new RouteBuilder()
		{
			@Override
			public void configure() throws Exception
			{
				//This stub simulates the SOAP backend system providing a list of customers
				from("netty-http:http://localhost:19999/customer")
					.log("got request...")
					.pollEnrich("file:src/data?noop=true&fileName=privateCustomers.xml")
					.to("validator:schemas/customers.xsd")
					.to("mock:soap-stub");

				//This stub simulates there is a decoupled system serving XSLTs
				//making it simple to replace transformation causing no impact on Fuse
				from("netty-http:http://localhost:29999/xslt")
					.log("serving xslt...")
					.pollEnrich("file:src/data/xslt?noop=true&fileName=transformResponse.xsl")
					.to("mock:xslt-stub");
			}
		});
	}	

	@Test
	public void test01CamelRouteEndToEndLocalXSLT() throws Exception
	{
		//We configure the system to perform transformations using local XSLTs
		System.setProperty("transform.mode", "default");
		
		//start harness
		startTestHarness();
		
		//we prepare the request.
		DefaultExchange request = new DefaultExchange(context);
		
		//We simulate the user interface triggers a request
		Exchange response = template.send(esb, request);
		
		//System.out.println(">>>>>>>>>>>>>>>>>> response:\n"+response.getIn().getBody(String.class));
		
		//set expectations
        MockEndpoint mockSoapStub = getMockEndpoint("mock:soap-stub");
        mockSoapStub.expectedMessageCount(1);
        
		//set expectations
        MockEndpoint mockXsltStub = getMockEndpoint("mock:xslt-stub");
        mockXsltStub.expectedMessageCount(0);
        
		//Validate our expectations
		assertMockEndpointsSatisfied();
		
		//expected JSON body back
		String expected = "[{\"name\":\"John\",\"email\":\"John@king.st\"},{\"name\":\"Lucy\",\"email\":\"lucy@newcourt.st\"}]";
		
		//validate
		assertEquals("response not matching", expected, response.getIn().getBody(String.class));
	}
	
	@Test
	public void test02CamelRouteEndToEndRemoteXSLT() throws Exception
	{
		//We configure the system to perform transformations from remote XSLTs
		System.setProperty("transform.mode", "remote");
		
		//start harness
		startTestHarness();
		
		//we prepare the request.
		DefaultExchange request = new DefaultExchange(context);
		
		//We simulate the user interface triggers a request
		Exchange response = template.send(esb, request);
		
		//System.out.println(">>>>>>>>>>>>>>>>>> response:\n"+response.getIn().getBody(String.class));
		
		//set expectations
        MockEndpoint mockSoapStub = getMockEndpoint("mock:soap-stub");
        mockSoapStub.expectedMessageCount(1);
        
		//set expectations
        MockEndpoint mockXsltStub = getMockEndpoint("mock:xslt-stub");
        mockXsltStub.expectedMessageCount(1);
        
		//Validate our expectations
		assertMockEndpointsSatisfied();
		
		//expected JSON body back
		String expected = "[{\"name\":\"John\",\"email\":\"John@king.st\"},{\"name\":\"Lucy\",\"email\":\"lucy@newcourt.st\"}]";
		
		//validate
		assertEquals("response not matching", expected, response.getIn().getBody(String.class));
	}
	
	//This test is currently disabled while waiting to complete this type of transformer
	@Test
	@Ignore
	public void test03CamelRouteEndToEndMapper() throws Exception
	{
		//We configure the system to perform transformations using the GUI defined mapping
		System.setProperty("transform.mode", "data-mapper");
		
		//start harness
		startTestHarness();
		
		//we prepare the request.
		DefaultExchange request = new DefaultExchange(context);
		
		//We simulate the user interface triggers a request
		Exchange response = template.send(esb, request);
		
		//System.out.println(">>>>>>>>>>>>>>>>>> response:\n"+response.getIn().getBody(String.class));
		
		//set expectations
        MockEndpoint mockSoapStub = getMockEndpoint("mock:soap-stub");
        mockSoapStub.expectedMessageCount(1);
        
		//set expectations
        MockEndpoint mockXsltStub = getMockEndpoint("mock:xslt-stub");
        mockXsltStub.expectedMessageCount(0);
        
		//Validate our expectations
		assertMockEndpointsSatisfied();
		
		//expected JSON body back
		String expected = "[{\"name\":\"John\",\"email\":\"John@king.st\"},{\"name\":\"Lucy\",\"email\":\"lucy@newcourt.st\"}]";
		
		//validate
		assertEquals("response not matching", expected, response.getIn().getBody(String.class));
	}

	
	@Override
	protected ClassPathXmlApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext(
				"META-INF/spring/camel-context.xml");
	}

}
