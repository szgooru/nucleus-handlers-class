package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Created by ashish on 16/5/16.
 */
@Table("collection")
public class AJEntityCollection extends Model {
    // Instead of stating equals assessment we are saying not equals collection because we need to include both
    // assessment and external assessment here
    public static final String FETCH_VISIBLE_ASSESSMENTS_QUERY =
        "select id from collection where course_id = ? and format != 'collection'::content_container_type and "
            + "is_deleted = false and class_visibility ?? ?";
    // Select both id and type and then in CPU separate them in buckets instead of going to db multiple times
    public static final String FETCH_VISIBLE_ITEMS_QUERY =
        "select id, format from collection where course_id = ? and" + " is_deleted = false and class_visibility ?? ?";

    public static final String FETCH_STATISTICS_QUERY =
        "select course_id, unit_id, lesson_id, format, count(id) from collection where course_id = ? and is_deleted ="
            + " false and class_visibility ?? ? group by course_id, unit_id, lesson_id, format";
    public static final String COLLECTIONS_QUERY_FILTER =
        "course_id = ?::uuid and id = ANY(?::uuid[]) and is_deleted = false and not class_visibility ?? ?";
    public static final String TABLE_COLLECTION = "collection";

    private static final String FORMAT_TYPE = "format";
    private static final String FORMAT_TYPE_COLLECTION = "collection";
    private static final String FORMAT_TYPE_ASSESSMENT = "assessment";
    private static final String FORMAT_TYPE_ASSESSMENT_EXT = "assessment-external";

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
