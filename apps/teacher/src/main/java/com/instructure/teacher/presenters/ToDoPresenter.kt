/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.instructure.teacher.presenters

import com.instructure.canvasapi2.managers.AssignmentManager
import com.instructure.canvasapi2.managers.CourseManager
import com.instructure.canvasapi2.managers.EnrollmentManager
import com.instructure.canvasapi2.managers.GroupManager
import com.instructure.canvasapi2.managers.ToDoManager
import com.instructure.canvasapi2.models.Assignment
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Enrollment
import com.instructure.canvasapi2.models.GradeableStudent
import com.instructure.canvasapi2.models.GradeableStudentSubmission
import com.instructure.canvasapi2.models.Group
import com.instructure.canvasapi2.models.GroupAssignee
import com.instructure.canvasapi2.models.StudentAssignee
import com.instructure.canvasapi2.models.Submission
import com.instructure.canvasapi2.models.ToDo
import com.instructure.canvasapi2.models.User
import com.instructure.canvasapi2.utils.intersectBy
import com.instructure.canvasapi2.utils.weave.WeaveJob
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.awaitApis
import com.instructure.canvasapi2.utils.weave.awaitPaginated
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.inParallel
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.blueprint.SyncPresenter
import com.instructure.pandautils.utils.AssignmentUtils2
import com.instructure.teacher.events.ToDoListUpdatedEvent
import com.instructure.teacher.utils.getState
import com.instructure.teacher.viewinterface.ToDoView
import org.greenrobot.eventbus.EventBus

class ToDoPresenter : SyncPresenter<ToDo, ToDoView>(ToDo::class.java) {

    private var apiCall: WeaveJob? = null
    private var routeCalls: WeaveJob? = null

    override fun loadData(forceNetwork: Boolean) {
        if (data.size() > 0 && !forceNetwork) return
        viewCallback?.onRefreshStarted()

        apiCall = tryWeave {
            viewCallback?.onRefreshStarted()

            val courses: HashMap<Long, Course> = HashMap()
            val groups: HashMap<Long, Group> = HashMap()
            inParallel {

                // Get Courses
                await<List<Course>>({ CourseManager.getCoursesTeacher(forceNetwork, it) }) {
                    it.forEach { courses.put(it.id, it) }
                }

                // Get groups
                await<List<Group>>({ GroupManager.getAllGroups(it, forceNetwork) }) {
                    it.forEach { groups.put(it.id, it) }
                }
            }
            awaitPaginated<List<ToDo>> {
                onRequest { callback ->
                    ToDoManager.getUserTodos(callback, forceNetwork)
                }
                onResponse { response ->
                    val toDos = response
                        .filter {
                            // Exclude items that don't need grading. These may be from a Section the teacher/ta can't access.
                            it.needsGradingCount > 0
                        }
                        .onEach {
                            // Set the context info for each to do
                            ToDo.setContextInfo(it, courses, groups)
                        }

                    data.addOrUpdate(toDos)
                    data.removeDistinctItems(toDos)

                    // We want the count of the assignments that need grading. If there are more than 100 we will just show 99+
                    val todoCount = toDos.sumOf { it.needsGradingCount }

                    EventBus.getDefault().post(ToDoListUpdatedEvent(todoCount))
                    viewCallback?.onRefreshFinished()
                    viewCallback?.checkIfEmpty()
                }
                onError { }
            }

        } catch {
            it.printStackTrace()
        }
    }

