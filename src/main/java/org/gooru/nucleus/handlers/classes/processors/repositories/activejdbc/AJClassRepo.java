package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.ClassRepo;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers.DBHandlerBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.transactions.TransactionExecutor;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;

/**
 * Created by ashish on 28/1/16.
 */
class AJClassRepo implements ClassRepo {
  private final ProcessorContext context;

  public AJClassRepo(ProcessorContext context) {
    this.context = context;
  }

  @Override
  public MessageResponse createClass() {
    return TransactionExecutor.executeTransaction(DBHandlerBuilder.buildCreateClassHandler(context));
  }

  @Override
  public MessageResponse updateClass() {
    return TransactionExecutor.executeTransaction(DBHandlerBuilder.buildUpdateClassHandler(context));
  }

  @Override
  public MessageResponse fetchClass() {
    return TransactionExecutor.executeTransaction(DBHandlerBuilder.buildFetchClassHandler(context));
  }

  @Override
  public MessageResponse fetchClassMembers() {
    return TransactionExecutor.executeTransaction(DBHandlerBuilder.buildFetchClassMembersHandler(context));
  }

  @Override
  public MessageResponse fetchClassesForCourse() {
    return TransactionExecutor.executeTransaction(DBHandlerBuilder.buildFetchClassesForCourseHandler(context));
  }

  @Override
  public MessageResponse fetchClassesForUser() {
    return TransactionExecutor.executeTransaction(DBHandlerBuilder.buildFetchClassesForUserHandler(context));
  }

  @Override
  public MessageResponse joinClassByStudent() {
    return TransactionExecutor.executeTransaction(DBHandlerBuilder.buildJoinClassByStudentHandler(context));
  }

  @Override
  public MessageResponse inviteStudentToClass() {
    return TransactionExecutor.executeTransaction(DBHandlerBuilder.buildInviteStudentToClassHandler(context));
  }

  @Override
  public MessageResponse deleteClass() {
    return TransactionExecutor.executeTransaction(DBHandlerBuilder.buildDeleteClassHandler(context));
  }

  @Override
  public MessageResponse associateCourseWithClass() {
    return TransactionExecutor.executeTransaction(DBHandlerBuilder.buildAssociateCourseWithClassHandler(context));
  }

  @Override
  public MessageResponse updateCollaboratorForClass() {
    return TransactionExecutor.executeTransaction(DBHandlerBuilder.buildUpdateCollaboratorForClassHandler(context));
  }
}
