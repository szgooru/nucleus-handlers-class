package org.gooru.nucleus.handlers.classes.processors;

import org.gooru.nucleus.handlers.classes.constants.MessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Created by ashish on 20/3/16.
 */
public final class ProcessorContextHelper {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorContext.class);

  public static boolean validateUser(String userId) {
    return !(userId == null || userId.isEmpty()) && (userId.equalsIgnoreCase(MessageConstants.MSG_USER_ANONYMOUS) || validateUuid(userId));
  }

  public static boolean validateContextForCode(ProcessorContext context) {
    if (context.classCode() == null || context.classCode().isEmpty()) {
      LOGGER.error("Invalid request, class code is invalid");
      return false;
    }
    return true;
  }

  public static boolean validatePrefsForEmail(ProcessorContext context) {
    String email = context.prefs().getString(MessageConstants.EMAIL_ID);
    if (email == null || email.isEmpty() || !email.contains("@")) {
      LOGGER.error("Incorrect authroization, email not available");
      return false;
    }
    return true;
  }

  public static boolean validateContextWithCourse(ProcessorContext context) {
    return validateContextOnlyCourse(context) && validateContext(context);
  }
  
  public static boolean validateContextWithStudentId(ProcessorContext context) {
    return validateContext(context) && validateContextOnlyStudentId(context);
  }

  public static boolean validateContextWithStudentEmail(ProcessorContext context) {
    return validateContext(context) && validateContextOnlyStudentEmail(context);
  }

  public static boolean validateContextOnlyCourse(ProcessorContext context) {
    if (!validateId(context.courseId())) {
      LOGGER.error("Invalid request, course id not available/incorrect format. Aborting");
      return false;
    }
    return true;
  }

  public static boolean validateContext(ProcessorContext context) {
    if (!validateId(context.classId())) {
      LOGGER.error("Invalid request, class id not available/incorrect format. Aborting");
      return false;
    }
    return true;
  }

  private static boolean validateContextOnlyStudentEmail(ProcessorContext context) {
    if (!validateEmail(context.studentEmail())) {
      LOGGER.error("Invalid request, student email not available/incorrect format. Aborting");
      return false;
    }
    return true;
  }
  
  private static boolean validateEmail(String email) {
    // Rudimentary check for presence of @
    if (email.indexOf('@') != -1) {
      return true;
    }
    return false;
  }

  private static boolean validateContextOnlyStudentId(ProcessorContext context) {
    if (!validateId(context.studentId())) {
      LOGGER.error("Invalid request, student id not available/incorrect format. Aborting");
      return false;
    }
    return true;
  }

  private ProcessorContextHelper() {
    throw new AssertionError("Should not instantiate");
  }

  private static boolean validateId(String id) {
    return !(id == null || id.isEmpty()) && validateUuid(id);
  }

  private static boolean validateUuid(String uuidString) {
    try {
      UUID.fromString(uuidString);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    } catch (Exception e) {
      return false;
    }
  }

}
