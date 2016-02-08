package org.gooru.nucleus.handlers.classes.processors.repositories;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.AJClassRepoBuilder;

/**
 * Created by ashish on 28/1/16.
 */
public final class RepoBuilder {

  private RepoBuilder() {
    throw new AssertionError();
  }

  public static ClassRepo buildClassRepo(ProcessorContext context) {
    return AJClassRepoBuilder.buildClassRepo(context);
  }
}
