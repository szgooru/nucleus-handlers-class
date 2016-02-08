package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;

/**
 * Created by ashish on 8/2/16.
 */
class DeleteClassHandler implements DBHandler {
  private final ProcessorContext context;

  DeleteClassHandler(ProcessorContext context) {
    this.context = context;
  }

  @Override
  public ExecutionResult<MessageResponse> checkSanity() {
    return null;
  }

  @Override
  public ExecutionResult<MessageResponse> validateRequest() {
    return null;
  }

  @Override
  public ExecutionResult<MessageResponse> executeRequest() {
    return null;
  }

  @Override
  public boolean handlerReadOnly() {
    return false;
  }
}
