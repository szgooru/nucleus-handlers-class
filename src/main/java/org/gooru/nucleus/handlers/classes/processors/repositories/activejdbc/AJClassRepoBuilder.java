package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc;

import org.gooru.nucleus.handlers.classes.processors.ProcessorContext;
import org.gooru.nucleus.handlers.classes.processors.repositories.ClassRepo;

/**
 * Created by ashish on 28/1/16.
 */
public class AJClassRepoBuilder {
  public ClassRepo buildClassRepo(ProcessorContext context) {
    return new AJClassRepo(context);
  }
}
