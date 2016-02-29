package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import io.vertx.core.json.JsonObject;
import org.gooru.nucleus.handlers.classes.constants.MessageConstants;
import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.events.EventBuilderFactory;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth.AuthorizerBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJClassMember;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.DBException;
import org.javalite.activejdbc.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.ResourceBundle;

/**
 * Created by ashish on 8/2/16.
 */
class DeleteClassHandler implements DBHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(DeleteClassHandler.class);
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
  private final ProcessorContext context;
  private AJEntityClass entityClass;

  DeleteClassHandler(ProcessorContext context) {
    this.context = context;
  }

  @Override
  public ExecutionResult<MessageResponse> checkSanity() {
    if (context.classId() == null || context.classId().isEmpty()) {
      LOGGER.warn("Missing class id");
      return new ExecutionResult<>(MessageResponseFactory.createNotFoundResponse(RESOURCE_BUNDLE.getString("missing.class.id")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
    // The user should not be anonymous
    if (context.userId() == null || context.userId().isEmpty() || context.userId().equalsIgnoreCase(MessageConstants.MSG_USER_ANONYMOUS)) {
      LOGGER.warn("Anonymous user attempting to delete class");
      return new ExecutionResult<>(MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
        ExecutionResult.ExecutionStatus.FAILED);
    }

    return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
  }

  @Override
  public ExecutionResult<MessageResponse> validateRequest() {
    LazyList<AJEntityClass> entityClasses = AJEntityClass.findBySQL(AJEntityClass.DELETE_QUERY, context.classId());
    if (entityClasses.isEmpty()) {
      LOGGER.warn("Class id '{}' not present in DB", context.classId());
      return new ExecutionResult<>(MessageResponseFactory.createNotFoundResponse(RESOURCE_BUNDLE.getString("class.id") + context.classId()),
        ExecutionResult.ExecutionStatus.FAILED);
    }
    this.entityClass = entityClasses.get(0);
    // Class should be of current version and Class should not be archived
    if (!this.entityClass.isCurrentVersion() || this.entityClass.isArchived()) {
      LOGGER.warn("Class '{}' is either archived or not of current version, can not delete", context.classId());
      return new ExecutionResult<>(
        MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("class.archived.or.incorrect.version")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
    String courseId = this.entityClass.getString(AJEntityClass.COURSE_ID);
    if (courseId != null && !courseId.isEmpty()) {
      LOGGER.warn("Delete request for class '{}' which is having a course '{}'", context.classId(), courseId);
    }

    return AuthorizerBuilder.buildDeleteAuthorizer(context).authorize(this.entityClass);
  }

  @Override
  public ExecutionResult<MessageResponse> executeRequest() {
    this.entityClass.setBoolean(AJEntityClass.IS_DELETED, true);
    this.entityClass.setModifierId(context.userId());

    boolean result = this.entityClass.save();
    if (!result) {
      LOGGER.error("Class with id '{}' failed to delete", context.classId());
      if (this.entityClass.hasErrors()) {
        Map<String, String> map = this.entityClass.errors();
        JsonObject errors = new JsonObject();
        map.forEach(errors::put);
        return new ExecutionResult<>(MessageResponseFactory.createValidationErrorResponse(errors), ExecutionResult.ExecutionStatus.FAILED);
      }
    }
    try {
      Base.exec(AJClassMember.DELETE_MEMBERSHIP_FOR_CLASS_QUERY, context.classId());
      return new ExecutionResult<>(MessageResponseFactory
        .createNoContentResponse(RESOURCE_BUNDLE.getString("deleted"), EventBuilderFactory.getDeleteClassEventBuilder(context.classId())),
        ExecutionResult.ExecutionStatus.SUCCESSFUL);
    } catch (DBException dbe) {
      LOGGER.warn("Unable to delete membership details for class '{}' delete request", context.classId());
      return new ExecutionResult<>(
        MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("membership.delete.failure")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
  }

  @Override
  public boolean handlerReadOnly() {
    return false;
  }
}
