package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities;

import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.converters.ConverterRegistry;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.converters.FieldConverter;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.validators.FieldSelector;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.validators.FieldValidator;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.validators.ValidatorRegistry;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by ashish on 8/2/16.
 */
@Table("class")
public class AJEntityClass extends Model {

  public static final String ID = "id";
  public static final String CREATOR_ID = "creator_id";
  public static final String IS_DELETED = "is_deleted";
  public static final String MODIFIER_ID = "modifier_id";
  public static final String TITLE = "title";
  public static final String DESCRIPTION = "description";
  public static final String GREETING = "greeting";
  public static final String CLASS_SHARING = "class_sharing";
  public static final String COVER_IMAGE = "cover_image";
  public static final String GRADE = "grade";
  public static final String CODE = "code";
  public static final String MIN_SCORE = "min_score";
  public static final String END_DATE = "end_date";
  public static final String GOORU_VERSION = "gooru_version";
  public static final String CONTENT_VISIBILITY = "content_visibility";
  public static final String IS_ARCHIVED = "is_archived";
  public static final String COLLABORATOR = "collaborator";
  public static final String COURSE_ID = "course_id";
  public static final String TABLE_COURSE = "course";
  public static final int CURRENT_VERSION = 3;

  public static final String CLASS_SHARING_TYPE_NAME = "class_sharing_type";
  public static final String CLASS_SHARING_TYPE_OPEN = "open";
  public static final String CLASS_SHARING_TYPE_RESTRICTED = "restricted";

  public static final String FETCH_QUERY_FILTER = "id = ?::uuid and is_deleted = false";
  public static final String DELETE_QUERY =
    "select id, creator_id, end_date, course_id, gooru_version, is_archived from class where id = ?::uuid and is_deleted = false";

  public static final Set<String> EDITABLE_FIELDS = new HashSet<>(
    Arrays.asList("title", "description", "greeting", "grade", "class_sharing", "cover_image", "min_score", "end_time", "collaborator"));
  public static final Set<String> CREATABLE_FIELDS = EDITABLE_FIELDS;
  public static final Set<String> MANDATORY_FIELDS = new HashSet<>(Arrays.asList("title", "class_sharing", "min_score"));
  public static final Set<String> FORBIDDEN_FIELDS =
    new HashSet<>(Arrays.asList("id", "created_at", "updated_at", "creator_id", "modifier_id", "is_deleted", "gooru_version", "is_archived"));
  public static final Set<String> COLLABORATOR_FIELDS = new HashSet<>(Arrays.asList(COLLABORATOR));

  private static final Map<String, FieldValidator> validatorRegistry;
  private static final Map<String, FieldConverter> converterRegistry;


  static {
    validatorRegistry = initializeValidators();
    converterRegistry = initializeConverters();
  }

  private static Map<String, FieldConverter> initializeConverters() {
    Map<String, FieldConverter> converterMap = new HashMap<>();
    converterMap.put(ID, (fieldValue -> FieldConverter.convertFieldToUuid((String) fieldValue)));
    converterMap.put(GRADE, (fieldValue -> FieldConverter.convertFieldToJson(fieldValue.toString())));
    converterMap.put(END_DATE, (fieldValue -> FieldConverter.convertFieldToDateWithFormat(fieldValue.toString(), DateTimeFormatter.ISO_LOCAL_DATE)));
    converterMap.put(CONTENT_VISIBILITY, (fieldValue -> FieldConverter.convertFieldToJson(fieldValue.toString())));
    converterMap.put(CREATOR_ID, (fieldValue -> FieldConverter.convertFieldToUuid((String) fieldValue)));
    converterMap.put(MODIFIER_ID, (fieldValue -> FieldConverter.convertFieldToUuid((String) fieldValue)));
    converterMap.put(COURSE_ID, (fieldValue -> FieldConverter.convertFieldToUuid((String) fieldValue)));
    converterMap.put(CLASS_SHARING, (fieldValue -> FieldConverter.convertFieldToNamedType((String) fieldValue, CLASS_SHARING_TYPE_NAME)));
    converterMap.put(COLLABORATOR, (fieldValue -> FieldConverter.convertFieldToJson(fieldValue.toString())));
    return Collections.unmodifiableMap(converterMap);
  }

