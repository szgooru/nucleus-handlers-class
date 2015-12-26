package org.gooru.nucleus.handlers.classes.processors;

import io.vertx.core.json.JsonObject;

public interface Processor {
  public JsonObject process();
}
