package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import java.util.ResourceBundle;

import org.gooru.nucleus.handlers.classes.constants.MessageConstants;
import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth.AuthorizerBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhelpers.VisibleContentHelper;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.javalite.activejdbc.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;

/**
 * Created by ashish on 12/5/16.
 */
class VisibleContentHandler implements DBHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisibleContentHandler.class);
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
    private final ProcessorContext context;
    private AJEntityClass entityClass;
    private String courseId;

    public VisibleContentHandler(ProcessorContext context) {
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
        if (context.userId() == null || context.userId().isEmpty() || context.userId()
            .equalsIgnoreCase(MessageConstants.MSG_USER_ANONYMOUS)) {
            LOGGER.warn("Anonymous user attempting to get visibility for course");
            return new ExecutionResult<>(
                MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
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
            return new ExecutionResult<>(MessageResponseFactory
                .createInvalidRequestResponse(RESOURCE_BUNDLE.getString("class.archived.or.incorrect.version")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        // Class should be associated with course
        courseId = this.entityClass.getString(AJEntityClass.COURSE_ID);
        if (courseId == null) {
            LOGGER
                .error("Class '{}' is not assigned to course, hence cannot get content visibility", context.classId());
            return new ExecutionResult<>(
                MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("class.without.course")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        return AuthorizerBuilder.buildVisibleContentAuthorizer(context).authorize(this.entityClass);
    }

    @Override
    public ExecutionResult<MessageResponse> executeRequest() {
        // First check what is content visibility type setting for this class. If all is visible, no need to send any
        // specific id. just tell that everything is visible. If only collections are visible, then fetch and send
        // assessments which are explicitly marked as visible. If everything is hidden, then fetch everything from
        // course and send it back.
        String contentVisibilitySetting = this.entityClass.getContentVisibility();
        JsonObject response = new JsonObject();
        if (contentVisibilitySetting.equalsIgnoreCase(AJEntityClass.CONTENT_VISIBILITY_TYPE_VISIBLE_ALL)) {
            response.put(AJEntityClass.CONTENT_VISIBILITY, AJEntityClass.CONTENT_VISIBILITY_TYPE_VISIBLE_ALL);
        } else if (contentVisibilitySetting
            .equalsIgnoreCase(AJEntityClass.CONTENT_VISIBILITY_TYPE_VISIBLE_COLLECTION)) {
            response.put(AJEntityClass.CONTENT_VISIBILITY, AJEntityClass.CONTENT_VISIBILITY_TYPE_VISIBLE_COLLECTION);
            VisibleContentHelper.populateVisibleAssessments(this.context.classId(), this.courseId, response);
        } else if (contentVisibilitySetting.equalsIgnoreCase(AJEntityClass.CONTENT_VISIBILITY_TYPE_VISIBLE_NONE)) {
            response.put(AJEntityClass.CONTENT_VISIBILITY, AJEntityClass.CONTENT_VISIBILITY_TYPE_VISIBLE_NONE);
            VisibleContentHelper.populateVisibleItems(this.context.classId(), this.courseId, response);
        }
        return new ExecutionResult<>(MessageResponseFactory.createOkayResponse(response),
            ExecutionResult.ExecutionStatus.SUCCESSFUL);
    }

    @Override
    public boolean handlerReadOnly() {
        return true;
    }
}
