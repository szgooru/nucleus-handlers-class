package org.gooru.nucleus.handlers.classes.processors;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.gooru.nucleus.handlers.classes.constants.MessageConstants;
import org.gooru.nucleus.handlers.classes.processors.repositories.RepoBuilder;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MessageProcessor implements Processor {

  private static final Logger LOGGER = LoggerFactory.getLogger(Processor.class);
  private String userId;
  private JsonObject prefs;
  private JsonObject request;
  private final Message<Object> message;

  public MessageProcessor(Message<Object> message) {
    this.message = message;
  }

  @Override
  public MessageResponse process() {

    MessageResponse result;
    try {
      // Validate the message itself
      ExecutionResult<MessageResponse> validateResult = validateAndInitialize();
      if (validateResult.isCompleted()) {
        return validateResult.result();
      }

      final String msgOp = message.headers().get(MessageConstants.MSG_HEADER_OP);
      switch (msgOp) {
        case MessageConstants.MSG_OP_CLASS_CREATE:
          result = processClassCreate();
          break;
        case MessageConstants.MSG_OP_CLASS_GET:
          result = processClassGet();
          break;
        case MessageConstants.MSG_OP_CLASS_UPDATE:
          result = processClassUpdate();
          break;
        default:
          LOGGER.error("Invalid operation type passed in, not able to handle");
          return MessageResponseFactory.createInvalidRequestResponse("Invalid operation");
      }
      return result;
    } catch (Throwable e) {
      LOGGER.error("Unhandled exception in processing", e);
      return MessageResponseFactory.createInternalErrorResponse();
    }
  }

  private MessageResponse processClassUpdate() {
    ProcessorContext context = createContext();
    if (context.classId() == null || context.classId().isEmpty()) {
      LOGGER.error("Invalid request, class id not available. Aborting");
      return MessageResponseFactory.createInvalidRequestResponse("Invalid class id");
    }
    return RepoBuilder.buildClassRepo(context).updateClass();
  }

  private MessageResponse processClassGet() {
    ProcessorContext context = createContext();
    if (context.classId() == null || context.classId().isEmpty()) {
      LOGGER.error("Invalid request, class id not available. Aborting");
      return MessageResponseFactory.createInvalidRequestResponse("Invalid class id");
    }
    return RepoBuilder.buildClassRepo(context).fetchClass();
  }

  private MessageResponse processClassCreate() {
    ProcessorContext context = createContext();

    return RepoBuilder.buildClassRepo(context).createClass();
  }


  private ProcessorContext createContext() {
    String classId = message.headers().get(MessageConstants.CLASS_ID);

    return new ProcessorContext(userId, prefs, request, classId);
  }

  private ExecutionResult<MessageResponse> validateAndInitialize() {
    if (message == null || !(message.body() instanceof JsonObject)) {
      LOGGER.error("Invalid message received, either null or body of message is not JsonObject ");
      return new ExecutionResult<>(MessageResponseFactory.createInvalidRequestResponse(), ExecutionResult.ExecutionStatus.FAILED);
    }

    userId = ((JsonObject) message.body()).getString(MessageConstants.MSG_USER_ID);
    if (userId == null) {
      LOGGER.error("Invalid user id passed. Not authorized.");
      return new ExecutionResult<>(MessageResponseFactory.createForbiddenResponse(), ExecutionResult.ExecutionStatus.FAILED);
    }
    prefs = ((JsonObject) message.body()).getJsonObject(MessageConstants.MSG_KEY_PREFS);
    request = ((JsonObject) message.body()).getJsonObject(MessageConstants.MSG_HTTP_BODY);

    if (prefs == null || prefs.isEmpty()) {
      LOGGER.error("Invalid preferences obtained, probably not authorized properly");
      return new ExecutionResult<>(MessageResponseFactory.createForbiddenResponse(), ExecutionResult.ExecutionStatus.FAILED);
    }

    if (request == null) {
      LOGGER.error("Invalid JSON payload on Message Bus");
      return new ExecutionResult<>(MessageResponseFactory.createInvalidRequestResponse(), ExecutionResult.ExecutionStatus.FAILED);
    }

    // All is well, continue processing
    return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
  }

}
