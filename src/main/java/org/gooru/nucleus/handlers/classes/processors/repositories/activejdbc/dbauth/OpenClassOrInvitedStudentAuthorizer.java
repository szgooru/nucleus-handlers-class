package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;

/**
 * Created by ashish on 8/2/16.
 */
class OpenClassOrInvitedStudentAuthorizer implements Authorizer<AJEntityClass> {
  public OpenClassOrInvitedStudentAuthorizer(ProcessorContext context) {
  }

  @Override
  public ExecutionResult<MessageResponse> authorize(AJEntityClass model) {
    return null;
  }
}
