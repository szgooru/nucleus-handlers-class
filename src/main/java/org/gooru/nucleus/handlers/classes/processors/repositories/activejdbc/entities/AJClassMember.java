package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities;

import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.converters.FieldConverter;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * Created by ashish on 27/2/16.
 */
@Table("class_member")
public class AJClassMember extends Model {

  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
  private static final Logger LOGGER = LoggerFactory.getLogger(AJClassMember.class);

  public static final String CLASS_ID = "class_id";
  public static final String USER_ID = "user_id";
  public static final String CREATOR_ID = "creator_id";
  public static final String CREATED_AT = "created_at";
  public static final String UPDATED_AT = "updated_at";
  public static final String CREATOR_SYSTEM = "creator_system";
  public static final String ROSTER_ID = "roster_id";
  public static final String CLASS_MEMBER_STATUS = "class_member_status";
  public static final String CLASS_MEMBER_STATUS_TYPE = "class_member_status_type";
  public static final String CLASS_MEMBER_STATUS_TYPE_INVITED = "invited";
  public static final String CLASS_MEMBER_STATUS_TYPE_JOINED = "joined";

  public static final String INVITE_STUDENT_QUERY =
    "insert into class_member (class_id, user_id, class_member_status, creator_system) values (?::uuid, ?::uuid, ?::class_member_status_type, ?)";

  public static final String FETCH_FOR_USER_QUERY_FILTER = "class_id = ?::uuid and user_id = ?::uuid";
  public static final String FETCH_ALL_QUERY_FILTER = "class_id = ?::uuid";
  public static final String DELETE_MEMBERSHIP_FOR_CLASS_QUERY = "delete from class_member where class_id = ?::uuid";
  public static final String FETCH_USER_MEMBERSHIP_QUERY = "select class_id, class_member_status from class_member where user_id = ?::uuid";

  public void setClassId(String classId) {
    if (classId != null && !classId.isEmpty()) {
      PGobject value = FieldConverter.convertFieldToUuid(classId);
      if (value != null) {
        this.set(CLASS_ID, value);
      } else {
        LOGGER.warn("Not able to set class id as '{}' for membership", classId);
        this.errors().put(CLASS_ID, RESOURCE_BUNDLE.getString("invalid.class.for.membership"));

      }
    }
  }

  public void setUserId(String userId) {
    if (userId != null && !userId.isEmpty()) {
      PGobject value = FieldConverter.convertFieldToUuid(userId);
      if (value != null) {
        this.set(USER_ID, value);
      } else {
        LOGGER.warn("Not able to set user id as '{}' for membership", userId);
        this.errors().put(CLASS_ID, RESOURCE_BUNDLE.getString("invalid.user.for.membership"));
      }

    }
  }

  public void setStatusJoined() {
    PGobject value = FieldConverter.convertFieldToNamedType(CLASS_MEMBER_STATUS_TYPE_JOINED, CLASS_MEMBER_STATUS_TYPE);
    if (value != null) {
      this.set(CLASS_MEMBER_STATUS, value);
    } else {
      LOGGER.warn("Not able to set status as '{}' for membership", CLASS_MEMBER_STATUS_TYPE_JOINED);
      this.errors().put(CLASS_ID, RESOURCE_BUNDLE.getString("invalid.status.for.membership"));

    }
  }

  public void setStatusInvited() {
    PGobject value = FieldConverter.convertFieldToNamedType(CLASS_MEMBER_STATUS_TYPE_INVITED, CLASS_MEMBER_STATUS_TYPE);
    if (value != null) {
      this.set(CLASS_MEMBER_STATUS, value);
    } else {
      LOGGER.warn("Not able to set status as '{}' for membership", CLASS_MEMBER_STATUS_TYPE_INVITED);
      this.errors().put(CLASS_ID, RESOURCE_BUNDLE.getString("invalid.status.for.membership"));
    }
  }

  public void setCreatorSystem(String creatorSystem) {
    if (creatorSystem != null) {
      this.setString(CREATOR_SYSTEM, creatorSystem);
    }
  }

  public void setRosterId(String rosterId) {
    if (rosterId != null) {
      this.setString(ROSTER_ID, rosterId);
    }
  }

}
