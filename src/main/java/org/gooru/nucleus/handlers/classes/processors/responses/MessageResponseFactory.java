package org.gooru.nucleus.handlers.classes.processors.responses;

import io.vertx.core.json.JsonObject;
import org.gooru.nucleus.handlers.classes.constants.MessageConstants;

/**
 * Created by ashish on 6/1/16.
 */
public class MessageResponseFactory {
  public static MessageResponse createInvalidRequestResponse() {
    return new MessageResponse.Builder().failed().setStatusBadRequest().build();
  }

  public static MessageResponse createForbiddenResponse() {
    return new MessageResponse.Builder().failed().setStatusForbidden().build();
  }

  public static MessageResponse createInternalErrorResponse() {
    return new MessageResponse.Builder().failed().setStatusInternalError().build();
  }


  public static MessageResponse createInvalidRequestResponse(String message) {
    return new MessageResponse.Builder().failed().setStatusBadRequest().setResponseBody(new JsonObject().put(MessageConstants.MSG_MESSAGE, message))
                                        .build();
  }

  public static MessageResponse createForbiddenResponse(String message) {
    return new MessageResponse.Builder().failed().setStatusForbidden().setResponseBody(new JsonObject().put(MessageConstants.MSG_MESSAGE, message))
                                        .build();
  }

  public static MessageResponse createInternalErrorResponse(String message) {
    return new MessageResponse.Builder().failed().setStatusInternalError()
                                        .setResponseBody(new JsonObject().put(MessageConstants.MSG_MESSAGE, message)).build();
  }
}
