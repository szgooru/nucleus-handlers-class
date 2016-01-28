package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;

/**
 * Created by ashish on 11/1/16.
 */
public interface DBHandler {
  ExecutionResult<MessageResponse> checkSanity();

  ExecutionResult<MessageResponse> validateRequest();

  ExecutionResult<MessageResponse> executeRequest();

  boolean handlerReadOnly();
}
