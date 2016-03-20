package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.gooru.nucleus.handlers.classes.constants.MessageConstants;
import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.Utils;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth.AuthorizerBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJClassMember;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJUserDemographic;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.formatter.JsonFormatterBuilder;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponseFactory;
import org.javalite.activejdbc.DBException;
import org.javalite.activejdbc.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by ashish on 8/2/16.
 */
class FetchClassMembersHandler implements DBHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(FetchClassMembersHandler.class);
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
  private final ProcessorContext context;
  private AJEntityClass entityClass;
  private static final String RESPONSE_BUCKET_OWNER = "owner";
  private static final String RESPONSE_BUCKET_COLLABORATOR = "collaborator";
  private static final String RESPONSE_BUCKET_MEMBER = "member";
  private static final String RESPONSE_BUCKET_INVITEES = "invitees";
  private static final String RESPONSE_BUCKET_MEMBER_DETAILS = "details";


  public FetchClassMembersHandler(ProcessorContext context) {
    this.context = context;
  }

  @Override
  public ExecutionResult<MessageResponse> checkSanity() {
    if (context.classId() == null || context.classId().isEmpty()) {
      LOGGER.warn("Missing class");
      return new ExecutionResult<>(MessageResponseFactory.createInvalidRequestResponse(RESOURCE_BUNDLE.getString("missing.class.id")),
        ExecutionResult.ExecutionStatus.FAILED);
    }

    if (context.userId() == null || context.userId().isEmpty() || MessageConstants.MSG_USER_ANONYMOUS.equalsIgnoreCase(context.userId())) {
      LOGGER.warn("Invalid user");
      return new ExecutionResult<>(MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
    return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
  }

  @Override
  public ExecutionResult<MessageResponse> validateRequest() {
    try {
      LazyList<AJEntityClass> classes = AJEntityClass.where(AJEntityClass.FETCH_QUERY_FILTER, context.classId());
      if (classes.isEmpty()) {
        LOGGER.warn("Not able to find class '{}'", this.context.classId());
        return new ExecutionResult<>(MessageResponseFactory.createNotFoundResponse(RESOURCE_BUNDLE.getString("not.found")),
          ExecutionResult.ExecutionStatus.FAILED);
      }
      this.entityClass = classes.get(0);
      return AuthorizerBuilder.buildFetchClassMembersAuthorizer(context).authorize(this.entityClass);
    } catch (DBException e) {
      LOGGER.error("Not able to fetch class from DB", e);
      return new ExecutionResult<>(MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
        ExecutionResult.ExecutionStatus.FAILED);
    }
  }

  @Override
  public ExecutionResult<MessageResponse> executeRequest() {

    JsonObject result = new JsonObject();
    List<String> memberIdList = new ArrayList<>();

    // Fetch all the class members
    LazyList<AJClassMember> members = AJClassMember.where(AJClassMember.FETCH_ALL_QUERY_FILTER, this.context.classId());
    populateOwnerInfo(memberIdList, result);
    populateCollaboratorsInfo(memberIdList, result);
    populateMembersInfo(result, memberIdList, members);
    populateDemographics(memberIdList, result);

    return new ExecutionResult<>(MessageResponseFactory.createOkayResponse(result), ExecutionResult.ExecutionStatus.SUCCESSFUL);
  }

  private void populateDemographics(List<String> memberIdList, JsonObject result) {
    // Now resolve the demographic of members
    LazyList<AJUserDemographic> demographics =
      AJUserDemographic.findBySQL(AJUserDemographic.GET_SUMMARY_QUERY, Utils.convertListToPostgresArrayStringRepresentation(memberIdList));
    // update that in the response
    JsonArray userDemographics =
      new JsonArray(JsonFormatterBuilder.buildSimpleJsonFormatter(false, AJUserDemographic.GET_SUMMARY_QUERY_FIELD_LIST).toJson(demographics));
    result.put(RESPONSE_BUCKET_MEMBER_DETAILS, userDemographics);
  }

  private void populateMembersInfo(JsonObject result, List<String> memberIdList, LazyList<AJClassMember> members) {
    // Update the IDs for members
    if (!members.isEmpty()) {
      JsonArray membersArray = new JsonArray();
      JsonArray invitedArrays = new JsonArray();
      members.forEach(ajClassMember -> {
        final String ajClassMemberIdString = ajClassMember.getString(AJClassMember.USER_ID);
        if (ajClassMemberIdString != null && !ajClassMemberIdString.isEmpty()) {
          memberIdList.add(ajClassMemberIdString);
          membersArray.add(ajClassMemberIdString);
        } else {
          final String ajClassMemberEmailString = ajClassMember.getString(AJClassMember.EMAIL);
          if (ajClassMemberEmailString != null && !ajClassMemberEmailString.isEmpty()) {
            invitedArrays.add(ajClassMemberEmailString);
          }
        }
      });
      result.put(RESPONSE_BUCKET_MEMBER, membersArray);
      result.put(RESPONSE_BUCKET_INVITEES, invitedArrays);
    } else {
      result.put(RESPONSE_BUCKET_MEMBER, new JsonArray());
      result.put(RESPONSE_BUCKET_INVITEES, new JsonArray());
    }
  }

  private void populateCollaboratorsInfo(List<String> memberIdList, JsonObject result) {
    // Update the IDs for collaborator
    String collaboratorString = this.entityClass.getString(AJEntityClass.COLLABORATOR);
    if (collaboratorString != null && !collaboratorString.isEmpty()) {
      JsonArray collaborators = new JsonArray(collaboratorString);
      result.put(RESPONSE_BUCKET_COLLABORATOR, collaborators);
      collaborators.forEach(o -> memberIdList.add(o.toString()));
    } else {
      result.put(RESPONSE_BUCKET_COLLABORATOR, new JsonArray());
    }
  }

  private void populateOwnerInfo(List<String> memberIdList, JsonObject result) {
    // Update IDs for owner
    final String owner = this.entityClass.getString(AJEntityClass.CREATOR_ID);
    result.put(RESPONSE_BUCKET_OWNER, new JsonArray().add(owner));
    memberIdList.add(owner);
  }

  @Override
  public boolean handlerReadOnly() {
    return true;
  }
}
