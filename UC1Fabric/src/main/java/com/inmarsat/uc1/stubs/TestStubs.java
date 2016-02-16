package com.inmarsat.uc1.stubs;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class TestStubs extends RouteBuilder {

    @Override
    public void configure() throws Exception
    {
		//This stub simulates Microsoft Dynamics
		//It can be configured to validate XML against Schema v1 or against v2.
		from("netty-http:http://localhost:19999/ms-dyn").id("stub-ms-dynamics")
			.onException(org.apache.camel.processor.validation.SchemaValidationException.class)
				.log("MS Schema ${sys.ms-stub-schema-version} validation failure.")
				.handled(true)
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
				.setBody().simple("invalid data")
				.end()
			.convertBodyTo(String.class)
			.to("direct:stub-recorder-ms")
			.log("MS validating with Schema ${sys.ms-stub-schema-version}.")
			.choice()
				.when().simple("${sys.ms-stub-schema-version} == 'v2'")
					.to("validator:schema/ms-stub-v2.xsd")
				.otherwise()
					.to("validator:schema/ms-stub-v1.xsd")
			.end();

		//This stub simulates SAP ECC
		from("netty-http:http://localhost:29999/sap-ecc").id("stub-sap-ecc")
			.to("direct:stub-recorder-sap");
		

		//keeps track of received messages in MS
		from("direct:stub-recorder-ms")
			.to("mock:ms-crm");
		
		//keeps track of received messages in SAP
		from("direct:stub-recorder-sap")
			.to("mock:sap-ecc");
    }
}


