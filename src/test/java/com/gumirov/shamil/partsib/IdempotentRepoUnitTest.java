package com.gumirov.shamil.partsib;

import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.processor.idempotent.FileIdempotentRepository;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import static org.apache.camel.builder.ExpressionBuilder.append;

public class IdempotentRepoUnitTest extends CamelTestSupport {

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce(uri = "direct:from")
  protected FluentProducerTemplate template;

  @Test
  public void testSendMatchingMessage() throws Exception {

//    resultEndpoint.expectedHeaderValuesReceivedInAnyOrder("CamelFileName", Arrays.asList("1","2"));
    resultEndpoint.expectedMessageCount(3);

//    template.sendBodyAndHeader(" ", "CamelFileName", "f1");
//    template.sendBodyAndHeader(" ", "CamelFileName", "f2");
//    template.sendBodyAndHeader(" ", "CamelFileName", "f1");
    template.to("direct:from").withBody(" ").
      withHeader("CamelFileName", "f1").
      withHeader("CamelFileLength", "1").
      send();
    template.to("direct:from").withBody(" ").
      withHeader("CamelFileName", "f1").
      withHeader("CamelFileLength", "2").
      send();
    template.to("direct:from").withBody(" ").
      withHeader("CamelFileName", "f2").
      withHeader("CamelFileLength", "1").
      send();
    template.to("direct:from").withBody(" ").
      withHeader("CamelFileName", "f2").
      withHeader("CamelFileLength", "1").
      send();

    resultEndpoint.assertIsSatisfied();
  }

  @Before
  public void setup() {
    File idempotentRepo = new File("target/idempotent_repo.dat");
    if (idempotentRepo.exists()) {
      idempotentRepo.delete();
      log.info("removed repo");
    }
  }

  @Override
  protected RoutesBuilder createRouteBuilder() {
    return new RouteBuilder()
    {
      @Override
      public void configure() throws Exception
      {
        File idempotentRepo = new File("target/idempotent_repo.dat");
        if (!idempotentRepo.exists()) idempotentRepo.createNewFile();
        idempotentRepo.deleteOnExit();
        from("direct:from").
          idempotentConsumer(
              append(append(header("CamelFileName"), simple("-")), header("CamelFileLength")),
              FileIdempotentRepository.fileIdempotentRepository(idempotentRepo,
                  100000, 102400000)).
          to("mock:result");
      }
    };
  }
}
