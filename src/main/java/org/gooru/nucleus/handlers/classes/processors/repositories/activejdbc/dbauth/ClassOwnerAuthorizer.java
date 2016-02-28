package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * Created by ashish on 8/2/16.
 */
class ClassOwnerAuthorizer implements Authorizer<AJEntityClass> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClassOwnerAuthorizer.class);
  private final ProcessorContext context;
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");


  ClassOwnerAuthorizer(ProcessorContext context) {
    this.context = context;
  }

  @Override
  public ExecutionResult<MessageResponse> authorize(AJEntityClass model) {
    String creatorId = model.getString(AJEntityClass.CREATOR_ID);
    if (creatorId == null || creatorId.isEmpty() || !creatorId.equalsIgnoreCase(context.userId())) {
      LOGGER.warn("User '{}' is not owner of class '{}'", context.userId(), context.classId());
      return new ExecutionResult<>(MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
        ExecutionResult.ExecutionStatus.FAILED);
    }

    return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
  }
}