    fun goToUngradedSubmissions(assignment: Assignment, courseId: Long) {
        routeCalls = tryWeave {
            viewCallback?.onRefreshStarted()

            val unfilteredSubmissions: List<GradeableStudentSubmission>
            // Get the course
            val course = awaitApi<Course> { CourseManager.getCourse(courseId, it, true) }
            val (gradeableStudents, enrollments, submissions) = awaitApis<List<GradeableStudent>, List<Enrollment>, List<Submission>>(
                    { AssignmentManager.getAllGradeableStudentsForAssignment(assignment.courseId, assignment.id, true, it) },
                    { EnrollmentManager.getAllEnrollmentsForCourse(assignment.courseId, null, true, it) },
                    { AssignmentManager.getAllSubmissionsForAssignment(assignment.courseId, assignment.id, true, it) }
            )
            val enrollmentMap = enrollments.associateBy { it.user?.id }
            val students = gradeableStudents.distinctBy { it.id }.map { enrollmentMap[it.id]?.user }.filterNotNull()
            if (assignment.groupCategoryId > 0 && !assignment.isGradeGroupsIndividually) {
                val groups = awaitApi<List<Group>> { CourseManager.getGroupsForCourse(assignment.courseId, it, false) }
                        .filter { it.groupCategoryId == assignment.groupCategoryId }
                unfilteredSubmissions = makeGroupSubmissions(students, groups, submissions)
            } else {
                val submissionMap = submissions.associateBy { it.userId }
                unfilteredSubmissions = students.map {
                    GradeableStudentSubmission(StudentAssignee(it), submissionMap[it.id])
                }
            }

            // filter the submissions to just the ones that need grading
            val filteredSubmissions = unfilteredSubmissions.filter {
                it.submission?.let { submission ->
                    assignment.getState(submission, true) in listOf(
                        AssignmentUtils2.ASSIGNMENT_STATE_SUBMITTED,
                        AssignmentUtils2.ASSIGNMENT_STATE_SUBMITTED_LATE
                    ) || !submission.isGradeMatchesCurrentSubmission
                } ?: false
            }

            viewCallback?.onRefreshFinished()
            viewCallback?.onRouteSuccessfully(course, assignment, filteredSubmissions)

        } catch {
            viewCallback?.onRefreshFinished()
            viewCallback?.onRouteFailed()
            it.printStackTrace()
        }
    }

    override fun onDestroyed() {
        apiCall?.cancel()
        routeCalls?.cancel()
        super.onDestroyed()
    }

    override fun refresh(forceNetwork: Boolean) {
        apiCall?.cancel()
        clearData()
        loadData(forceNetwork)
    }

    fun nextPage() = apiCall?.next()

    override fun areContentsTheSame(oldItem: ToDo, newItem: ToDo): Boolean {
        return if (containsNull(oldItem.scheduleItem, newItem.scheduleItem) || oldItem.assignment?.id != newItem.assignment?.id) {
            false
        } else oldItem.htmlUrl == newItem.htmlUrl
    }

    override fun areItemsTheSame(item1: ToDo, item2: ToDo) = item1.id == item2.id

    // We don't want to sort the items locally, but we do need id comparison for item updates
    override fun compare(item1: ToDo, item2: ToDo) = if (item1.id == item2.id) 0 else -1

    private fun containsNull(oldItem: Any?, newItem: Any?) = oldItem == null || newItem == null

    private fun makeGroupSubmissions(students: List<User>, groups: List<Group>, submissions: List<Submission>): List<GradeableStudentSubmission> {
        val userMap = students.associateBy { it.id }
        val groupAssignees = groups.map { GroupAssignee(it, it.users.map { userMap[it.id] ?: it }) }

        // Set up individual student assignees, if any
        val individualIds = students.map { it.id } - groupAssignees.flatMap { it.students.map { it.id } }
        val individualAssignees = individualIds
            .map { userMap[it] }
            .filterNotNull()
            .map { StudentAssignee(it) }

        // Divide submissions into group and individual submissions
        val (groupedSubmissions, individualSubmissions) = submissions.partition { it.group?.id ?: 0L != 0L }
        val studentSubmissionMap = individualSubmissions.associateBy { it.userId }
        val groupSubmissionMap = groupedSubmissions
            .groupBy { it.group!!.id }
            .mapValues {
                it.value.reduce { acc, submission ->
                    acc.submissionComments = acc.submissionComments.intersectBy(submission.submissionComments) {
                        "${it.authorId}|${it.comment}|${it.createdAt?.time}"
                    }
                    acc
                }
            }
            .toMap()

        // Set up GradeableStudentSubmissions
        val groupSubs = groupAssignees.map { GradeableStudentSubmission(it, groupSubmissionMap[it.group.id]) }
        val individualSubs = individualAssignees.map { GradeableStudentSubmission(it, studentSubmissionMap[it.student.id]) }

        return groupSubs + individualSubs
    }
}
