package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhandlers;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;

/**
 * Created by ashish on 11/1/16.
 */
public final class DBHandlerBuilder {

  public static DBHandler buildCreateClassHandler(ProcessorContext context) {
    return new CreateClassHandler(context);
  }

  public static DBHandler buildUpdateClassHandler(ProcessorContext context) {
    return new UpdateClassHandler(context);
  }

  public static DBHandler buildFetchClassHandler(ProcessorContext context) {
    return new FetchClassHandler(context);
  }

  private DBHandlerBuilder() {
    throw new AssertionError();
  }
}
