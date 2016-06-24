package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import java.sql.PreparedStatement;
import java.util.ResourceBundle;

import org.gooru.nucleus.handlers.classes.constants.MessageConstants;
import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.events.EventBuilderFactory;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.Utils;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth.AuthorizerBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJClassMember;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
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
 * Created by ashish on 8/2/16.
 */
class InviteStudentToClassHandler implements DBHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InviteStudentToClassHandler.class);
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
    private final ProcessorContext context;

    InviteStudentToClassHandler(ProcessorContext context) {
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
            LOGGER.warn("Anonymous user attempting to invite student to class");
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
        JsonObject errors = new DefaultPayloadValidator()
            .validatePayload(context.request(), AJEntityClass.inviteStudentFieldSelector(),
                AJEntityClass.getValidatorRegistry());
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
        AJEntityClass entityClass = classes.get(0);
        // Class should be of current version and Class should not be archived
        if (!entityClass.isCurrentVersion() || entityClass.isArchived()) {
            LOGGER.warn("Class '{}' is either archived or not of current version", context.classId());
            return new ExecutionResult<>(MessageResponseFactory
                .createInvalidRequestResponse(RESOURCE_BUNDLE.getString("class.archived.or.incorrect.version")),
                ExecutionResult.ExecutionStatus.FAILED);
        }

        return AuthorizerBuilder.buildInviteStudentToClassAuthorizer(this.context).authorize(entityClass);
    }

    @Override
    public ExecutionResult<MessageResponse> executeRequest() {
        String creatorSystem = this.context.request().getString(AJClassMember.CREATOR_SYSTEM);
        JsonArray invitees = this.context.request().getJsonArray(AJEntityClass.INVITEES);
        JsonArray prunedInvitees = pruneAlreadyInvitedUsers(invitees);

        if (prunedInvitees.isEmpty()) {
            // Nothing to do
            return new ExecutionResult<>(
                MessageResponseFactory.createNoContentResponse(RESOURCE_BUNDLE.getString("invited")),
                ExecutionResult.ExecutionStatus.SUCCESSFUL);
        }
        return saveInvitations(creatorSystem, prunedInvitees);
    }

    @Override
    public boolean handlerReadOnly() {
        return false;
    }

    private JsonArray pruneAlreadyInvitedUsers(JsonArray invitees) {
        JsonArray result = new JsonArray();
        LazyList<AJClassMember> memberships = AJClassMember
            .where(AJClassMember.FETCH_FOR_MULTIPLE_EMAILS_QUERY_FILTER, context.classId(),
                Utils.convertListToPostgresArrayStringRepresentation(invitees.getList()));
        if (memberships.isEmpty()) {
            // Add everything
            return result.addAll(invitees);
        } else if (memberships.size() == invitees.size()) {
            // Nothing to add, all present already
            return result;
        } else {
            result.addAll(invitees);
            for (AJClassMember member : memberships) {
                result.remove(member.getString(AJClassMember.EMAIL));
            }
        }
        return result;
    }

    private ExecutionResult<MessageResponse> saveInvitations(String creatorSystem, JsonArray invitees) {
        try {
            PreparedStatement ps = Base.startBatch(AJClassMember.INVITE_STUDENT_QUERY);
            for (Object invitee : invitees) {
                Base.addBatch(ps, this.context.classId(), invitee.toString(),
                    AJClassMember.CLASS_MEMBER_STATUS_TYPE_INVITED, creatorSystem);
            }
            Base.executeBatch(ps);
            return new ExecutionResult<>(MessageResponseFactory
                .createNoContentResponse(RESOURCE_BUNDLE.getString("invited"),
                    EventBuilderFactory.getStudentInvitedEventBuilder(context.classId(), invitees)),
                ExecutionResult.ExecutionStatus.SUCCESSFUL);

        } catch (DBException dbe) {
            LOGGER.error("Error trying to save invitations", dbe);
            return new ExecutionResult<>(
                MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
    }

    private static class DefaultPayloadValidator implements PayloadValidator {
    }
}
