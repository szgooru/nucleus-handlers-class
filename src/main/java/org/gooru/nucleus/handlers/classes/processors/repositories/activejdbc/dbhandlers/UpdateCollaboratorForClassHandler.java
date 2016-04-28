package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.gooru.nucleus.handlers.classes.constants.MessageConstants;
import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.events.EventBuilderFactory;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.Utils;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth.AuthorizerBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJClassMember;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entitybuilders.EntityBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.validators.PayloadValidator;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.javalite.activejdbc.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Created by ashish on 8/2/16.
 */
class UpdateCollaboratorForClassHandler implements DBHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCollaboratorForClassHandler.class);
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
    private static final String COLLABORATORS_REMOVED = "collaborators.removed";
    private static final String COLLABORATORS_ADDED = "collaborators.added";
    private final ProcessorContext context;
    private AJEntityClass entityClass;

    UpdateCollaboratorForClassHandler(ProcessorContext context) {
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
            LOGGER.warn("Anonymous user attempting to add collaborator to class");
            return new ExecutionResult<>(
                MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        // Payload should not be empty
        if (context.request() == null || context.request().isEmpty()) {
            LOGGER.warn("Empty payload supplied to add collaborator to class");
            return new ExecutionResult<>(
                MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("empty.payload")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        // Our validators should certify this
        JsonObject errors = new DefaultPayloadValidator().validatePayload(context.request(),
            AJEntityClass.updateCollaboratorFieldSelector(), AJEntityClass.getValidatorRegistry());
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
        if (!this.entityClass.isCurrentVersion() || this.entityClass.isArchived()) {
            LOGGER.warn("Class '{}' is either archived or not of current version", context.classId());
            return new ExecutionResult<>(
                MessageResponseFactory
                    .createInvalidRequestResponse(RESOURCE_BUNDLE.getString("class.archived.or.incorrect.version")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        // Validate if incoming list of collaborators is not the creator or student of class
        if (collaboratorsAreTeachersOrStudents()) {
            return new ExecutionResult<>(
                MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("existing.member")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        return AuthorizerBuilder.buildUpdateCollaboratorAuthorizer(this.context).authorize(this.entityClass);
    }

    @Override
    public ExecutionResult<MessageResponse> executeRequest() {
        JsonObject diffCollaborators = calculateDiffOfCollaborators();
        this.entityClass.setModifierId(context.userId());
        // Now auto populate is done, we need to setup the converter machinery
        new DefaultAJEntityClassEntityBuilder().build(this.entityClass, context.request(),
            AJEntityClass.getConverterRegistry());

        boolean result = this.entityClass.save();
        if (!result) {
            LOGGER.error("Class with id '{}' failed to save", context.classId());
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
                EventBuilderFactory.getCollaboratorUpdatedEventBuilder(context.classId(), diffCollaborators)),
            ExecutionResult.ExecutionStatus.SUCCESSFUL);
    }

    @Override
    public boolean handlerReadOnly() {
        return false;
    }

    private JsonObject calculateDiffOfCollaborators() {
        JsonObject result = new JsonObject();
        // Find current collaborators
        String currentCollaboratorsAsString = this.entityClass.getString(AJEntityClass.COLLABORATOR);
        JsonArray currentCollaborators;
        currentCollaborators = currentCollaboratorsAsString != null && !currentCollaboratorsAsString.isEmpty()
            ? new JsonArray(currentCollaboratorsAsString) : new JsonArray();
        JsonArray newCollaborators = this.context.request().getJsonArray(AJEntityClass.COLLABORATOR);
        if (currentCollaborators.isEmpty() && !newCollaborators.isEmpty()) {
            // Adding all
            result.put(COLLABORATORS_ADDED, newCollaborators.copy());
            result.put(COLLABORATORS_REMOVED, new JsonArray());
        } else if (!currentCollaborators.isEmpty() && newCollaborators.isEmpty()) {
            // Removing all
            result.put(COLLABORATORS_ADDED, new JsonArray());
            result.put(COLLABORATORS_REMOVED, currentCollaborators.copy());
        } else if (!currentCollaborators.isEmpty() && !newCollaborators.isEmpty()) {
            // Do the diffing
            JsonArray toBeAdded = new JsonArray();
            JsonArray toBeDeleted = currentCollaborators.copy();
            for (Object o : newCollaborators) {
                if (toBeDeleted.contains(o)) {
                    toBeDeleted.remove(o);
                } else {
                    toBeAdded.add(o);
                }
            }
            result.put(COLLABORATORS_ADDED, toBeAdded);
            result.put(COLLABORATORS_REMOVED, toBeDeleted);
        } else {
            // WHAT ????
            LOGGER.warn(
                "Updating collaborator with empty payload when current collaborator is empty for assessment '{}'",
                this.context.classId());
            result.put(COLLABORATORS_ADDED, new JsonArray());
            result.put(COLLABORATORS_REMOVED, new JsonArray());
        }
        return result;
    }

    private boolean collaboratorsAreTeachersOrStudents() {
        String creatorId = this.entityClass.getString(AJEntityClass.CREATOR_ID);
        JsonArray newCollaborators = this.context.request().getJsonArray(AJEntityClass.COLLABORATOR);
        if (newCollaborators.contains(creatorId)) {
            return true;
        }
        List<?> rawCollaborator = newCollaborators.getList();
        List<String> collaborators =
            rawCollaborator.stream().map(Object::toString).collect(Collectors.toList());
        Long countOfStudents = AJClassMember.count(AJClassMember.STUDENT_COUNT_FROM_SET_FILTER, context.classId(),
            Utils.convertListToPostgresArrayStringRepresentation(collaborators));
        return (countOfStudents != null && countOfStudents != 0);
    }

    private static class DefaultPayloadValidator implements PayloadValidator {
    }

    private static class DefaultAJEntityClassEntityBuilder implements EntityBuilder<AJEntityClass> {
    }

}
