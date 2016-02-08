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

  private ProcessorContext(String userId, JsonObject prefs, JsonObject request, String classId, String courseId) {
    if (prefs == null || userId == null || prefs.isEmpty()) {
      throw new IllegalStateException("Processor Context creation failed because of invalid values");
    }
    this.courseId = courseId;
    this.userId = userId;
    this.prefs = prefs.copy();
    this.request = request != null ? request.copy() : null;
    this.classId = classId;
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

  public static class ProcessorContextBuilder {
    private final String userId;
    private final JsonObject prefs;
    private final JsonObject request;
    private final String classId;
    private String courseId;
    private boolean built = false;

    ProcessorContextBuilder(String userId, JsonObject prefs, JsonObject request, String classId) {
      if (prefs == null || userId == null || prefs.isEmpty()) {
        throw new IllegalStateException("Processor Context creation failed because of invalid values");
      }
      this.userId = userId;
      this.prefs = prefs.copy();
      this.request = request != null ? request.copy() : null;
      this.classId = classId;
    }

    ProcessorContextBuilder setCourseId(String courseId) {
      if (courseId == null || courseId.isEmpty()) {
        throw new IllegalStateException("Invalid values");
      }
      this.courseId = courseId;
      return this;
    }

    ProcessorContext build() {
      if (this.built) {
        throw new IllegalStateException("Tried to build again");
      } else {
        this.built = true;
        return new ProcessorContext(userId, prefs, request, classId, courseId);
      }
    }
  }

}
