package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import io.vertx.core.json.JsonObject;
import org.gooru.nucleus.handlers.classes.constants.MessageConstants;
import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.events.EventBuilderFactory;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth.AuthorizerBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entitybuilders.EntityBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.validators.PayloadValidator;
import org.gooru.nucleus.handlers.classes.processors.repositories.generators.Generator;
import org.gooru.nucleus.handlers.classes.processors.repositories.generators.GeneratorBuilder;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.javalite.activejdbc.DBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.ResourceBundle;

/**
 * Created by ashish on 28/1/16.
 */
class CreateClassHandler implements DBHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateClassHandler.class);
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
  private static final int RETRY_COUNT_FOR_CODE_GENERATION = 5;
  private final ProcessorContext context;
  private AJEntityClass entityClass;

  CreateClassHandler(ProcessorContext context) {
    this.context = context;
  }

  @Override
  public ExecutionResult<MessageResponse> checkSanity() {
    // The user should not be anonymous
    if (context.userId() == null || context.userId().isEmpty() || context.userId().equalsIgnoreCase(MessageConstants.MSG_USER_ANONYMOUS)) {
      LOGGER.warn("Anonymous or invalid user attempting to create class");
      return new ExecutionResult<>(MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
    // Payload should not be empty
    if (context.request() == null || context.request().isEmpty()) {
      LOGGER.warn("Empty payload supplied to create class");
      return new ExecutionResult<>(MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("empty.payload")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
    // Our validators should certify this
    JsonObject errors =
      new DefaultPayloadValidator().validatePayload(context.request(), AJEntityClass.createFieldSelector(), AJEntityClass.getValidatorRegistry());
    if (errors != null && !errors.isEmpty()) {
      LOGGER.warn("Validation errors for request");
      return new ExecutionResult<>(MessageResponseFactory.createValidationErrorResponse(errors), ExecutionResult.ExecutionStatus.FAILED);
    }
    return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
  }

  @Override
  public ExecutionResult<MessageResponse> validateRequest() {
    // Pass through here, no validations from DB side
    return AuthorizerBuilder.buildCreateClassAuthorizer(this.context).authorize(null);
  }

  @Override
  public ExecutionResult<MessageResponse> executeRequest() {
    this.entityClass = new AJEntityClass();
    if (!populateClassCode()) {
      return new ExecutionResult<>(MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("class.code.generation.failure")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
    autoPopulate();

    boolean result = this.entityClass.save();
    if (!result) {
      LOGGER.error("Class for user '{}' failed to create", context.userId());
      if (this.entityClass.hasErrors()) {
        Map<String, String> map = this.entityClass.errors();
        JsonObject errors = new JsonObject();
        map.forEach(errors::put);
        return new ExecutionResult<>(MessageResponseFactory.createValidationErrorResponse(errors), ExecutionResult.ExecutionStatus.FAILED);
      }
    }
    return new ExecutionResult<>(MessageResponseFactory.createCreatedResponse(this.entityClass.getId().toString(),
      EventBuilderFactory.getCreateClassEventBuilder(this.entityClass.getString(AJEntityClass.ID))), ExecutionResult.ExecutionStatus.SUCCESSFUL);
  }

  @Override
  public boolean handlerReadOnly() {
    return false;
  }

  private void autoPopulate() {
    // Need to populate modifier id, creator id, version. Note that "code" is already done at higher level to enable exception handling
    this.entityClass.setModifierId(this.context.userId());
    this.entityClass.setCreatorId(this.context.userId());
    this.entityClass.setVersion();
    // Now we hydrate model from payload
    new DefaultAJEntityClassBuilder().build(this.entityClass, this.context.request(), AJEntityClass.getConverterRegistry());
  }

  private boolean populateClassCode() {
    Generator<String> generator = GeneratorBuilder.buildClassCodeGenerator();
    String resultCode = null;
    int retries = 0;
    for (retries = 0; retries < RETRY_COUNT_FOR_CODE_GENERATION; retries++) {
      resultCode = generator.generate();
      if (checkUniqueness(resultCode)) {
        LOGGER.info("Class code generation took '{}' retries", retries);
        this.entityClass.set(AJEntityClass.CODE, resultCode);
        return true;
      }
    }
    LOGGER.warn("Not able to generate unique class code for user '{}'", context.userId());
    return false;
  }

  private boolean checkUniqueness(String resultCode) {
    try {
      Long count = AJEntityClass.count(AJEntityClass.CODE_UNIQUENESS_QUERY, resultCode);
      return count == 0;
    } catch (DBException e) {
      // Since this is read only query, there may not be an impact on the connection state (like in integrity constraints violations). So we
      // continue to retry and eat up exception after logging
      LOGGER.error("Error checking unique code for user '{user}' and code '{}'", context.userId(), resultCode, e);
    }
    return false;
  }

  private static class DefaultPayloadValidator implements PayloadValidator {
  }

  private static class DefaultAJEntityClassBuilder implements EntityBuilder<AJEntityClass> {
  }
}
