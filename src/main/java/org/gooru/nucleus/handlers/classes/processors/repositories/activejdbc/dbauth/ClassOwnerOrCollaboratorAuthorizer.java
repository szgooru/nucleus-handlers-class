package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth;

import java.util.ResourceBundle;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;

/**
 * Created by ashish on 8/2/16.
 */
class ClassOwnerOrCollaboratorAuthorizer implements Authorizer<AJEntityClass> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassOwnerAuthorizer.class);
    private final ProcessorContext context;
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");

    ClassOwnerOrCollaboratorAuthorizer(ProcessorContext context) {
        this.context = context;
    }

    @Override
    public ExecutionResult<MessageResponse> authorize(AJEntityClass model) {
        String courseId = model.getString(AJEntityClass.COURSE_ID);
        if (courseId == null || courseId.isEmpty()) {
            LOGGER.warn("Authorization request for class '{}' which is having a course '{}'", context.classId(),
                courseId);
        }
        if (checkOwner(model) || checkCollaborator(model)) {
            return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
        }
        return new ExecutionResult<>(
            MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
            ExecutionResult.ExecutionStatus.FAILED);
    }

    private boolean checkOwner(AJEntityClass model) {
        String creatorId = model.getString(AJEntityClass.CREATOR_ID);
        if (creatorId == null || creatorId.isEmpty() || !creatorId.equalsIgnoreCase(context.userId())) {
            LOGGER.warn("User '{}' is not owner of class '{}'", context.userId(), context.classId());
            return false;
        }
        return true;
    }

    private boolean checkCollaborator(AJEntityClass model) {
        String collaboratorString = model.getString(AJEntityClass.COLLABORATOR);
        if (collaboratorString != null && !collaboratorString.isEmpty()) {
            JsonArray collaborators = new JsonArray(collaboratorString);
            if (collaborators.contains(context.userId())) {
                return true;
            }
        }
        LOGGER.warn("User '{}' is not collaborator of class '{}'", context.userId(), context.classId());
        return false;
    }
}
