package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbhelpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityClass;
import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities.AJEntityCollection;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.conversion.ConversionException;
import org.javalite.common.Convert;
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
     * @param classId  Id of the class for which visibility is needed
     * @param courseId The course associated with specified class
     * @param result   JsonObject which will be populated with all the assessments id which are visible in that class
     */
    public static void populateVisibleAssessments(String classId, String courseId, JsonObject result) {
        LazyList<AJEntityCollection> assessments =
            AJEntityCollection.findBySQL(AJEntityCollection.FETCH_VISIBLE_ASSESSMENTS_QUERY, courseId, classId);
        
        Map<String, Set<String>> unitLessonMap = new HashMap<>();
        Map<String, JsonArray> assessmentsByLesson = new HashMap<>();

        assessments.forEach(map -> {
            String unitId = map.get(AJEntityCollection.UNIT_ID).toString();
            String lessonId = map.get(AJEntityCollection.LESSON_ID).toString();
            if (unitLessonMap.containsKey(unitId)) {
                unitLessonMap.get(unitId).add(map.getString(AJEntityCollection.LESSON_ID));
            } else {
                Set<String> lessons = new HashSet<>();
                lessons.add(map.getString(AJEntityCollection.LESSON_ID));
                unitLessonMap.put(unitId, lessons);
            }
            
            if (assessmentsByLesson.containsKey(lessonId)) {
                assessmentsByLesson.get(lessonId).add(map.getString(AJEntityCollection.ID));
            } else {
                JsonArray assessmentsArray = new JsonArray();
                assessmentsArray.add(map.getString(AJEntityCollection.ID));
                assessmentsByLesson.put(lessonId, assessmentsArray);
            }
        });

        JsonArray unitArray = new JsonArray();
        for (Map.Entry<String, Set<String>> stringSetEntry : unitLessonMap.entrySet()) {
            Set<String> lessons = stringSetEntry.getValue();
            JsonArray lessonArray = new JsonArray();
            for (String lessonId : lessons) {
                JsonObject lesson = new JsonObject();
                lesson.put(AJEntityCollection.ID, lessonId);
                lesson.put(AJEntityClass.CV_ASSESSMENTS, assessmentsByLesson.get(lessonId));
                lessonArray.add(lesson);
            }

            JsonObject unit = new JsonObject();
            unit.put(AJEntityCollection.ID, stringSetEntry.getKey());
            unit.put(AJEntityCollection.LESSONS, lessonArray);

            unitArray.add(unit);
        }
        JsonObject course = new JsonObject();
        course.put(AJEntityCollection.ID, courseId);
        course.put(AJEntityCollection.UNITS, unitArray);

        result.put(AJEntityCollection.COURSE, course);
    }

    /**
     * Based on the provided class Id and courseId, find out all the assessments/collections that are visible for that
     * class. This includes both assessments and external assessments
     *
     * @param classId  Id of the class for which visibility is needed
     * @param courseId The course associated with specified class
     * @param result   JsonObject which will get populated with all assessments/collections which are visible
     */
    public static void populateVisibleItems(String classId, String courseId, JsonObject result) {
        LazyList<AJEntityCollection> items =
            AJEntityCollection.findBySQL(AJEntityCollection.FETCH_VISIBLE_ITEMS_QUERY, courseId, classId);
        
        Map<String, Set<String>> unitLessonMap = new HashMap<>();
        Map<String, JsonArray> collectionsByLesson = new HashMap<>();
        Map<String, JsonArray> assessmentsByLesson = new HashMap<>();

        items.forEach(collection -> {
            String id = collection.getString(AJEntityCollection.ID);
            String unitId = collection.getString(AJEntityCollection.UNIT_ID);
            String lessonId = collection.getString(AJEntityCollection.LESSON_ID);

            if (unitLessonMap.containsKey(unitId)) {
                unitLessonMap.get(unitId).add(collection.getString(AJEntityCollection.LESSON_ID));
            } else {
                Set<String> lessons = new HashSet<>();
                lessons.add(collection.getString(AJEntityCollection.LESSON_ID));
                unitLessonMap.put(unitId, lessons);
            }
            
            if (collection.isAssessment() || collection.isAssessmentExternal()) {
                if (assessmentsByLesson.containsKey(lessonId)) {
                    assessmentsByLesson.get(lessonId).add(id);
                } else {
                    JsonArray assessmentsArray = new JsonArray();
                    assessmentsArray.add(id);
                    assessmentsByLesson.put(lessonId, assessmentsArray);
                }
            } else if (collection.isCollection()) {
                if (collectionsByLesson.containsKey(lessonId)) {
                    collectionsByLesson.get(lessonId).add(id);
                } else {
                    JsonArray collectionsArray = new JsonArray();
                    collectionsArray.add(id);
                    collectionsByLesson.put(lessonId, collectionsArray);
                }
            } else {
                LOGGER.warn("Invalid format for collection/assessment id {}", id);
            }
        });

        JsonArray unitArray = new JsonArray();
        for (Map.Entry<String, Set<String>> stringSetEntry : unitLessonMap.entrySet()) {
            Set<String> lessons = stringSetEntry.getValue();
            JsonArray lessonArray = new JsonArray();
            for (String lessonId : lessons) {
                JsonObject lesson = new JsonObject();
                lesson.put(AJEntityCollection.ID, lessonId);
                lesson.put(AJEntityClass.CV_ASSESSMENTS, assessmentsByLesson.get(lessonId));
                lesson.put(AJEntityClass.CV_COLLECTIONS, collectionsByLesson.get(lessonId));
                lessonArray.add(lesson);
            }

            JsonObject unit = new JsonObject();
            unit.put(AJEntityCollection.ID, stringSetEntry.getKey());
            unit.put(AJEntityCollection.LESSONS, lessonArray);

            unitArray.add(unit);
        }
        JsonObject course = new JsonObject();
        course.put(AJEntityCollection.ID, courseId);
        course.put(AJEntityCollection.UNITS, unitArray);

        result.put(AJEntityCollection.COURSE, course);
    }

    /**
     * Find the needed statistics
     * It queries the database to find out count of visible items and does a group on course, unit, lesson,
     * collection#format. It then converts it to JSON. Thus the caller gets the count of assessments and/or
     * collections in that CUL hierarchy.
     * It does not roll up the same to Unit and Course. The caller need to run arithmetic to roll up from lesson
     * level to above levels
     *
     * @param classId  Id of the class for which visibility is needed
     * @param courseId The course associated with specified class
     * @return JsonObject with necessary statistics
     */
    public static JsonObject getCourseVisibleStatistics(String classId, String courseId) {
        List<Map> counts = Base.findAll(AJEntityCollection.FETCH_STATISTICS_QUERY, courseId, classId);
        Map<String, Set<String>> unitLessonMap = new HashMap<>();
        Map<String, Integer> collectionCountByLesson = new HashMap<>();
        Map<String, Integer> assessmentCountByLesson = new HashMap<>();

        counts.forEach(map -> {
            Integer count;
            String unitId = map.get(AJEntityCollection.UNIT_ID).toString();
            String lessonId = map.get(AJEntityCollection.LESSON_ID).toString();
            String format = map.get(AJEntityCollection.FORMAT_TYPE).toString();
            if (unitLessonMap.containsKey(unitId)) {
                unitLessonMap.get(unitId).add(map.get(AJEntityCollection.LESSON_ID).toString());
            } else {
                Set<String> lessons = new HashSet<>();
                lessons.add(map.get(AJEntityCollection.LESSON_ID).toString());
                unitLessonMap.put(unitId, lessons);
            }
            try {
                count = Convert.toInteger(map.get(AJEntityCollection.CA_COUNT));
            } catch (ConversionException ce) {
                count = 0;
            }

            if (format.equalsIgnoreCase(AJEntityCollection.FORMAT_TYPE_ASSESSMENT) || format
                .equalsIgnoreCase(AJEntityCollection.FORMAT_TYPE_ASSESSMENT_EXT)) {
                assessmentCountByLesson.put(lessonId, count);
            } else if (format.equalsIgnoreCase(AJEntityCollection.FORMAT_TYPE_COLLECTION)) {
                collectionCountByLesson.put(lessonId, count);
            }
        });

        JsonObject result = new JsonObject();
        JsonArray unitArray = new JsonArray();
        for (Map.Entry<String, Set<String>> stringSetEntry : unitLessonMap.entrySet()) {
            Set<String> lessons = stringSetEntry.getValue();
            JsonArray lessonArray = new JsonArray();
            for (String lessonId : lessons) {
                JsonObject lesson = new JsonObject();
                Integer assessmentCount = assessmentCountByLesson.get(lessonId);
                Integer collectionCOunt = collectionCountByLesson.get(lessonId);
                lesson.put(AJEntityCollection.ID, lessonId);
                lesson.put(AJEntityCollection.ASSESSMENT_COUNT, assessmentCount != null ? assessmentCount : 0);
                lesson.put(AJEntityCollection.COLLECTION_COUNT, collectionCOunt != null ? collectionCOunt : 0);
                lessonArray.add(lesson);
            }

            JsonObject unit = new JsonObject();
            unit.put(AJEntityCollection.ID, stringSetEntry.getKey());
            unit.put(AJEntityCollection.LESSONS, lessonArray);

            unitArray.add(unit);
        }
        JsonObject course = new JsonObject();
        course.put(AJEntityCollection.ID, courseId);
        course.put(AJEntityCollection.UNITS, unitArray);

        result.put(AJEntityCollection.COURSE, course);
        return result;
    }
}
