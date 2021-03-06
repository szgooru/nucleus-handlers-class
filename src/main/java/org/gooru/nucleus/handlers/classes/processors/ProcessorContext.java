package org.gooru.nucleus.handlers.classes.processors;

import io.vertx.core.json.JsonObject;

/**
 * Created by ashish on 7/1/16.
 */
public final class ProcessorContext {

    private final String userId;
    private final JsonObject prefs;
    private final JsonObject request;
    private final String classId;
    private final String courseId;
    private final String classCode;
    private final String studentId;
    private final String studentEmail;

    private ProcessorContext(String userId, JsonObject prefs, JsonObject request, String classId, String courseId,
        String classCode, String studentId, String studentEmail) {
        if (prefs == null || userId == null || prefs.isEmpty()) {
            throw new IllegalStateException("Processor Context creation failed because of invalid values");
        }
        this.courseId = courseId;
        this.userId = userId;
        this.prefs = prefs.copy();
        this.request = request != null ? request.copy() : null;
        this.classId = classId;
        this.classCode = classCode;
        this.studentEmail = studentEmail;
        this.studentId = studentId;
    }

    public String userId() {
        return this.userId;
    }

    public JsonObject prefs() {
        return this.prefs.copy();
    }

    public JsonObject request() {
        return this.request;
    }

    public String classId() {
        return this.classId;
    }

    public String courseId() {
        return this.courseId;
    }

    public String classCode() {
        return this.classCode;
    }

    public String studentId() {
        return this.studentId;
    }

    public String studentEmail() {
        return this.studentEmail;
    }

    public static class ProcessorContextBuilder {
        private final String userId;
        private final JsonObject prefs;
        private final JsonObject request;
        private final String classId;
        private String courseId;
        private String studentId;
        private String studentEmail;
        private final String classCode;
        private boolean built = false;

        ProcessorContextBuilder(String userId, JsonObject prefs, JsonObject request, String classId, String classCode) {
            if (prefs == null || userId == null || prefs.isEmpty()) {
                throw new IllegalStateException("Processor Context creation failed because of invalid values");
            }
            this.userId = userId;
            this.prefs = prefs.copy();
            this.request = request != null ? request.copy() : null;
            this.classId = classId;
            this.classCode = classCode;
        }

        ProcessorContextBuilder setCourseId(String courseId) {
            if (courseId == null || courseId.isEmpty()) {
                throw new IllegalStateException("Invalid values");
            }
            this.courseId = courseId;
            return this;
        }

        ProcessorContextBuilder setStudentId(String studentId) {
            if (studentId == null || studentId.isEmpty()) {
                throw new IllegalStateException("Invalid values");
            }
            this.studentId = studentId;
            return this;
        }

        ProcessorContextBuilder setStudentEmail(String email) {
            if (studentEmail == null || studentEmail.isEmpty()) {
                throw new IllegalStateException("Invalid values");
            }
            this.studentEmail = email;
            return this;
        }

        ProcessorContext build() {
            if (this.built) {
                throw new IllegalStateException("Tried to build again");
            } else {
                this.built = true;
                return new ProcessorContext(userId, prefs, request, classId, courseId, classCode, studentId,
                    studentEmail);
            }
        }
    }

}
