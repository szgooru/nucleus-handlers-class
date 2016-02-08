package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities;

import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.validators.FieldSelector;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.validators.ValidatorRegistry;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Created by ashish on 8/2/16.
 */
@Table("class")
public class AJEntityClass extends Model {
  public static FieldSelector associateCourseFieldSelector() {
    throw new RuntimeException("Not implemented");
  }

  public static ValidatorRegistry getValidatorRegistry() {
    throw new RuntimeException("Not implemented");
  }

  public static FieldSelector createFieldSelector() {
    throw new RuntimeException("Not implemented");
  }

  public static FieldSelector inviteStudentFieldSelector() {
    throw new RuntimeException("Not implemented");
  }

  public static FieldSelector joinClassFieldSelector() {
    throw new RuntimeException("Not implemented");
  }

  public static FieldSelector updateClassFieldSelector() {
    throw new RuntimeException("Not implemented");
  }

  public static FieldSelector updateCollaboratorFieldSelector() {
    throw new RuntimeException("Not implemented");
  }
}
