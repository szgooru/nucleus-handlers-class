package org.gooru.nucleus.handlers.classes.processors.events;

import io.vertx.core.json.JsonObject;

/**
 * Created by ashish on 19/1/16.
 */
public final class EventBuilderFactory {

  private static final String EVT_CLASS_CREATE = "event.class.create";
  private static final String EVT_CLASS_UPDATE = "event.class.update";
  private static final String EVT_CLASS_DELETE = "event.class.delete";
  private static final String EVT_CLASS_COPY = "event.class.copy";
  private static final String EVENT_NAME = "event.name";
  private static final String EVENT_BODY = "event.body";
  private static final String CLASS_ID = "id";

  public static EventBuilder getDeleteClassEventBuilder(String questionId) {
    return () -> new JsonObject().put(EVENT_NAME, EVT_CLASS_DELETE).put(EVENT_BODY, new JsonObject().put(CLASS_ID, questionId));
  }

  public static EventBuilder getCreateClassEventBuilder(String questionId) {
    return () -> new JsonObject().put(EVENT_NAME, EVT_CLASS_CREATE).put(EVENT_BODY, new JsonObject().put(CLASS_ID, questionId));
  }

  public static EventBuilder getUpdateClassEventBuilder(String questionId) {
    return () -> new JsonObject().put(EVENT_NAME, EVT_CLASS_UPDATE).put(EVENT_BODY, new JsonObject().put(CLASS_ID, questionId));
  }

  private EventBuilderFactory() {
    throw new AssertionError();
  }
}
