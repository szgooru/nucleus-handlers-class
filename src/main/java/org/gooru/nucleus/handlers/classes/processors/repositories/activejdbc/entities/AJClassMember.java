package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.entities;

import java.util.ResourceBundle;

import org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.converters.FieldConverter;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.CompositePK;
import org.javalite.activejdbc.annotations.Table;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ashish on 27/2/16.
 */
@Table("class_member")
@CompositePK({ "class_id", "email" })
public class AJClassMember extends Model {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("messages");
    private static final Logger LOGGER = LoggerFactory.getLogger(AJClassMember.class);

    public static final String CLASS_ID = "class_id";
    public static final String USER_ID = "user_id";
    public static final String EMAIL = "email";
    public static final String CREATOR_ID = "creator_id";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String CREATOR_SYSTEM = "creator_system";
    public static final String ROSTER_ID = "roster_id";
    public static final String CLASS_MEMBER_STATUS = "class_member_status";
    public static final String CLASS_MEMBER_STATUS_TYPE = "class_member_status_type";
    public static final String CLASS_MEMBER_STATUS_TYPE_INVITED = "invited";
    public static final String CLASS_MEMBER_STATUS_TYPE_JOINED = "joined";
    public static final String TABLE_CLASS_MEMBER = "class_member";

    public static final String INVITE_STUDENT_QUERY =
        "insert into class_member (class_id, email, class_member_status, creator_system) "
            + "values (?::uuid, ?, ?::class_member_status_type, ?)";

    public static final String FETCH_FOR_USER_QUERY_FILTER = "class_id = ?::uuid and user_id = ?::uuid";
    public static final String FETCH_FOR_EMAIL_QUERY_FILTER = "class_id = ?::uuid and email = ?";
    public static final String FETCH_FOR_MULTIPLE_EMAILS_QUERY_FILTER =
        "class_id = ?::uuid and email = ANY(?::text[])";
    public static final String FETCH_ALL_QUERY_FILTER = "class_id = ?::uuid";
    public static final String DELETE_MEMBERSHIP_FOR_CLASS_QUERY = "delete from class_member where class_id = ?::uuid";
    public static final String FETCH_USER_MEMBERSHIP_QUERY =
        "select class_id from class_member cm, class c where cm.user_id = ?::uuid and cm.class_member_status = "
            + "'joined'::class_member_status_type and cm.class_id = c.id and c.is_deleted = false order by "
            + "cm.updated_at desc";
    public static final String FETCH_MEMBERSHIP_COUNT_FOR_CLASSES =
        "select class_id, count(class_id) from class_member where "
            + "class_member_status = 'joined'::class_member_status_type and class_id = ANY"
            + "(?::uuid[]) group by class_id";
    public static final String STUDENT_COUNT_FROM_SET_FILTER = "class_id = ?::uuid and user_id = ANY(?::uuid[])";
    public static final String FETCH_MEMBERSHIP_COUNT_FOR_CLASS_QUERY =
        "class_member_status = 'joined'::class_member_status_type and class_id = ?::uuid";
    public static final String DELETE_INVITE_QUERY_FILTER =
        "class_id = ?::uuid and email = ? and class_member_status = 'invited'::class_member_status_type";
    public static final String REMOVE_STUDENT_QUERY_FILTER =
        "class_id = ?::uuid and user_id = ?::uuid and class_member_status = 'joined'::class_member_status_type";

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
        PGobject value =
            FieldConverter.convertFieldToNamedType(CLASS_MEMBER_STATUS_TYPE_JOINED, CLASS_MEMBER_STATUS_TYPE);
        if (value != null) {
            this.set(CLASS_MEMBER_STATUS, value);
        } else {
            LOGGER.warn("Not able to set status as '{}' for membership", CLASS_MEMBER_STATUS_TYPE_JOINED);
            this.errors().put(CLASS_ID, RESOURCE_BUNDLE.getString("invalid.status.for.membership"));

        }
    }

    public void setStatusInvited() {
        PGobject value =
            FieldConverter.convertFieldToNamedType(CLASS_MEMBER_STATUS_TYPE_INVITED, CLASS_MEMBER_STATUS_TYPE);
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
