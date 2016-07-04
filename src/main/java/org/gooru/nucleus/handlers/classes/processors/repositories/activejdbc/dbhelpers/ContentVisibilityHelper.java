package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.Utils;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityCollection;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.DBException;
import org.javalite.activejdbc.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Created by ashish on 24/3/16.
 */
public final class ContentVisibilityHelper {

    /**
     * Validate that all the collections and assessments are present and not deleted and are not visible for that class.
     * In case they are either not present, or deleted or already visible, this is an error. Note that this function
     * does not check the class setting. This should have happened already.
     *
     * @param payload
     * @param courseId
     * @param classId
     * @return ExecutionResult which signifies whether to continue or not
     */
    public static ExecutionResult<MessageResponse> validatePayloadWithDB(JsonObject payload, String courseId,
        String classId) {
        JsonArray contentsCollection = payload.getJsonArray(AJEntityClass.CV_COLLECTIONS);
        JsonArray contentsAssessment = payload.getJsonArray((AJEntityClass.CV_ASSESSMENTS));
        return validateCollectionsAssessmentsExistence(contentsCollection, contentsAssessment, courseId, classId);
    }

    /**
     * Validate that based on class setting, the payload is doing right thing.
     * If class setting is visible all then it is an error to call this API
     * If collections are visible as per class setting, then it is error to try and make collections visible
     * If all is hidden, then call is assumed to be fine.
     *
     * @param payload
     * @param entityClass
     * @return ExecutionResult signifying whether to continue post this validation
     */
    public static ExecutionResult<MessageResponse> validatePayloadWithClassSetting(JsonObject payload,
        AJEntityClass entityClass, String entity) {
        String visibility = entityClass.getContentVisibility();
        if (AJEntityClass.CONTENT_VISIBILITY_TYPE_VISIBLE_ALL.equalsIgnoreCase(visibility)) {
            return new ExecutionResult<>(MessageResponseFactory
                .createInvalidRequestResponse(RESOURCE_BUNDLE.getString("class.setting.visible.all.error")),
                ExecutionResult.ExecutionStatus.FAILED);
        } else if (AJEntityClass.CONTENT_VISIBILITY_TYPE_VISIBLE_COLLECTION.equalsIgnoreCase(visibility)) {
            if (payload.getJsonArray(AJEntityClass.CV_COLLECTIONS) != null
                || (entity != null && entity.equalsIgnoreCase(AJEntityCollection.FORMAT_TYPE_COLLECTION))) {
                return new ExecutionResult<>(
                    MessageResponseFactory.createInvalidRequestResponse(
                        RESOURCE_BUNDLE.getString("class.setting.visible.collections.error")),
                    ExecutionResult.ExecutionStatus.FAILED);
            }
        }
        return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentVisibilityHelper.class);
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");

    private static ExecutionResult<MessageResponse> validateCollectionsAssessmentsExistence(JsonArray collections,
        JsonArray assessments, String courseId, String classId) {
        JsonArray input = new JsonArray();
        if (collections != null && !collections.isEmpty()) {
            input.addAll(collections);
        }
        if (assessments != null && !assessments.isEmpty()) {
            input.addAll(assessments);
        }
        if (!input.isEmpty()) {
            try {
                // Should we set expectedCount by trying to remove duplicates in the
                // contentIds list first???
                long expectedCount = input.size();
                long count =
                    Base.count(AJEntityCollection.TABLE_COLLECTION, AJEntityCollection.COLLECTIONS_QUERY_FILTER,
                        courseId, Utils.convertListToPostgresArrayStringRepresentation(input.getList()), classId);

                if (expectedCount == count) {
                    return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
                } else {
                    return new ExecutionResult<>(MessageResponseFactory.createInvalidRequestResponse(
                        RESOURCE_BUNDLE.getString("content.visibility.collection.course.invalid")),
                        ExecutionResult.ExecutionStatus.FAILED);
                }
            } catch (DBException dbe) {
                LOGGER
                    .error("Error validating collection counts for visibility setting for course '{}'", courseId, dbe);
                return new ExecutionResult<>(
                    MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
                    ExecutionResult.ExecutionStatus.FAILED);
            }

        } else {
            return new ExecutionResult<>(
                MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("empty.payload")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
    }
    
    public static List<String> getNonVisibleCollectionsAssessments(String classId, String courseId, String unitId, String lessonId) {
        List<String> collections = getNonVisibleItems(classId, courseId, unitId, lessonId, AJEntityCollection.FORMAT_TYPE_COLLECTION);
        List<String> assessments = getNonVisibleItems(classId, courseId, unitId, lessonId, AJEntityCollection.FORMAT_TYPE_ASSESSMENT);
        List<String> extAssessments = getNonVisibleItems(classId, courseId, unitId, lessonId, AJEntityCollection.FORMAT_TYPE_ASSESSMENT_EXT);
        
        int resultSize = collections.size() + assessments.size() + extAssessments.size();
        List<String> result = new ArrayList<>(resultSize);
        if (collections != null) {
            result.addAll(collections);
        }
        
        if (assessments != null) {
            result.addAll(assessments);
        }
        
        if (extAssessments != null) {
            result.addAll(extAssessments);
        }
        
        return result;
    }
    
    public static List<String> getNonVisibleItems(String classId, String courseId, String unitId, String lessonId, String type) {
        LazyList<AJEntityCollection> collections;
        if (courseId != null && unitId != null && lessonId != null) {
            collections = AJEntityCollection.findBySQL(AJEntityCollection.SELECT_NONVISIBLE_ITEMS_BY_CUL, classId, type, courseId, unitId, lessonId);
        } else if (courseId != null && unitId != null ) {
            collections = AJEntityCollection.findBySQL(AJEntityCollection.SELECT_NONVISIBLE_ITEMS_BY_CU, classId, type, courseId, unitId);
        } else {
            collections = AJEntityCollection.findBySQL(AJEntityCollection.SELECT_NONVISIBLE_ITEMS_BY_C, classId, type, courseId);
        }
        
        List<String> ids = new ArrayList<>(collections.size());
        if (collections != null && !collections.isEmpty()) {
            collections.forEach(collection -> ids.add(collection.get(AJEntityCollection.ID).toString()));
        }
        LOGGER.debug("number of non visible '{}': {}", type, ids.size());
        return ids;
    }

    private ContentVisibilityHelper() {
        throw new AssertionError("Should not be instantiated");
    }
}
