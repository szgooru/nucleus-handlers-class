package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities;

import io.vertx.core.json.JsonObject;
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
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String CREATOR_SYSTEM = "creator_system";
    public static final String ROSTER_ID = "roster_id";
    public static final int CURRENT_VERSION = 3;
    public static final String INVITEES = "invitees";

    // Dummy field names for Content Visibility
    public static final String CV_UNITS = "units";
    public static final String CV_LESSONS = "lessons";
    public static final String CV_COLLECTIONS = "collections";
    public static final String CV_ASSESSMENTS = "assessments";
    public static final Set<String> CV_FIELDS =
        new HashSet<>(Arrays.asList(CV_ASSESSMENTS, CV_COLLECTIONS, CV_LESSONS, CV_UNITS));

    public static final String CLASS_SHARING_TYPE_NAME = "class_sharing_type";
    public static final String CLASS_SHARING_TYPE_OPEN = "open";
    public static final String CLASS_SHARING_TYPE_RESTRICTED = "restricted";

    public static final String FETCH_QUERY_FILTER = "id = ?::uuid and is_deleted = false";
    public static final String FETCH_MULTIPLE_QUERY_FILTER = "id = ANY(?::uuid[]) and is_deleted = false";
    public static final String FETCH_FOR_OWNER_COLLABORATOR_QUERY =
        "select id, creator_id from class where (creator_id = ?::uuid or collaborator ?? ? ) and is_deleted = false "
            + "order by created_at desc";
    public static final String FETCH_FOR_COURSE_QUERY_FILTER = "course_id = ?::uuid and is_deleted = false";
    public static final String FETCH_VIA_CODE_FILTER = "code = ? and is_deleted = false";
    public static final String COURSE_ASSOCIATION_FILTER = "id = ?::uuid and is_deleted = false and owner_id = ?::uuid";
    public static final String DELETE_QUERY =
        "select id, creator_id, end_date, course_id, gooru_version, is_archived from class where id = ?::uuid and "
            + "is_deleted = false";
    public static final String CODE_UNIQUENESS_QUERY = "code = ?";

    public static final Set<String> EDITABLE_FIELDS = new HashSet<>(Arrays.asList(TITLE, DESCRIPTION, GREETING, GRADE,
        CLASS_SHARING, COVER_IMAGE, MIN_SCORE, END_DATE, COLLABORATOR));
    public static final Set<String> CREATABLE_FIELDS = new HashSet<>(Arrays.asList(TITLE, DESCRIPTION, GREETING, GRADE,
        CLASS_SHARING, COVER_IMAGE, MIN_SCORE, END_DATE, COLLABORATOR, CREATOR_SYSTEM, ROSTER_ID));
    public static final Set<String> MANDATORY_FIELDS = new HashSet<>(Arrays.asList(TITLE, CLASS_SHARING));
    public static final Set<String> FORBIDDEN_FIELDS = new HashSet<>(
        Arrays.asList(ID, CREATED_AT, UPDATED_AT, CREATOR_ID, MODIFIER_ID, IS_DELETED, GOORU_VERSION, IS_ARCHIVED));
    public static final Set<String> COLLABORATOR_FIELDS = new HashSet<>(Arrays.asList(COLLABORATOR));
    public static final Set<String> INVITE_MANDATORY_FIELDS = new HashSet<>(Arrays.asList(INVITEES));
    public static final Set<String> INVITE_ALLOWED_FIELDS = new HashSet<>(Arrays.asList(INVITEES, CREATOR_SYSTEM));
    public static final List<String> FETCH_QUERY_FIELD_LIST =
        Arrays.asList(ID, CREATOR_ID, TITLE, DESCRIPTION, GREETING, GRADE, CLASS_SHARING, COVER_IMAGE, CODE, MIN_SCORE,
            END_DATE, COURSE_ID, COLLABORATOR, GOORU_VERSION, CONTENT_VISIBILITY, IS_ARCHIVED, CREATED_AT, UPDATED_AT);
    public static final Set<String> JOIN_CLASS_FIELDS = new HashSet<>(Arrays.asList(ROSTER_ID, CREATOR_SYSTEM));

    private static final Map<String, FieldValidator> validatorRegistry;
    private static final Map<String, FieldConverter> converterRegistry;

    static {
        validatorRegistry = initializeValidators();
        converterRegistry = initializeConverters();
    }

    private static Map<String, FieldConverter> initializeConverters() {
        Map<String, FieldConverter> converterMap = new HashMap<>();
        converterMap.put(ID, (fieldValue -> FieldConverter.convertFieldToUuid((String) fieldValue)));
        converterMap.put(GRADE, (FieldConverter::convertFieldToJson));
        converterMap.put(END_DATE,
            (fieldValue -> FieldConverter.convertFieldToDateWithFormat(fieldValue, DateTimeFormatter.ISO_LOCAL_DATE)));
        converterMap.put(CONTENT_VISIBILITY, (FieldConverter::convertFieldToJson));
        converterMap.put(CREATOR_ID, (fieldValue -> FieldConverter.convertFieldToUuid((String) fieldValue)));
        converterMap.put(MODIFIER_ID, (fieldValue -> FieldConverter.convertFieldToUuid((String) fieldValue)));
        converterMap.put(COURSE_ID, (fieldValue -> FieldConverter.convertFieldToUuid((String) fieldValue)));
        converterMap.put(CLASS_SHARING,
            (fieldValue -> FieldConverter.convertFieldToNamedType(fieldValue, CLASS_SHARING_TYPE_NAME)));
        converterMap.put(COLLABORATOR, (FieldConverter::convertFieldToJson));
        return Collections.unmodifiableMap(converterMap);
    }

    private static Map<String, FieldValidator> initializeValidators() {
        Map<String, FieldValidator> validatorMap = new HashMap<>();
        validatorMap.put(ID, (FieldValidator::validateUuid));
        validatorMap.put(TITLE, (value) -> FieldValidator.validateString(value, 5000));
        validatorMap.put(DESCRIPTION, (value) -> FieldValidator.validateStringIfPresent(value, 5000));
        validatorMap.put(GREETING, (value) -> FieldValidator.validateStringIfPresent(value, 5000));
        validatorMap.put(GRADE, FieldValidator::validateJsonArrayIfPresent);
        validatorMap.put(CLASS_SHARING,
            (value) -> ((value != null) && (value instanceof String)
                && (CLASS_SHARING_TYPE_OPEN.equalsIgnoreCase((String) value)
                    || CLASS_SHARING_TYPE_RESTRICTED.equalsIgnoreCase((String) value))));
        validatorMap.put(COVER_IMAGE, (value) -> FieldValidator.validateStringIfPresent(value, 2000));
        validatorMap.put(MIN_SCORE, (FieldValidator::validateInteger));
        validatorMap.put(END_DATE,
            (value -> FieldValidator.validateDateWithFormat(value, DateTimeFormatter.ISO_LOCAL_DATE, false)));
        validatorMap.put(COURSE_ID, (value -> FieldValidator.validateUuidIfPresent((String) value)));
        validatorMap.put(COLLABORATOR,
            (value) -> FieldValidator.validateDeepJsonArrayIfPresent(value, FieldValidator::validateUuid));
        validatorMap.put(CREATOR_SYSTEM, (value) -> FieldValidator.validateStringIfPresent(value, 255));
        validatorMap.put(ROSTER_ID, (value) -> FieldValidator.validateStringIfPresent(value, 512));
        validatorMap.put(INVITEES,
            (value) -> FieldValidator.validateDeepJsonArrayIfPresent(value, FieldValidator::validateEmail));
        validatorMap.put(CV_ASSESSMENTS,
            (value) -> FieldValidator.validateDeepJsonArray(value, FieldValidator::validateUuid));
        validatorMap.put(CV_COLLECTIONS,
            (value) -> FieldValidator.validateDeepJsonArray(value, FieldValidator::validateUuid));
        validatorMap.put(CV_LESSONS,
            (value) -> FieldValidator.validateDeepJsonArray(value, FieldValidator::validateUuid));
        validatorMap.put(CV_UNITS,
            (value) -> FieldValidator.validateDeepJsonArray(value, FieldValidator::validateUuid));
        return Collections.unmodifiableMap(validatorMap);
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
        return new FieldSelector() {
            @Override
            public Set<String> mandatoryFields() {
                return Collections.unmodifiableSet(INVITE_MANDATORY_FIELDS);
            }

            @Override
            public Set<String> allowedFields() {
                return Collections.unmodifiableSet(INVITE_ALLOWED_FIELDS);
            }
        };
    }

    public static FieldSelector joinClassFieldSelector() {
        return () -> Collections.unmodifiableSet(JOIN_CLASS_FIELDS);
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

    public static FieldSelector contentVisibilityFieldSelector() {
        return () -> Collections.unmodifiableSet(CV_FIELDS);
    }

    public static ValidatorRegistry getValidatorRegistry() {
        return new ClassValidationRegistry();
    }

    public static ConverterRegistry getConverterRegistry() {
        return new ClassConverterRegistry();
    }

    public void setContentVisibility(JsonObject visibility) {
        FieldConverter fc = converterRegistry.get(CONTENT_VISIBILITY);
        if (fc != null) {
            this.set(CONTENT_VISIBILITY, fc.convertField(visibility.toString()));
        } else {
            this.set(CONTENT_VISIBILITY, visibility.toString());
        }
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

    public void setCourseId(String courseId) {
        FieldConverter fc = converterRegistry.get(COURSE_ID);
        if (fc != null) {
            this.set(COURSE_ID, fc.convertField(courseId));
        } else {
            this.set(COURSE_ID, courseId);
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
        return getInteger(GOORU_VERSION) == CURRENT_VERSION;
    }

    public boolean isArchived() {
        return getBoolean(IS_ARCHIVED);
    }

    public void setVersion() {
        this.set(GOORU_VERSION, CURRENT_VERSION);
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
