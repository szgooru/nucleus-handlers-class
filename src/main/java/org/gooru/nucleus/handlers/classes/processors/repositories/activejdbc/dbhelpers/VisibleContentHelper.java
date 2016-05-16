package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhelpers;

import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityCollection;
import org.javalite.activejdbc.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Helper class to provide utility functions for getting visible contents and associated statistics
 * Created by ashish on 16/5/16.
 */
public final class VisibleContentHelper {
    private VisibleContentHelper() {
        throw new AssertionError();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(VisibleContentHelper.class);

    /**
     * Based on the provided class Id and courseId, find out all the assessments that are visible for that class.
     * This includes both assessments and external assessments
     *
     * @param classId
     * @param courseId
     * @param result   JsonbObject which will be populated with all the assessments id which are visible in that class
     */
    public static void populateVisibleAssessments(String classId, String courseId, JsonObject result) {
        LazyList<AJEntityCollection> assessments =
            AJEntityCollection.findBySQL(AJEntityCollection.FETCH_VISIBLE_ASSESSMENTS_QUERY, courseId, classId);
        JsonArray idArray = new JsonArray();
        for (AJEntityCollection assessment : assessments) {
            idArray.add(assessment.getId().toString());
        }
        result.put(AJEntityClass.CV_ASSESSMENTS, idArray);
    }

    /**
     * Based on the provided class Id and courseId, find out all the assessments/collections that are visible for that
     * class. This includes both assessments and external assessments
     *
     * @param classId
     * @param courseId
     * @param result   JsonObject which will get populated with all assessments/collections which are visible
     */
    public static void populateVisibleItems(String classId, String courseId, JsonObject result) {
        LazyList<AJEntityCollection> items =
            AJEntityCollection.findBySQL(AJEntityCollection.FETCH_VISIBLE_ITEMS_QUERY, courseId, classId);
        JsonArray collections = new JsonArray();
        JsonArray assessments = new JsonArray();
        String id;
        for (AJEntityCollection item : items) {
            id = item.getId().toString();
            if (item.isAssessment() || item.isAssessmentExternal()) {
                assessments.add(id);
            } else if (item.isCollection()) {
                collections.add(id);
            } else {
                LOGGER.warn("Invalid format for collection/assessment id {}", id);
            }
        }
        result.put(AJEntityClass.CV_ASSESSMENTS, assessments);
        result.put(AJEntityClass.CV_COLLECTIONS, collections);
    }

    /**
     * Find the needed statistics
     * It queries the database to find out count of visible items and does a group on course, unit, lesson,
     * collection#format. It then converts it to JSON. Thus the caller gets the count of assessments and/or
     * collections in that CUL hierarchy.
     * It does not roll up the same to Unit and Course. The caller need to run arithmetic to roll up from lesson
     * level to above levels
     *
     * @param classId
     * @param courseId
     * @return JsonObject with necessary statistics
     */
    public static JsonObject getCourseVisibleStatistics(String classId, String courseId) {
        // TODO: Implement this using query defined in the AJEntityCollection
        return null;
    }
}
