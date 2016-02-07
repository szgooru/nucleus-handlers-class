package org.gooru.nucleus.handlers.classes.processors;

import io.vertx.core.eventbus.Message;

public final class ProcessorBuilder {

  public static Processor build(Message<Object> message) {
    return new MessageProcessor(message);
  }

  private ProcessorBuilder() {
    throw new AssertionError();
  }
}
