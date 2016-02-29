package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.gooru.nucleus.handlers.classes.constants.MessageConstants;
import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJClassMember;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.formatter.JsonFormatterBuilder;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.javalite.activejdbc.DBException;
import org.javalite.activejdbc.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by ashish on 8/2/16.
 */
class FetchClassesForUserHandler implements DBHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(FetchClassesForUserHandler.class);
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
  private final ProcessorContext context;
  private final List<String> classIdList = new ArrayList<>();
  private static final String RESPONSE_BUCKET_OWNER = "owner";
  private static final String RESPONSE_BUCKET_COLLABORATOR = "collaborator";
  private static final String RESPONSE_BUCKET_MEMBER = "member";
  private static final String RESPONSE_BUCKET_INVITED = "invited";
  private static final String RESPONSE_BUCKET_CLASSES = "classes";

  FetchClassesForUserHandler(ProcessorContext context) {
    this.context = context;
  }

  @Override
  public ExecutionResult<MessageResponse> checkSanity() {
    if (context.userId() == null || context.userId().isEmpty() || MessageConstants.MSG_USER_ANONYMOUS.equalsIgnoreCase(context.userId())) {
      LOGGER.warn("Invalid user");
      return new ExecutionResult<>(MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
    return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
  }

  @Override
  public ExecutionResult<MessageResponse> validateRequest() {
    // Nothing to validate
    return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
  }

  @Override
  public ExecutionResult<MessageResponse> executeRequest() {
    JsonObject result = new JsonObject();
    ExecutionResult<MessageResponse> response = populateOwnedOrCollaboratedClassesId(result);
    if (response.hasFailed()) {
      return response;
    }
    response = populateMembershipClassesId(result);
    if (response.hasFailed()) {
      return response;
    }
    return populateClassDetails(result);
  }

  @Override
  public boolean handlerReadOnly() {
    return true;
  }

  private ExecutionResult<MessageResponse> populateOwnedOrCollaboratedClassesId(JsonObject result) {
    try {
      LazyList<AJEntityClass> classes = AJEntityClass.findBySQL(AJEntityClass.FETCH_FOR_OWNER_COLLABORATOR_QUERY, context.userId(), context.userId());
      JsonArray ownedClassIds = new JsonArray();
      JsonArray collaboratedClassIds = new JsonArray();
      for (AJEntityClass entityClass : classes) {
        String classId = entityClass.getId().toString();
        String creatorId = entityClass.getString(AJEntityClass.CREATOR_ID);
        if (context.userId().equalsIgnoreCase(creatorId)) {
          ownedClassIds.add(classId);
        } else {
          collaboratedClassIds.add(classId);
        }
        classIdList.add(classId);
      }
      result.put(RESPONSE_BUCKET_OWNER, ownedClassIds);
      result.put(RESPONSE_BUCKET_COLLABORATOR, collaboratedClassIds);
      return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
    } catch (DBException dbe) {
      LOGGER.warn("Unable to fetch owned or collaborated classes for user '{}'", context.userId(), dbe);
      return new ExecutionResult<>(MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
  }

  private ExecutionResult<MessageResponse> populateMembershipClassesId(JsonObject result) {
    try {
      LazyList<AJClassMember> members = AJClassMember.findBySQL(AJClassMember.FETCH_USER_MEMBERSHIP_QUERY, context.userId());
      JsonArray memberClassIdArray = new JsonArray();
      JsonArray invitedClassIdArray = new JsonArray();
      for (AJClassMember member : members) {
        String classId = member.getString(AJClassMember.CLASS_ID);
        if (AJClassMember.CLASS_MEMBER_STATUS_TYPE_INVITED.equalsIgnoreCase(member.getString(AJClassMember.CLASS_MEMBER_STATUS))) {
          invitedClassIdArray.add(classId);
          classIdList.add(classId);
        } else if (AJClassMember.CLASS_MEMBER_STATUS_TYPE_JOINED.equalsIgnoreCase(member.getString(AJClassMember.CLASS_MEMBER_STATUS))) {
          memberClassIdArray.add(classId);
          classIdList.add(classId);
        } else {
          LOGGER.warn("Invalid membership status for class '{}' and user '{}'", classId, context.userId());
        }
      }
      result.put(RESPONSE_BUCKET_MEMBER, memberClassIdArray);
      result.put(RESPONSE_BUCKET_INVITED, invitedClassIdArray);
      return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
    } catch (DBException dbe) {
      LOGGER.warn("Unable to fetch membership classes for user '{}'", context.userId(), dbe);
      return new ExecutionResult<>(MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
  }

  private ExecutionResult<MessageResponse> populateClassDetails(JsonObject result) {
    StringBuilder builder = new StringBuilder(classIdList.size() * context.userId().length() + 16);
    for (String classId : classIdList) {
      builder.append('"').append(classId).append('"');
    }
    LazyList<AJEntityClass> classes = AJEntityClass.where(AJEntityClass.FETCH_MULTIPLE_QUERY_FILTER, listToPostgresArrayString(classIdList));
    JsonArray classDetails =
      new JsonArray(JsonFormatterBuilder.buildSimpleJsonFormatter(false, AJEntityClass.FETCH_QUERY_FIELD_LIST).toJson(classes));
    result.put(RESPONSE_BUCKET_CLASSES, classDetails);
    return new ExecutionResult<>(MessageResponseFactory.createOkayResponse(result), ExecutionResult.ExecutionStatus.SUCCESSFUL);

  }

  private String listToPostgresArrayString(List<String> input) {
    int approxSize = ((input.size() + 1) * 36); // Length of UUID is around 36 chars
    Iterator<String> it = input.iterator();
    if (!it.hasNext()) {
      return "{}";
    }

    StringBuilder sb = new StringBuilder(approxSize);
    sb.append('{');
    for (; ; ) {
      String s = it.next();
      sb.append('"').append(s).append('"');
      if (!it.hasNext()) {
        return sb.append('}').toString();
      }
      sb.append(',');
    }

  }
}
