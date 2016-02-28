package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJClassMember;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.javalite.activejdbc.LazyList;

import java.util.ResourceBundle;

/**
 * Created by ashish on 8/2/16.
 */
class OpenClassOrInvitedStudentAuthorizer implements Authorizer<AJEntityClass> {
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
  private final ProcessorContext context;
  private final AJClassMember membership;

  public OpenClassOrInvitedStudentAuthorizer(ProcessorContext context, AJClassMember membership) {
    this.context = context;
    this.membership = membership;
  }

  @Override
  public ExecutionResult<MessageResponse> authorize(AJEntityClass model) {
    if (checkClassTypeOpen(model) || checkStudentInvited(model)) {
      return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
    }
    return new ExecutionResult<>(MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
      ExecutionResult.ExecutionStatus.FAILED);

  }

  private boolean checkClassTypeOpen(AJEntityClass model) {
    return AJEntityClass.CLASS_SHARING_TYPE_OPEN.equalsIgnoreCase(model.getString(AJEntityClass.CLASS_SHARING));
  }

  private boolean checkStudentInvited(AJEntityClass model) {
    if (this.membership == null) {
      return false;
    }
    // Membership record means that user is either invited or already joined
    return true;
  }
}
