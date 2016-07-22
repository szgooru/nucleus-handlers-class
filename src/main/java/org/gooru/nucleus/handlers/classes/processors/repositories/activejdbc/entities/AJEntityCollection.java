package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Created by ashish on 16/5/16.
 */
@Table("collection")
public class AJEntityCollection extends Model {

    public static final String ID = "id";
    public static final String COURSE_ID = "course_id";
    public static final String UNIT_ID = "unit_id";
    public static final String LESSON_ID = "lesson_id";
    public static final String CA_COUNT = "count";
    public static final String ASSESSMENT_COUNT = "assessment_count";
    public static final String COLLECTION_COUNT = "collection_count";
    public static final String COURSE = "course";
    public static final String UNITS = "units";
    public static final String LESSONS = "lessons";

    // Instead of stating equals assessment we are saying not equals collection because we need to include both
    // assessment and external assessment here
    public static final String FETCH_VISIBLE_ASSESSMENTS_QUERY =
        "select id, course_id, unit_id, lesson_id from collection where course_id = ?::uuid and format != 'collection'::content_container_type and "
            + "is_deleted = false and class_visibility ?? ? group by course_id, unit_id, lesson_id, id";
    // Select both id and type and then in CPU separate them in buckets instead of going to db multiple times
    public static final String FETCH_VISIBLE_ITEMS_QUERY =
        "select id, course_id, unit_id, lesson_id, format from collection where course_id = ?::uuid and"
            + " is_deleted = false and class_visibility ?? ? group by course_id, unit_id, lesson_id, format, id";

    public static final String FETCH_STATISTICS_QUERY =
        "select course_id, unit_id, lesson_id, format, count(id) from collection where course_id = ?::uuid and "
            + "is_deleted = false and class_visibility ?? ? group by course_id, unit_id, lesson_id, format";
    public static final String COLLECTIONS_QUERY_FILTER =
        "course_id = ?::uuid and id = ANY(?::uuid[]) and is_deleted = false and (not class_visibility ?? ? or "
            + "class_visibility is null)";
    public static final String TABLE_COLLECTION = "collection";

    public static final String FORMAT_TYPE = "format";
    public static final String FORMAT_TYPE_COLLECTION = "collection";
    public static final String FORMAT_TYPE_ASSESSMENT = "assessment";
    public static final String FORMAT_TYPE_ASSESSMENT_EXT = "assessment-external";
    public static final String VISIBILITY_DML = "update collection set class_visibility = class_visibility || "
        + "?::jsonb where course_id = ?::uuid and is_deleted = false and id = ANY(?::uuid[])";

    // The model needs to be hydrated with format, else it may fail
    public boolean isAssessment() {
        return FORMAT_TYPE_ASSESSMENT.equalsIgnoreCase(this.getString(FORMAT_TYPE));
    }

    // The model needs to be hydrated with format, else it may fail
    public boolean isCollection() {
        return FORMAT_TYPE_COLLECTION.equalsIgnoreCase(this.getString(FORMAT_TYPE));
    }

    // The model needs to be hydrated with format, else it may fail
    public boolean isAssessmentExternal() {
        return FORMAT_TYPE_ASSESSMENT_EXT.equalsIgnoreCase(this.getString(FORMAT_TYPE));
    }

}
