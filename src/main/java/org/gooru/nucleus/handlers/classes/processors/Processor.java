package org.gooru.nucleus.handlers.classes.processors;

import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;

public interface Processor {
  public MessageResponse process();
}
