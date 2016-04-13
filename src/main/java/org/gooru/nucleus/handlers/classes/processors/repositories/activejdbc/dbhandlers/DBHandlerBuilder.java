package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;

/**
 * Created by ashish on 11/1/16.
 */
public final class DBHandlerBuilder {

  private DBHandlerBuilder() {
    throw new AssertionError();
  }

  public static DBHandler buildCreateClassHandler(ProcessorContext context) {
    return new CreateClassHandler(context);
  }

  public static DBHandler buildUpdateClassHandler(ProcessorContext context) {
    return new UpdateClassHandler(context);
  }

  public static DBHandler buildFetchClassHandler(ProcessorContext context) {
    return new FetchClassHandler(context);
  }

  public static DBHandler buildFetchClassMembersHandler(ProcessorContext context) {
    return new FetchClassMembersHandler(context);
  }

  public static DBHandler buildFetchClassesForCourseHandler(ProcessorContext context) {
    return new FetchClassesForCourseHandler(context);
  }

  public static DBHandler buildFetchClassesForUserHandler(ProcessorContext context) {
    return new FetchClassesForUserHandler(context);
  }

  public static DBHandler buildJoinClassByStudentHandler(ProcessorContext context) {
    return new JoinClassByStudentHandler(context);
  }

  public static DBHandler buildInviteStudentToClassHandler(ProcessorContext context) {
    return new InviteStudentToClassHandler(context);
  }

  public static DBHandler buildDeleteClassHandler(ProcessorContext context) {
    return new DeleteClassHandler(context);
  }

  public static DBHandler buildAssociateCourseWithClassHandler(ProcessorContext context) {
    return new AssociateCourseWithClassHandler(context);
  }

  public static DBHandler buildUpdateCollaboratorForClassHandler(ProcessorContext context) {
    return new UpdateCollaboratorForClassHandler(context);
  }

  public static DBHandler buildSetContentVisibilityHandler(ProcessorContext context) {
    return new ContentVisibilityHandler(context);
  }

  public static DBHandler buildRemoveInviteHandler(ProcessorContext context) {
    return new RemoveInviteHandler(context);
  }

  public static DBHandler buildRemoveStudentHandler(ProcessorContext context) {
    return new RemoveStudentHandler(context);
  }
}
