package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import java.util.Map;
import java.util.ResourceBundle;

import org.gooru.nucleus.handlers.classes.constants.MessageConstants;
import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.events.EventBuilderFactory;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth.AuthorizerBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhelpers.ContentVisibilityHelper;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.validators.PayloadValidator;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.javalite.activejdbc.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;

/**
 * Created by ashish on 21/3/16.
 */
public class ContentVisibilityHandler implements DBHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentVisibilityHandler.class);
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
    private final ProcessorContext context;
    private AJEntityClass entityClass;

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
        JsonObject errors = new DefaultPayloadValidator().validatePayload(context.request(),
            AJEntityClass.contentVisibilityFieldSelector(), AJEntityClass.getValidatorRegistry());
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
        // Check authorization
        ExecutionResult<MessageResponse> result =
            AuthorizerBuilder.buildContentVisibilityAuthorizer(this.context).authorize(entityClass);
        if (result.hasFailed()) {
            return result;
        }
        // Class should be associated with course
        String courseId = this.entityClass.getString(AJEntityClass.COURSE_ID);
        if (courseId == null) {
            LOGGER.error("Class '{}' is not assigned to course, hence cannot set content visibility",
                context.classId());
            return new ExecutionResult<>(
                MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("class.without.course")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        // Now validate the payload with DB
        return ContentVisibilityHelper.validatePayloadWithDB(context.request(), courseId);
    }

    @Override
    public ExecutionResult<MessageResponse> executeRequest() {
        // Merge the DB provided visibility with what request provides
        String dbVisibility = this.entityClass.getString(AJEntityClass.CONTENT_VISIBILITY);
        JsonObject finalVisibility = ContentVisibilityHelper.updateContentVisibility(context.request(), dbVisibility);
        this.entityClass.setContentVisibility(finalVisibility);
        this.entityClass.setModifierId(this.context.userId());
        boolean result = this.entityClass.save();
        if (!result) {
            LOGGER.error("Class with id '{}' failed to save content visibility", context.classId());
            if (this.entityClass.hasErrors()) {
                Map<String, String> map = this.entityClass.errors();
                JsonObject errors = new JsonObject();
                map.forEach(errors::put);
                return new ExecutionResult<>(MessageResponseFactory.createValidationErrorResponse(errors),
                    ExecutionResult.ExecutionStatus.FAILED);
            }
        }
        return new ExecutionResult<>(
            MessageResponseFactory.createNoContentResponse(RESOURCE_BUNDLE.getString("updated"),
                EventBuilderFactory.getContentVisibleEventBuilder(context.classId(), context.request())),
            ExecutionResult.ExecutionStatus.SUCCESSFUL);

    }

    @Override
    public boolean handlerReadOnly() {
        return false;
    }

    private static class DefaultPayloadValidator implements PayloadValidator {
    }

}
