package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;

/**
 * Created by ashish on 11/1/16.
 */
public class DBHandlerBuilder {

  public DBHandler buildCreateClassHandler(ProcessorContext context) {
    return new CreateClassHandler(context);
  }

  public DBHandler buildUpdateClassHandler(ProcessorContext context) {
    return new UpdateClassHandler(context);
  }

  public DBHandler buildFetchClassHandler(ProcessorContext context) {
    return new FetchClassHandler(context);
  }
}
