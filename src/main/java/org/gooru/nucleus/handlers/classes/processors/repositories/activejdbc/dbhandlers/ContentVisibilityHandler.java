package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.gooru.nucleus.handlers.classes.constants.MessageConstants;
import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.events.EventBuilderFactory;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.Utils;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth.AuthorizerBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhelpers.ContentVisibilityHelper;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityCollection;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.validators.PayloadValidator;
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
 * Created by ashish on 21/3/16.
 */
public class ContentVisibilityHandler implements DBHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentVisibilityHandler.class);
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
    private final ProcessorContext context;
    private AJEntityClass entityClass;
    private String courseId;

    private String type;
    private String entity;
    private String courseIdFromRequest;
    private String unitIdFromRequest;
    private String lessonIdFromRequest;

    private JsonObject scope;
    private JsonObject boundry;

    ContentVisibilityHandler(ProcessorContext context) {
        this.context = context;
    }

    @Override
    public ExecutionResult<MessageResponse> checkSanity() {
        // There should be a class id present
        if (context.classId() == null || context.classId().isEmpty()) {
            LOGGER.warn("Missing class");
            return new ExecutionResult<>(
                MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("missing.class.id")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        // The user should not be anonymous
        if (context.userId() == null || context.userId().isEmpty()
            || context.userId().equalsIgnoreCase(MessageConstants.MSG_USER_ANONYMOUS)) {
            LOGGER.warn("Anonymous user attempting to mark content visible in class");
            return new ExecutionResult<>(
                MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        // Payload should not be empty
        if (context.request() == null || context.request().isEmpty()) {
            LOGGER.warn("Empty payload supplied to student invite in class");
            return new ExecutionResult<>(
                MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("empty.payload")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        // Our validators should certify this
        JsonObject errors = payloadValidator(context.request());
        if (errors != null && !errors.isEmpty()) {
            LOGGER.warn("Validation errors for request");
            return new ExecutionResult<>(MessageResponseFactory.createValidationErrorResponse(errors),
                ExecutionResult.ExecutionStatus.FAILED);
        }

        return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
    }

    @Override
    public ExecutionResult<MessageResponse> validateRequest() {
        LazyList<AJEntityClass> classes = AJEntityClass.where(AJEntityClass.FETCH_QUERY_FILTER, context.classId());
        if (classes.isEmpty()) {
            LOGGER.warn("Not able to find class '{}'", this.context.classId());
            return new ExecutionResult<>(
                MessageResponseFactory.createNotFoundResponse(RESOURCE_BUNDLE.getString("not.found")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        this.entityClass = classes.get(0);
        // Class should be of current version and Class should not be archived
        if (!entityClass.isCurrentVersion() || entityClass.isArchived()) {
            LOGGER.warn("Class '{}' is either archived or not of current version", context.classId());
            return new ExecutionResult<>(
                MessageResponseFactory
                    .createInvalidRequestResponse(RESOURCE_BUNDLE.getString("class.archived.or.incorrect.version")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        ExecutionResult<MessageResponse> result =
            ContentVisibilityHelper.validatePayloadWithClassSetting(this.context.request(), this.entityClass, this.entity);
        if (result.hasFailed()) {
            return result;
        }
        // Check authorization
        result = AuthorizerBuilder.buildContentVisibilityAuthorizer(this.context).authorize(entityClass);
        if (result.hasFailed()) {
            return result;
        }
        // Class should be associated with course
        courseId = this.entityClass.getString(AJEntityClass.COURSE_ID);
        if (courseId == null) {
            LOGGER.error("Class '{}' is not assigned to course, hence cannot set content visibility",
                context.classId());
            return new ExecutionResult<>(
                MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("class.without.course")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        // Now validate the payload with DB
        ExecutionResult<MessageResponse> exeResult = null;
        if (this.type.equalsIgnoreCase(AJEntityClass.CV_TYPE_SPECIFIC)) {
            exeResult =
                ContentVisibilityHelper.validatePayloadWithDB(context.request(), courseId, this.context.classId());
        } else {
            exeResult = new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
        }

        return exeResult;
    }

    @Override
    public ExecutionResult<MessageResponse> executeRequest() {
        if (this.type.equalsIgnoreCase(AJEntityClass.CV_TYPE_SPECIFIC)) {
            JsonArray input = getInputToMarkVisible();
            // Note that here we are making collection table updates, this does
            // not qualify as collection update
            // So we should not mark modifier id or modified date as the user
            // may not even have access to these collections
            // From their perspective they are doing class operations
            LOGGER.debug("course id: {}", this.courseId);
            LOGGER.debug("collection/assessments to be updated: {}", input.toString());
            try {
                int count =
                    Base.exec(AJEntityCollection.VISIBILITY_DML, new JsonArray().add(this.context.classId()).toString(),
                        this.courseId, Utils.convertListToPostgresArrayStringRepresentation(input.getList()));
                LOGGER.debug("Marked {} items visible", count);
                return new ExecutionResult<>(
                    MessageResponseFactory.createNoContentResponse(RESOURCE_BUNDLE.getString("updated"),
                        EventBuilderFactory.getContentVisibleEventBuilder(context.classId(), context.request())),
                    ExecutionResult.ExecutionStatus.SUCCESSFUL);
            } catch (DBException e) {
                LOGGER.error("Unable to mark content visible for class {}", this.context.classId(), e);
                throw e;
            }
        } else if (this.type.equalsIgnoreCase(AJEntityClass.CV_TYPE_ALL)) {
            try {
                int count = 0;
                List<String> items;
                if (this.entity.equalsIgnoreCase(AJEntityClass.CV_ENTITY_ALL)) {
                    items = ContentVisibilityHelper.getNonVisibleCollectionsAssessments(this.context.classId(),
                        this.courseIdFromRequest, this.unitIdFromRequest, this.lessonIdFromRequest);
                } else if (this.entity.equalsIgnoreCase(AJEntityCollection.FORMAT_TYPE_COLLECTION)) {
                    items = ContentVisibilityHelper.getNonVisibleItems(this.context.classId(), this.courseIdFromRequest,
                        this.unitIdFromRequest, this.lessonIdFromRequest, AJEntityCollection.FORMAT_TYPE_COLLECTION);
                } else {
                    items =
                        ContentVisibilityHelper.getNonVisibleItems(this.context.classId(), this.courseIdFromRequest,
                            this.unitIdFromRequest, this.lessonIdFromRequest, AJEntityCollection.FORMAT_TYPE_ASSESSMENT);
                }

                if (this.courseIdFromRequest != null && this.unitIdFromRequest != null
                    && this.lessonIdFromRequest != null) {
                    if (this.entity.equalsIgnoreCase(AJEntityClass.CV_ENTITY_ALL)) {
                        count = Base.exec(AJEntityCollection.UPDATE_ITEMS_CV_BY_CUL,
                            new JsonArray().add(this.context.classId()).toString(), this.courseIdFromRequest,
                            this.unitIdFromRequest, this.lessonIdFromRequest,
                            Utils.convertListToPostgresArrayStringRepresentation(items));
                    } else if (this.entity.equalsIgnoreCase(AJEntityCollection.FORMAT_TYPE_COLLECTION)) {
                        count = Base.exec(AJEntityCollection.UPDATE_COLLECTIONS_CV_BY_CUL,
                            new JsonArray().add(this.context.classId()).toString(), this.courseIdFromRequest,
                            this.unitIdFromRequest, this.lessonIdFromRequest,
                            Utils.convertListToPostgresArrayStringRepresentation(items));
                    } else {
                        count = Base.exec(AJEntityCollection.UPDATE_ASSESSMENTS_CV_BY_CUL,
                            new JsonArray().add(this.context.classId()).toString(), this.courseIdFromRequest,
                            this.unitIdFromRequest, this.lessonIdFromRequest,
                            Utils.convertListToPostgresArrayStringRepresentation(items));
                    }
                } else if (this.courseIdFromRequest != null && this.unitIdFromRequest != null) {
                    if (this.entity.equalsIgnoreCase(AJEntityClass.CV_ENTITY_ALL)) {
                        count = Base.exec(AJEntityCollection.UPDATE_ITEMS_CV_BY_CU,
                            new JsonArray().add(this.context.classId()).toString(), this.courseIdFromRequest,
                            this.unitIdFromRequest, Utils.convertListToPostgresArrayStringRepresentation(items));
                    } else if (this.entity.equalsIgnoreCase(AJEntityCollection.FORMAT_TYPE_COLLECTION)) {
                        count = Base.exec(AJEntityCollection.UPDATE_COLLECTIONS_CV_BY_CU,
                            new JsonArray().add(this.context.classId()).toString(), this.courseIdFromRequest,
                            this.unitIdFromRequest, Utils.convertListToPostgresArrayStringRepresentation(items));
                    } else {
                        count = Base.exec(AJEntityCollection.UPDATE_ASSESSMENTS_CV_BY_CU,
                            new JsonArray().add(this.context.classId()).toString(), this.courseIdFromRequest,
                            this.unitIdFromRequest, Utils.convertListToPostgresArrayStringRepresentation(items));
                    }
                } else if (this.courseIdFromRequest != null) {
                    if (this.entity.equalsIgnoreCase(AJEntityClass.CV_ENTITY_ALL)) {
                        count = Base.exec(AJEntityCollection.UPDATE_ITEMS_CV_BY_C,
                            new JsonArray().add(this.context.classId()).toString(), this.courseIdFromRequest,
                            Utils.convertListToPostgresArrayStringRepresentation(items));
                    } else if (this.entity.equalsIgnoreCase(AJEntityCollection.FORMAT_TYPE_COLLECTION)) {
                        count = Base.exec(AJEntityCollection.UPDATE_COLLECTIONS_CV_BY_C,
                            new JsonArray().add(this.context.classId()).toString(), this.courseIdFromRequest,
                            Utils.convertListToPostgresArrayStringRepresentation(items));
                    } else {
                        count = Base.exec(AJEntityCollection.UPDATE_ASSESSMENTS_CV_BY_C,
                            new JsonArray().add(this.context.classId()).toString(), this.courseIdFromRequest,
                            Utils.convertListToPostgresArrayStringRepresentation(items));
                    }
                }

                LOGGER.debug("Marked {} items visible", count);
                return new ExecutionResult<>(
                    MessageResponseFactory.createNoContentResponse(RESOURCE_BUNDLE.getString("updated"),
                        EventBuilderFactory.getContentVisibleEventBuilder(context.classId(), context.request())),
                    ExecutionResult.ExecutionStatus.SUCCESSFUL);
            } catch (DBException e) {
                LOGGER.error("Unable to mark content visible for class {}", this.context.classId(), e);
                throw e;
            }
        }

        return new ExecutionResult<>(
            MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.type")),
            ExecutionResult.ExecutionStatus.FAILED);

    }

    @Override
    public boolean handlerReadOnly() {
        return false;
    }

    private JsonArray getInputToMarkVisible() {
        JsonArray input = new JsonArray();
        JsonArray contentsCollection = this.context.request().getJsonArray(AJEntityClass.CV_COLLECTIONS);
        JsonArray contentsAssessment = this.context.request().getJsonArray((AJEntityClass.CV_ASSESSMENTS));
        if (contentsAssessment != null && !contentsAssessment.isEmpty()) {
            input.addAll(contentsAssessment);
        }
        if (contentsCollection != null && !contentsCollection.isEmpty()) {
            input.addAll(contentsCollection);
        }
        return input;
    }

    private JsonObject payloadValidator(JsonObject input) {
        List<String> TYPE_ALLOWED_VALUES = Arrays.asList(AJEntityClass.CV_TYPE_ALL, AJEntityClass.CV_TYPE_SPECIFIC);
        List<String> ENTITY_ALLOWED_VALUES = Arrays.asList(AJEntityClass.CV_ENTITY_ALL,
            AJEntityCollection.FORMAT_TYPE_ASSESSMENT, AJEntityCollection.FORMAT_TYPE_COLLECTION);

        JsonObject result = new JsonObject();
        if (!input.containsKey(AJEntityClass.CV_SCOPE)) {
            return result.put(AJEntityClass.CV_SCOPE, RESOURCE_BUNDLE.getString("missing.field"));
        }

        this.scope = input.getJsonObject(AJEntityClass.CV_SCOPE);
        if (this.scope != null && !this.scope.isEmpty()) {
            if (!this.scope.containsKey(AJEntityClass.CV_TYPE)) {
                return new JsonObject().put(AJEntityClass.CV_TYPE, RESOURCE_BUNDLE.getString("missing.field"));
            }

            this.type = this.scope.getString(AJEntityClass.CV_TYPE);
            if (this.type == null || this.type.isEmpty() || !TYPE_ALLOWED_VALUES.contains(this.type)) {
                return new JsonObject().put(AJEntityClass.CV_TYPE, RESOURCE_BUNDLE.getString("invalid.value"));
            }

            if (this.type.equalsIgnoreCase(AJEntityClass.CV_TYPE_SPECIFIC)) {
                JsonObject errors = new DefaultPayloadValidator().validatePayload(input,
                    AJEntityClass.contentVisibilityFieldSelector(), AJEntityClass.getValidatorRegistry());
                return errors;
            } else if (this.type.equalsIgnoreCase(AJEntityClass.CV_TYPE_ALL)) {
                if (!this.scope.containsKey(AJEntityClass.CV_BOUNDRY)) {
                    return new JsonObject().put(AJEntityClass.CV_BOUNDRY, RESOURCE_BUNDLE.getString("missing.field"));
                }

                this.boundry = this.scope.getJsonObject(AJEntityClass.CV_BOUNDRY);
                if (this.boundry == null || this.boundry.isEmpty()) {
                    return new JsonObject().put(AJEntityClass.CV_BOUNDRY, RESOURCE_BUNDLE.getString("invalid.value"));
                }

                if (!this.boundry.containsKey(AJEntityClass.CV_COURSE_ID)
                    || !this.boundry.containsKey(AJEntityClass.CV_ENTITY)) {
                    return new JsonObject().put("course_id or entity", RESOURCE_BUNDLE.getString("missing.field"));
                }

                this.entity = this.boundry.getString(AJEntityClass.CV_ENTITY);
                if (this.entity == null || this.entity.isEmpty() || !ENTITY_ALLOWED_VALUES.contains(this.entity)) {
                    return new JsonObject().put(AJEntityClass.CV_ENTITY, RESOURCE_BUNDLE.getString("invalid.value"));
                }

                // TODO: validate UUID for CUL ids
                this.courseIdFromRequest = this.boundry.getString(AJEntityClass.CV_COURSE_ID);
                if (this.courseIdFromRequest == null || this.courseIdFromRequest.isEmpty()) {
                    return new JsonObject().put(AJEntityClass.CV_ENTITY, RESOURCE_BUNDLE.getString("invalid.value"));
                }

                this.unitIdFromRequest = this.boundry.getString(AJEntityClass.CV_UNIT_ID);
                this.lessonIdFromRequest = this.boundry.getString(AJEntityClass.CV_LESSON_ID);
            }
        } else {
            result.put(AJEntityClass.CV_SCOPE, RESOURCE_BUNDLE.getString("invalid.value"));
        }
        return result;
    }

    private static class DefaultPayloadValidator implements PayloadValidator {

    }

}
