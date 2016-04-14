package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth;

import java.util.ResourceBundle;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJClassMember;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.javalite.activejdbc.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;

/**
 * Created by ashish on 3/3/16.
 */
class ClassMemberAuthorizer implements Authorizer<AJEntityClass> {
    private final ProcessorContext context;
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassMemberAuthorizer.class);
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");

    public ClassMemberAuthorizer(ProcessorContext context) {
        this.context = context;
    }

    @Override
    public ExecutionResult<MessageResponse> authorize(AJEntityClass model) {
        if (checkOwner(model) || checkCollaborator(model) || checkStudent(model)) {
            return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
        }
        return new ExecutionResult<>(
            MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
            ExecutionResult.ExecutionStatus.FAILED);
    }

    private boolean checkStudent(AJEntityClass model) {
        LazyList<AJClassMember> members = AJClassMember.where(AJClassMember.FETCH_FOR_USER_QUERY_FILTER,
            this.context.classId(), this.context.userId());
        return !members.isEmpty();
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
