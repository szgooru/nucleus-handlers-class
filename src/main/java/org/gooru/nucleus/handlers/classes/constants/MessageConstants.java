package org.gooru.nucleus.handlers.classes.constants;

public final class MessageConstants {

    public static final String MSG_HEADER_OP = "mb.operation";
    public static final String MSG_HEADER_TOKEN = "session.token";
    public static final String MSG_OP_AUTH_WITH_PREFS = "auth.with.prefs";
    public static final String MSG_OP_STATUS = "mb.operation.status";
    public static final String MSG_KEY_PREFS = "prefs";
    public static final String MSG_OP_STATUS_SUCCESS = "success";
    public static final String MSG_OP_STATUS_ERROR = "error";
    public static final String MSG_OP_STATUS_VALIDATION_ERROR = "error.validation";
    public static final String MSG_USER_ANONYMOUS = "anonymous";
    public static final String MSG_USER_ID = "user_id";
    public static final String MSG_HTTP_STATUS = "http.status";
    public static final String MSG_HTTP_BODY = "http.body";
    public static final String MSG_HTTP_RESPONSE = "http.response";
    public static final String MSG_HTTP_ERROR = "http.error";
    public static final String MSG_HTTP_VALIDATION_ERROR = "http.validation.error";
    public static final String MSG_HTTP_HEADERS = "http.headers";
    public static final String MSG_MESSAGE = "message";

    // Class operations
    public static final String MSG_OP_CLASS_CREATE = "class.create";
    public static final String MSG_OP_CLASS_UPDATE = "class.update";
    public static final String MSG_OP_CLASS_DELETE = "class.delete";
    public static final String MSG_OP_CLASS_GET = "class.get";
    public static final String MSG_OP_CLASS_MEMBERS_GET = "class.members.get";
    public static final String MSG_OP_CLASS_COLLABORATORS_UPDATE = "class.collaborators.update";
    public static final String MSG_OP_CLASS_LIST = "class.list";
    public static final String MSG_OP_CLASS_LIST_FOR_COURSE = "class.list.for.course";
    public static final String MSG_OP_CLASS_JOIN = "class.join";
    public static final String MSG_OP_CLASS_INVITE = "class.invite.user";
    public static final String MSG_OP_CLASS_COURSE_ASSOCIATION = "class.course.association";
    public static final String MSG_OP_CLASS_SET_CONTENT_VISIBILITY = "class.content.visibility.set";
    public static final String MSG_OP_CLASS_GET_CONTENT_VISIBILITY_STATS = "class.content.visibility.get.stats";
    public static final String MSG_OP_CLASS_GET_CONTENT_VISIBILITY = "class.content.visibility.get";
    public static final String MSG_OP_CLASS_INVITE_REMOVE = "class.invite.user.remove";
    public static final String MSG_OP_CLASS_REMOVE_STUDENT = "class.join.removal";

    // Containers for different responses
    public static final String RESP_CONTAINER_MBUS = "mb.container";
    public static final String RESP_CONTAINER_EVENT = "mb.event";

    public static final String CLASS_ID = "classId";
    public static final String COURSE_ID = "courseId";
    public static final String STUDENT_ID = "studentId";
    public static final String CLASS_CODE = "classCode";
    public static final String EMAIL_ID = "email_id";
    public static final String USER_ID = "userId";
    public static final String EMAIL = "email";

    private MessageConstants() {
        throw new AssertionError();
    }

}
