/**
 * Created by ashish on 9/2/16.
 */
package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.dbauth;

/**
 * Here are the authorizers requirement that is needed for each DB Handler
 *
 * @link AssociateCourseWithClassHandler
 * Only the owner of course can do this operation. The ower should also be owner of course.
 *
 * @link CreateClassHandler
 * Anyone who is logged in can create the class
 *
 * @link DeleteClassHandler
 * Only the owner of class can delete the classes. Caller needs to make sure that there is no usage data present
 *
 * @link FetchClassesForCourseHandler
 * User should be owner of Course
 *
 * @link FetchClassesForUserHandler
 * Logged in user is important. Classes fetched will be classes where user is student as well as user is owner in different buckets
 *
 * @link FetchClassHandler
 * Anyone can fetch class details including anonymous
 *
 * @link FetchClassMembersHandler
 *
 * @link InviteStudentToClassHandler
 * Owner or collaborator of class can do this operation
 *
 * @link JoinClassByStudentHandler
 * If the class is restricted, then user needs to be invited. In case it is open, user needs to be logged in.
 *
 * @link UpdateClassHandler
 * Owner or collaborator of class can do this operation
 *
 * @link UpdateCollaboratorForClass
 * Only the owner of class can do this operation
 */
