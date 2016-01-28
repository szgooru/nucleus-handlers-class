package org.gooru.nucleus.handlers.classes.processors.repositories;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.AJClassRepoBuilder;

/**
 * Created by ashish on 28/1/16.
 */
public class RepoBuilder {
  public ClassRepo buildQuestionRepo(ProcessorContext context) {
    return new AJClassRepoBuilder().buildClassRepo(context);
  }
}
