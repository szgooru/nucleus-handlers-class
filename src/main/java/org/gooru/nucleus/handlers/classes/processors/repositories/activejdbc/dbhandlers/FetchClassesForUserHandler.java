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
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.DBException;
import org.javalite.activejdbc.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Created by ashish on 8/2/16.
 */
class FetchClassesForUserHandler implements DBHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FetchClassesForUserHandler.class);
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
    private final ProcessorContext context;
    private final List<String> classIdList = new ArrayList<>();
    private JsonArray memberClassIdArray;
    private static final String RESPONSE_BUCKET_OWNER = "owner";
    private static final String RESPONSE_BUCKET_COLLABORATOR = "collaborator";
    private static final String RESPONSE_BUCKET_MEMBER = "member";
    private static final String RESPONSE_BUCKET_CLASSES = "classes";
    private static final String RESPONSE_BUCKET_MEMBER_COUNT = "member_count";
    private static final String RESPONSE_BUCKET_TEACHER_DETAILS = "teacher_details";

    FetchClassesForUserHandler(ProcessorContext context) {
        this.context = context;
    }

    @Override
    public ExecutionResult<MessageResponse> checkSanity() {
        if ((context.userId() == null) || context.userId().isEmpty()
            || MessageConstants.MSG_USER_ANONYMOUS.equalsIgnoreCase(context.userId())) {
            LOGGER.warn("Invalid user");
            return new ExecutionResult<>(
                MessageResponseFactory.createForbiddenResponse(RESOURCE_BUNDLE.getString("not.allowed")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
        return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
    }

    @Override
    public ExecutionResult<MessageResponse> validateRequest() {
        // Nothing to validate
        return AuthorizerBuilder.buildFetchClassesForUserAuthorizer(context).authorize(null);
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
        response = populateClassMemberCounts(result);
        if (response.hasFailed()) {
            return response;
        }
        response = populateClassDetails(result);
        if (response.hasFailed()) {
            return response;
        }
        return populateTeacherDetails(result);
    }

    @Override
    public boolean handlerReadOnly() {
        return true;
    }

    private ExecutionResult<MessageResponse> populateOwnedOrCollaboratedClassesId(JsonObject result) {
        try {
            LazyList<AJEntityClass> classes = AJEntityClass.findBySQL(AJEntityClass.FETCH_FOR_OWNER_COLLABORATOR_QUERY,
                context.userId(), context.userId());
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
            return new ExecutionResult<>(
                MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
    }

    private ExecutionResult<MessageResponse> populateMembershipClassesId(JsonObject result) {
        try {
            LazyList<AJClassMember> members =
                AJClassMember.findBySQL(AJClassMember.FETCH_USER_MEMBERSHIP_QUERY, context.userId());
            memberClassIdArray = new JsonArray();
            for (AJClassMember member : members) {
                String classId = member.getString(AJClassMember.CLASS_ID);
                memberClassIdArray.add(classId);
                classIdList.add(classId);
            }
            result.put(RESPONSE_BUCKET_MEMBER, memberClassIdArray);
            return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
        } catch (DBException dbe) {
            LOGGER.warn("Unable to fetch membership classes for user '{}'", context.userId(), dbe);
            return new ExecutionResult<>(
                MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
    }

    private ExecutionResult<MessageResponse> populateClassMemberCounts(JsonObject result) {
        try {
            JsonObject memberCount = new JsonObject();
            List<Map> rs = Base.findAll(AJClassMember.FETCH_MEMBERSHIP_COUNT_FOR_CLASSES,
                Utils.convertListToPostgresArrayStringRepresentation(classIdList));
            rs.forEach(map -> memberCount.put(map.get("class_id").toString(), map.get("count")));
            result.put(RESPONSE_BUCKET_MEMBER_COUNT, memberCount);
            return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
        } catch (DBException dbe) {
            LOGGER.warn("Unable to fetch membership classes for user '{}'", context.userId(), dbe);
            return new ExecutionResult<>(
                MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
    }

    private ExecutionResult<MessageResponse> populateClassDetails(JsonObject result) {
        LazyList<AJEntityClass> classes = AJEntityClass.where(AJEntityClass.FETCH_MULTIPLE_QUERY_FILTER,
            Utils.convertListToPostgresArrayStringRepresentation(classIdList));
        JsonArray classDetails = new JsonArray(
            JsonFormatterBuilder.buildSimpleJsonFormatter(false, AJEntityClass.FETCH_QUERY_FIELD_LIST).toJson(classes));
        result.put(RESPONSE_BUCKET_CLASSES, classDetails);
        return new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
    }

    private ExecutionResult<MessageResponse> populateTeacherDetails(JsonObject result) {
        try {
            LazyList<AJUserDemographic> demographics =
                AJUserDemographic.findBySQL(AJUserDemographic.FETCH_TEACHER_DETAILS_QUERY,
                    Utils.convertListToPostgresArrayStringRepresentation(memberClassIdArray.getList()));
            // update that in the response
            JsonArray teacherDetails = new JsonArray(JsonFormatterBuilder
                .buildSimpleJsonFormatter(false, AJUserDemographic.GET_SUMMARY_QUERY_FIELD_LIST).toJson(demographics));
            result.put(RESPONSE_BUCKET_TEACHER_DETAILS, teacherDetails);
            return new ExecutionResult<>(MessageResponseFactory.createOkayResponse(result),
                ExecutionResult.ExecutionStatus.SUCCESSFUL);

        } catch (DBException dbe) {
            LOGGER.warn("Unable to fetch teacher details for classes for user '{}'", context.userId(), dbe);
            return new ExecutionResult<>(
                MessageResponseFactory.createInternalErrorResponse(RESOURCE_BUNDLE.getString("error.from.store")),
                ExecutionResult.ExecutionStatus.FAILED);
        }
    }
}
