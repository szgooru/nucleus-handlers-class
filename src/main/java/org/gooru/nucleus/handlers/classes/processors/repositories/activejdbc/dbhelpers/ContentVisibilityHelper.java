package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhelpers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.Utils;
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
 * Created by ashish on 24/3/16.
 */
public final class ContentVisibilityHelper {

  public static JsonObject updateContentVisibility(JsonObject requestPayload, String existingVisibilityString) {
    JsonObject existingVisibility =
      existingVisibilityString != null && !existingVisibilityString.isEmpty() ? new JsonObject(existingVisibilityString) : null;
    return mergeContentVisibility(requestPayload, existingVisibility);
  }

  public static ExecutionResult<MessageResponse> validatePayloadWithDB(JsonObject payload, String courseId) {
    // TODO: Implement this
    JsonArray contents = payload.getJsonArray(AJEntityClass.CV_UNITS);
    ExecutionResult<MessageResponse> result = validateUnitsExistence(contents, courseId);
    if (result.hasFailed()) {
      return result;
    }
    contents = payload.getJsonArray(AJEntityClass.CV_LESSONS);
    result = validateLessonsExistence(contents, courseId);
    if (result.hasFailed()) {
      return result;
    }
    contents = payload.getJsonArray(AJEntityClass.CV_COLLECTIONS);
    JsonArray contentsAssessment = payload.getJsonArray((AJEntityClass.CV_ASSESSMENTS));
    return validateCollectionsAssessmentsExistence(contents, contentsAssessment, courseId);
  }

  private static final String UNITS_QUERY_FILTER = "course_id = ?::uuid and unit_id = ANY(?::uuid[]) and is_deleted = false";
  private static final String LESSONS_QUERY_FILTER = "course_id = ?::uuid and lesson_id = ANY(?::uuid[]) and is_deleted = false";
  private static final String COLLECTIONS_QUERY_FILTER = "course_id = ?::uuid and id = ANY(?::uuid[]) and is_deleted = false";
  private static final Logger LOGGER = LoggerFactory.getLogger(ContentVisibilityHelper.class);
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
  private static final String TABLE_UNIT = "unit";
  private static final String TABLE_LESSON = "lesson";
  private static final String TABLE_COLLECTION = "collection";

  private static JsonObject mergeContentVisibility(JsonObject request, JsonObject existingVisibility) {
    if (request == null || request.isEmpty()) {
      return existingVisibility.copy();
    } else if (existingVisibility == null || existingVisibility.isEmpty()) {
      return request.copy();
    } else {
      JsonObject result = request.copy();
      existingVisibility.forEach(entry -> {
        String key = entry.getKey();
        Object value = entry.getValue();
        if (key != null && !key.isEmpty() && value != null) {
          JsonArray originalValue = result.getJsonArray(key);
          if (originalValue == null) {
            result.put(key, value);
          } else {
            JsonArray newValue = originalValue.copy();
            ((JsonArray) value).forEach(newValue::add);
            result.put(key, newValue);
          }
        }
      });
      return result;
    }
  }

  private static ExecutionResult<MessageResponse> validateUnitsExistence(JsonArray units, String courseId) {
    try {
      if (validateContentCountInCourse(units, courseId, TABLE_UNIT, UNITS_QUERY_FILTER)) {
        return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
      } else {
        return new ExecutionResult<>(
          MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("content.visibility.unit.course.invalid")),
          ExecutionResult.ExecutionStatus.FAILED);
      }
    } catch (DBException dbe) {
      LOGGER.error("Error validating unit counts for visibility setting for course '{}'", courseId, dbe);
      return new ExecutionResult<>(MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
  }

  private static ExecutionResult<MessageResponse> validateLessonsExistence(JsonArray lessons, String courseId) {
    try {
      if (validateContentCountInCourse(lessons, courseId, TABLE_LESSON, LESSONS_QUERY_FILTER)) {
        return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
      } else {
        return new ExecutionResult<>(
          MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("content.visibility.lesson.course.invalid")),
          ExecutionResult.ExecutionStatus.FAILED);
      }
    } catch (DBException dbe) {
      LOGGER.error("Error validating lesson counts for visibility setting for course '{}'", courseId, dbe);
      return new ExecutionResult<>(MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
  }

  private static ExecutionResult<MessageResponse> validateCollectionsAssessmentsExistence(JsonArray collections, JsonArray assessments,
                                                                                          String courseId) {
    JsonArray input = new JsonArray();
    if (collections != null && !collections.isEmpty()) {
      input.addAll(collections);
    }
    if (assessments != null && !assessments.isEmpty()) {
      input.addAll(assessments);
    }
    try {
      if (validateContentCountInCourse(input, courseId, TABLE_COLLECTION, COLLECTIONS_QUERY_FILTER)) {
        return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
      } else {
        return new ExecutionResult<>(
          MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("content.visibility.collection.course.invalid")),
          ExecutionResult.ExecutionStatus.FAILED);
      }
    } catch (DBException dbe) {
      LOGGER.error("Error validating collection counts for visibility setting for course '{}'", courseId, dbe);
      return new ExecutionResult<>(MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
  }

  /*
   * Note that this may throw DBException which should be handled by caller
   */
  private static boolean validateContentCountInCourse(JsonArray contentIds, String courseId, String tableName, String dbQuery) {
    if (contentIds == null || contentIds.isEmpty()) {
      return true;
    }
    // Should we set expectedCount by trying to remove duplicates in the contentIds list first???
    long expectedCount = contentIds.size();
    long count = Base.count(tableName, dbQuery, courseId, Utils.convertListToPostgresArrayStringRepresentation(contentIds.getList()));

    return expectedCount == count;
  }

  private ContentVisibilityHelper() {
    throw new AssertionError("Should not be instantiated");
  }
}
