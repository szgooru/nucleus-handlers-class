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

import java.util.ResourceBundle;
import java.util.UUID;

class MessageProcessor implements Processor {

  private static final Logger LOGGER = LoggerFactory.getLogger(Processor.class);
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
  private final Message<Object> message;
  private String userId;
  private JsonObject prefs;
  private JsonObject request;
  private String studentId;
  private String courseId;


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
        case MessageConstants.MSG_OP_CLASS_COLLABORATORS_UPDATE:
          result = updateCollaboratorForClass();
          break;
        case MessageConstants.MSG_OP_CLASS_COURSE_ASSOCIATION:
          result = associateCourseWithClass();
          break;
        case MessageConstants.MSG_OP_CLASS_DELETE:
          result = deleteClass();
          break;
        case MessageConstants.MSG_OP_CLASS_INVITE:
          result = inviteStudentToClass();
          break;
        case MessageConstants.MSG_OP_CLASS_JOIN:
          result = joinClassByStudent();
          break;
        case MessageConstants.MSG_OP_CLASS_LIST:
          result = listClassesForUser();
          break;
        case MessageConstants.MSG_OP_CLASS_LIST_FOR_COURSE:
          result = listClassesForCourse();
          break;
        case MessageConstants.MSG_OP_CLASS_MEMBERS_GET:
          result = listClassMembers();
          break;
        default:
          LOGGER.error("Invalid operation type passed in, not able to handle");
          return MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.operation"));
      }
      return result;
    } catch (Throwable e) {
      LOGGER.error("Unhandled exception in processing", e);
      return MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("unexpected.error"));
    }
  }

  private MessageResponse listClassMembers() {
    ProcessorContext context = createContext();
    if (!validateContext(context)) {
      return MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.class"));
    }
    return RepoBuilder.buildClassRepo(context).fetchClassMembers();
  }

  private MessageResponse listClassesForCourse() {
    ProcessorContext context = createContextWithCourse();
    if (!validateContextOnlyCourse(context)) {
      return MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.class.or.course"));
    }
    return RepoBuilder.buildClassRepo(context).fetchClassesForCourse();
  }

  private MessageResponse listClassesForUser() {
    ProcessorContext context = createContext();
    return RepoBuilder.buildClassRepo(context).fetchClassesForUser();
  }

  private MessageResponse joinClassByStudent() {
    ProcessorContext context = createContext();
    if (!validateContext(context)) {
      return MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.class"));
    }
    return RepoBuilder.buildClassRepo(context).joinClassByStudent();
  }

  private MessageResponse inviteStudentToClass() {
    ProcessorContext context = createContext();
    if (!validateContext(context)) {
      return MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.class"));
    }
    return RepoBuilder.buildClassRepo(context).inviteStudentToClass();
  }

  private MessageResponse deleteClass() {
    ProcessorContext context = createContext();
    if (!validateContext(context)) {
      return MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.class"));
    }
    return RepoBuilder.buildClassRepo(context).deleteClass();
  }

  private MessageResponse associateCourseWithClass() {
    ProcessorContext context = createContextWithCourse();
    if (!validateContextWithCourse(context)) {
      return MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.class.or.course"));
    }
    return RepoBuilder.buildClassRepo(context).associateCourseWithClass();
  }

  private MessageResponse updateCollaboratorForClass() {
    ProcessorContext context = createContext();
    if (!validateContext(context)) {
      return MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.class"));
    }
    return RepoBuilder.buildClassRepo(context).updateCollaboratorForClass();
  }

  private MessageResponse processClassUpdate() {
    ProcessorContext context = createContext();
    if (!validateContext(context)) {
      return MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.class"));
    }
    return RepoBuilder.buildClassRepo(context).updateClass();
  }

  private MessageResponse processClassGet() {
    ProcessorContext context = createContext();
    if (!validateContext(context)) {
      return MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.class"));
    }
    return RepoBuilder.buildClassRepo(context).fetchClass();
  }

  private MessageResponse processClassCreate() {
    ProcessorContext context = createContext();
    return RepoBuilder.buildClassRepo(context).createClass();
  }


  private ProcessorContext createContext() {
    String classId = message.headers().get(MessageConstants.CLASS_ID);
    String classCode = message.headers().get(MessageConstants.CLASS_CODE);
    return new ProcessorContext.ProcessorContextBuilder(userId, prefs, request, classId, classCode).build();
  }

  private ProcessorContext createContextWithCourse() {
    String classId = message.headers().get(MessageConstants.CLASS_ID);
    String courseId = message.headers().get(MessageConstants.COURSE_ID);
    String classCode = message.headers().get(MessageConstants.CLASS_CODE);
    return new ProcessorContext.ProcessorContextBuilder(userId, prefs, request, classId, classCode).setCourseId(courseId).build();

  }


  private boolean validateContextWithCourse(ProcessorContext context) {
    return validateContextOnlyCourse(context) && validateContext(context);
  }

  private boolean validateContextOnlyCourse(ProcessorContext context) {
    if (!validateId(context.courseId())) {
      LOGGER.error("Invalid request, course id not available/incorrect format. Aborting");
      return false;
    }
    return true;
  }

  private boolean validateContext(ProcessorContext context) {
    if (!validateId(context.classId())) {
      LOGGER.error("Invalid request, class id not available/incorrect format. Aborting");
      return false;
    }
    return true;
  }

  private ExecutionResult<MessageResponse> validateAndInitialize() {
    if (message == null || !(message.body() instanceof JsonObject)) {
      LOGGER.error("Invalid message received, either null or body of message is not JsonObject ");
      return new ExecutionResult<>(MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.payload")),
        ExecutionResult.ExecutionStatus.FAILED);
    }

    userId = ((JsonObject) message.body()).getString(MessageConstants.MSG_USER_ID);
    if (!validateUser(userId)) {
      LOGGER.error("Invalid user id passed. Not authorized.");
      return new ExecutionResult<>(MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("invalid.user")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
    prefs = ((JsonObject) message.body()).getJsonObject(MessageConstants.MSG_KEY_PREFS);
    request = ((JsonObject) message.body()).getJsonObject(MessageConstants.MSG_HTTP_BODY);

    if (prefs == null || prefs.isEmpty()) {
      LOGGER.error("Invalid preferences obtained, probably not authorized properly");
      return new ExecutionResult<>(MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("invalid.preferences")),
        ExecutionResult.ExecutionStatus.FAILED);
    }

    if (request == null) {
      LOGGER.error("Invalid JSON payload on Message Bus");
      return new ExecutionResult<>(MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("invalid.payload")),
        ExecutionResult.ExecutionStatus.FAILED);
    }

    // All is well, continue processing
    return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
  }

  private boolean validateUser(String userId) {
    return !(userId == null || userId.isEmpty()) && (userId.equalsIgnoreCase(MessageConstants.MSG_USER_ANONYMOUS) || validateUuid(userId));
  }

  private boolean validateId(String id) {
    return !(id == null || id.isEmpty()) && validateUuid(id);
  }

  private boolean validateUuid(String uuidString) {
    try {
      UUID uuid = UUID.fromString(uuidString);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    } catch (Exception e) {
      return false;
    }
  }
}