  private static Map<String, FieldValidator> initializeValidators() {
    Map<String, FieldValidator> validatorMap = new HashMap<>();
    validatorMap.put(ID, (FieldValidator::validateUuid));
    validatorMap.put(TITLE, (value) -> FieldValidator.validateString(value, 5000));
    validatorMap.put(DESCRIPTION, (value) -> FieldValidator.validateStringIfPresent(value, 5000));
    validatorMap.put(GREETING, (value) -> FieldValidator.validateStringIfPresent(value, 5000));
    validatorMap.put(GRADE, FieldValidator::validateJsonIfPresent);
    validatorMap.put(CLASS_SHARING, (value) -> (value != null && value instanceof String &&
      (CLASS_SHARING_TYPE_OPEN.equalsIgnoreCase((String) value) || CLASS_SHARING_TYPE_RESTRICTED.equalsIgnoreCase((String) value))));
    validatorMap.put(COVER_IMAGE, (value) -> FieldValidator.validateStringIfPresent(value, 2000));
    validatorMap.put(MIN_SCORE, (FieldValidator::validateInteger));
    validatorMap.put(END_DATE, (value -> FieldValidator.validateDateWithFormat(value, DateTimeFormatter.ISO_LOCAL_DATE)));
    validatorMap.put(COURSE_ID, (value -> FieldValidator.validateUuidIfPresent(value.toString())));
    validatorMap.put(COLLABORATOR, (value) -> FieldValidator.validateDeepJsonArrayIfPresent(value, FieldValidator::validateUuid));
    return Collections.unmodifiableMap(validatorMap);
  }


  public static FieldSelector associateCourseFieldSelector() {
    throw new RuntimeException("Not implemented");
  }

  public static FieldSelector createFieldSelector() {
    return new FieldSelector() {
      @Override
      public Set<String> allowedFields() {
        return Collections.unmodifiableSet(CREATABLE_FIELDS);
      }

      @Override
      public Set<String> mandatoryFields() {
        return Collections.unmodifiableSet(MANDATORY_FIELDS);
      }
    };
  }

  public static FieldSelector inviteStudentFieldSelector() {
    throw new RuntimeException("Not implemented");
  }

  public static FieldSelector joinClassFieldSelector() {
    throw new RuntimeException("Not implemented");
  }

  public static FieldSelector updateClassFieldSelector() {
    return () -> Collections.unmodifiableSet(EDITABLE_FIELDS);
  }

  public static FieldSelector updateCollaboratorFieldSelector() {
    return new FieldSelector() {
      @Override
      public Set<String> mandatoryFields() {
        return Collections.unmodifiableSet(COLLABORATOR_FIELDS);
      }

      @Override
      public Set<String> allowedFields() {
        return Collections.unmodifiableSet(COLLABORATOR_FIELDS);
      }
    };
  }


  public static ValidatorRegistry getValidatorRegistry() {
    return new ClassValidationRegistry();
  }

  public static ConverterRegistry getConverterRegistry() {
    return new ClassConverterRegistry();
  }


  public void setModifierId(String modifier) {
    FieldConverter fc = converterRegistry.get(MODIFIER_ID);
    if (fc != null) {
      this.set(MODIFIER_ID, fc.convertField(modifier));
    } else {
      this.set(MODIFIER_ID, modifier);
    }
  }

  public void setCreatorId(String modifier) {
    FieldConverter fc = converterRegistry.get(CREATOR_ID);
    if (fc != null) {
      this.set(CREATOR_ID, fc.convertField(modifier));
    } else {
      this.set(CREATOR_ID, modifier);
    }
  }


  public void setIdWithConverter(String id) {
    FieldConverter fc = converterRegistry.get(ID);
    if (fc != null) {
      this.set(ID, fc.convertField(id));
    } else {
      this.set(ID, id);
    }
  }

  public boolean isCurrentVersion() {
    return this.getInteger(GOORU_VERSION) == CURRENT_VERSION;
  }

  public boolean isArchived() {
    return this.getBoolean(IS_ARCHIVED);
  }

  private static class ClassValidationRegistry implements ValidatorRegistry {
    @Override
    public FieldValidator lookupValidator(String fieldName) {
      return validatorRegistry.get(fieldName);
    }
  }

  private static class ClassConverterRegistry implements ConverterRegistry {
    @Override
    public FieldConverter lookupConverter(String fieldName) {
      return converterRegistry.get(fieldName);
    }
  }

}
