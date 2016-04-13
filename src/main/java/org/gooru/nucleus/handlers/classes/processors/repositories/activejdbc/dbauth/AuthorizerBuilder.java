package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJClassMember;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.responses.ExecutionResult;

/**
 * Created by ashish on 29/1/16.
 */
public final class AuthorizerBuilder {

  private AuthorizerBuilder() {
    throw new AssertionError();
  }

  public static Authorizer<AJEntityClass> buildAssociateCourseWithClassAuthorizer(ProcessorContext context) {
    return new ClassOwnerAndCourseOwnerAuthorizer(context);
  }

  public static Authorizer<AJEntityClass> buildDeleteAuthorizer(ProcessorContext context) {
    return new ClassOwnerAuthorizer(context);
  }

  public static Authorizer<AJEntityClass> buildFetchClassesForCourseAuthorizer(ProcessorContext context) {
    // Course owner should be calling this API
    return new CourseOwnerAuthorizer(context);
  }

  public static Authorizer<AJEntityClass> buildFetchClassesForUserAuthorizer(ProcessorContext context) {
    // As long as session token is valid and user is not anonymous, which is the
    // case as we are, we should be fine
    return model -> new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
  }

  public static Authorizer<AJEntityClass> buildFetchClassAuthorizer(ProcessorContext context) {
    // As long as session token is valid and user is not anonymous, which is the
    // case as we are, we should be fine
    return model -> new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
  }

  public static Authorizer<AJEntityClass> buildFetchClassMembersAuthorizer(ProcessorContext context) {
    // User should be a member (which is either teacher or collaborator or
    // student of that class. The student may have
    // just been invited or (s)he may have joined, we don't care as long as
    // (s)he is there
    return new ClassMemberAuthorizer(context);
  }

  public static Authorizer<AJEntityClass> buildInviteStudentToClassAuthorizer(ProcessorContext context) {
    return new ClassOwnerOrCollaboratorAuthorizer(context);
  }

  public static Authorizer<AJEntityClass> buildRemoveInviteAuthorizer(ProcessorContext context) {
    return new ClassOwnerOrCollaboratorAuthorizer(context);
  }

  public static Authorizer<AJEntityClass> buildRemoveStudentAuthorizer(ProcessorContext context) {
    return new ClassOwnerOrCollaboratorAuthorizer(context);
  }

  public static Authorizer<AJEntityClass> buildJoinClassByStudentAuthorizer(ProcessorContext context, AJClassMember membership) {
    return new OpenClassOrInvitedStudentAuthorizer(context, membership);
  }

  public static Authorizer<AJEntityClass> buildUpdateClassAuthorizer(ProcessorContext context) {
    return new ClassOwnerOrCollaboratorAuthorizer(context);
  }

  public static Authorizer<AJEntityClass> buildUpdateCollaboratorAuthorizer(ProcessorContext context) {
    return new ClassOwnerAuthorizer(context);
  }

  public static Authorizer<AJEntityClass> buildCreateClassAuthorizer(ProcessorContext context) {
    // As long as session token is valid and user is not anonymous, which is the
    // case as we are, we should be fine
    return model -> new ExecutionResult<>(null, ExecutionResult.ExecutionStatus.CONTINUE_PROCESSING);
  }

  public static Authorizer<AJEntityClass> buildContentVisibilityAuthorizer(ProcessorContext context) {
    return new ClassOwnerOrCollaboratorAuthorizer(context);
  }
}
