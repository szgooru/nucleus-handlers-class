package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Created by ashish on 27/2/16.
 */
@Table("class_member")
public class AJClassMember extends Model {
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

  public static final String FETCH_FOR_USER_QUERY_FILTER = "class_id = ?::uuid and user_id = ?::uuid";

  public void setClassId(String classId) {

  }

  public void setUserId(String userId) {

  }

  public void setStatusJoined() {

  }

  public void setStatusInvited() {

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
