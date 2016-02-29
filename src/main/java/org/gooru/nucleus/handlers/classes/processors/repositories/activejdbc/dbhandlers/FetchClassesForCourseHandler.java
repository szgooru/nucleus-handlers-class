package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.gooru.nucleus.handlers.classes.constants.MessageConstants;
import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth.AuthorizerBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.formatter.JsonFormatterBuilder;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.javalite.activejdbc.DBException;
import org.javalite.activejdbc.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * Created by ashish on 8/2/16.
 */
class FetchClassesForCourseHandler implements DBHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(FetchClassesForCourseHandler.class);
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
  private final ProcessorContext context;
  private static final String RESPONSE_BUCKET_CLASSES = "classes";

  FetchClassesForCourseHandler(ProcessorContext context) {
    this.context = context;
  }

  @Override
  public ExecutionResult<MessageResponse> checkSanity() {
    if (context.courseId() == null || context.courseId().isEmpty()) {
      LOGGER.warn("Missing course");
      return new ExecutionResult<>(MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("missing.course")),
        ExecutionResult.ExecutionStatus.FAILED);
    }

    if (context.userId() == null || context.userId().isEmpty() || MessageConstants.MSG_USER_ANONYMOUS.equalsIgnoreCase(context.userId())) {
      LOGGER.warn("Invalid user");
      return new ExecutionResult<>(MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
    return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
  }

  @Override
  public ExecutionResult<MessageResponse> validateRequest() {
    return AuthorizerBuilder.buildFetchClassesForCourseAuthorizer(this.context).authorize(null);
  }

  @Override
  public ExecutionResult<MessageResponse> executeRequest() {
    try {
      LazyList<AJEntityClass> classes = AJEntityClass.where(AJEntityClass.FETCH_FOR_COURSE_QUERY_FILTER, context.courseId());
      JsonArray classesList =
        new JsonArray(JsonFormatterBuilder.buildSimpleJsonFormatter(false, AJEntityClass.FETCH_QUERY_FIELD_LIST).toJson(classes));
      return new ExecutionResult<>(MessageResponseFactory.createOkayResponse(new JsonObject().put(RESPONSE_BUCKET_CLASSES, classesList)),
        ExecutionResult.ExecutionStatus.SUCCESSFUL);
    } catch (DBException e) {
      LOGGER.error("Not able to fetch class from DB", e);
      return new ExecutionResult<>(MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
  }

  @Override
  public boolean handlerReadOnly() {
    return true;
  }
}
