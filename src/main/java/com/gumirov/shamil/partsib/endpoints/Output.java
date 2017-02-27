package com.gumirov.shamil.partsib.endpoints;

import org.apache.camel.Processor;
import org.apache.camel.impl.ProcessorEndpoint;

public class Output extends ProcessorEndpoint {

  public Output(Processor processor) {
    setProcessor(processor);
  }

  @Override
  protected String createEndpointUri() {
    return "outputprocessor";
  }
}
