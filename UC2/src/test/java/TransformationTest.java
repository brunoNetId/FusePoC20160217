

import java.io.FileInputStream;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TransformationTest extends CamelSpringTestSupport {
    
    @EndpointInject(uri = "mock:test01-test-output")
    private MockEndpoint resultEndpoint;
    
    @Produce(uri = "direct:test01-test-input")
    private ProducerTemplate startEndpoint;
    
    @Test
    public void transform() throws Exception
    {
        String body = readFile("src/data/privateCustomers.xml");
        
        //This test is simply to inspect the data mapping behaviour closer
        startEndpoint.sendBody(body);
    }
    
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:test01-test-input")
                	.log("validating input")
                	.to("validator:schemas/customers.xsd")
                    .log("Before transformation:\n ${body}")
                    .to("ref:myTransform")
                    .log("After transformation:\n ${body}")
                    .to("mock:test01-test-output");
            }
        };
    }
    
    @Override
    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("META-INF/spring/camel-context.xml");
    }
    
    private String readFile(String filePath) throws Exception {
        String content;
        FileInputStream fis = new FileInputStream(filePath);
        try {
             content = createCamelContext().getTypeConverter().convertTo(String.class, fis);
        } finally {
            fis.close();
        }
        return content;
    }
}
