package org.gooru.nucleus.handlers.classes.processors.repositories.generators;

/**
 * Created by ashish on 9/2/16.
 */
public final class GeneratorBuilder {

  public static Generator<String> buildClassCodeGenerator() {
    return new ClassCodeGenerator();
  }

  private GeneratorBuilder() {
    throw new AssertionError();
  }
}
