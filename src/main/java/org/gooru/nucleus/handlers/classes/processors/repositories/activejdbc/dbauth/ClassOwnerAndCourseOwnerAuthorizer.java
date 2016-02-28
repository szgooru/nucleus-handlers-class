package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.DBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * Created by ashish on 8/2/16.
 */
class ClassOwnerAndCourseOwnerAuthorizer implements Authorizer<AJEntityClass> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClassOwnerAndCourseOwnerAuthorizer.class);
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
  private final ProcessorContext context;

  ClassOwnerAndCourseOwnerAuthorizer(ProcessorContext context) {
    this.context = context;
  }

  @Override
  public ExecutionResult<MessageResponse> authorize(AJEntityClass model) {
    // Check user's ownership of class
    String creatorId = model.getString(AJEntityClass.CREATOR_ID);
    if (creatorId == null || creatorId.isEmpty() || !creatorId.equalsIgnoreCase(context.userId())) {
      LOGGER.warn("User '{}' is not owner of class '{}'", context.userId(), context.classId());
      return new ExecutionResult<>(MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
    // Check user's ownership of course and course should be a valid course
    return checkCourseAuthorization();

  }

  private ExecutionResult<MessageResponse> checkCourseAuthorization() {
    try {
      long count = Base.count(AJEntityClass.TABLE_COURSE, AJEntityClass.COURSE_ASSOCIATION_FILTER, context.courseId(), context.userId());
      if (count == 1) {
        return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
      }
      return new ExecutionResult<>(
        MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("course.not.found.or.not.available")),
        ExecutionResult.ExecutionStatus.FAILED);
    } catch (DBException e) {
      LOGGER.error("Error querying course '{}' availability for associating in class '{}'", context.courseId(), context.classId(), e);
      return new ExecutionResult<>(MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
  }
}
