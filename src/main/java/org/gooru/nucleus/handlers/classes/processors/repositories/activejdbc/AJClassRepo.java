package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.ClassRepo;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers.DBHandlerBuilder;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.transactions.TransactionExecutor;
import org.gooru.nucleus.handlers.classes.processors.responses.MessageResponse;

/**
 * Created by ashish on 28/1/16.
 */
public class AJClassRepo implements ClassRepo {
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
}
