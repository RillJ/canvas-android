/*
 * Copyright (C) 2024 - present Instructure, Inc.
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
package com.instructure.parentapp.features.alerts.list

import com.instructure.canvasapi2.apis.CourseAPI
import com.instructure.canvasapi2.apis.ObserverApi
import com.instructure.canvasapi2.models.Alert
import com.instructure.canvasapi2.models.AlertThreshold
import com.instructure.canvasapi2.models.AlertType
import com.instructure.canvasapi2.models.AlertWorkflowState
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.CourseSettings
import com.instructure.canvasapi2.models.ThresholdWorkflowState
import com.instructure.canvasapi2.utils.DataResult
import com.instructure.canvasapi2.utils.LinkHeaders
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.IllegalStateException
import java.time.Instant
import java.util.Date

class AlertsRepositoryTest {

    private val observerApi: ObserverApi = mockk(relaxed = true)
    private val courseApi: CourseAPI.CoursesInterface = mockk(relaxed = true)

    private lateinit var alertsRepository: AlertsRepository

    @Before
    fun setup() {
        createRepository()

        coEvery { courseApi.getCourse(any(), any()) } returns DataResult.Success(Course(id = 1L))
    }

    @Test
    fun `getAlertsForStudent should return an ordered list of alerts`() = runTest {
        val alerts = listOf(
            Alert(
                id = 1,
                actionDate = Date.from(
                    Instant.parse("2024-01-03T00:00:00Z")
                ),
                title = "Alert 1",
                workflowState = AlertWorkflowState.READ,
                alertType = AlertType.ASSIGNMENT_MISSING,
                htmlUrl = "https://example.com/alert1",
                contextId = 1L,
                contextType = "Course",
                lockedForUser = false,
                observerAlertThresholdId = 1L,
                observerId = 1L,
                userId = 2L
            ),
            Alert(
                id = 1,
                actionDate = Date.from(
                    Instant.parse("2024-01-01T00:00:00Z")
                ),
                title = "Alert 2",
                workflowState = AlertWorkflowState.READ,
                alertType = AlertType.ASSIGNMENT_MISSING,
                htmlUrl = "https://example.com/alert2",
                contextId = 1L,
                contextType = "Course",
                lockedForUser = false,
                observerAlertThresholdId = 1L,
                observerId = 1L,
                userId = 2L
            ),
            Alert(
                id = 1,
                actionDate = Date.from(
                    Instant.parse("2024-01-02T00:00:00Z")
                ),
                title = "Alert 3",
                workflowState = AlertWorkflowState.READ,
                alertType = AlertType.ASSIGNMENT_MISSING,
                htmlUrl = "https://example.com/alert3",
                contextId = 1L,
                contextType = "Course",
                lockedForUser = false,
                observerAlertThresholdId = 1L,
                observerId = 1L,
                userId = 2L
            ),
        )
        val expected = alerts.sortedByDescending { it.actionDate }

        coEvery { observerApi.getObserverAlerts(1L, any()) } returns DataResult.Success(alerts)

        val result = alertsRepository.getAlertsForStudent(1L, false)

        assertEquals(expected, result)
    }

    @Test
    fun `get alerts depaginates`() = runTest {
        val page1 = listOf(
            Alert(
                id = 1,
                actionDate = Date.from(
                    Instant.parse("2024-01-01T00:00:00Z")
                ),
                title = "Alert 1",
                workflowState = AlertWorkflowState.READ,
                alertType = AlertType.ASSIGNMENT_MISSING,
                htmlUrl = "https://example.com/alert1",
                contextId = 1L,
                contextType = "Course",
                lockedForUser = false,
                observerAlertThresholdId = 1L,
                observerId = 1L,
                userId = 2L
            )
        )
        val page2 = listOf(
            Alert(
                id = 2,
                actionDate = Date.from(
                    Instant.parse("2024-01-03T00:00:00Z")
                ),
                title = "Alert 2",
                workflowState = AlertWorkflowState.READ,
                alertType = AlertType.ASSIGNMENT_MISSING,
                htmlUrl = "https://example.com/alert2",
                contextId = 1L,
                contextType = "Course",
                lockedForUser = false,
                observerAlertThresholdId = 1L,
                observerId = 1L,
                userId = 2L
            )
        )

        val expected = (page1 + page2).sortedByDescending { it.actionDate }

        coEvery { observerApi.getObserverAlerts(1L, any()) } returns DataResult.Success(page1, linkHeaders = LinkHeaders(nextUrl = "page_2_url"))
        coEvery { observerApi.getNextPageObserverAlerts("page_2_url", any()) } returns DataResult.Success(page2)

        val result = alertsRepository.getAlertsForStudent(1L, false)

        assertEquals(expected, result)
    }

    @Test(expected = IllegalStateException::class)
    fun `get alerts throw exception if call fails`() = runTest {
        coEvery { observerApi.getObserverAlerts(1L, any()) } returns DataResult.Fail()

        alertsRepository.getAlertsForStudent(1L, false)
    }

    @Test
    fun `get alert thresholds`() = runTest {
        val expected = listOf(
            AlertThreshold(
                id = 1,
                observerId = 1,
                threshold = "3",
                alertType = AlertType.ASSIGNMENT_GRADE_LOW,
                userId = 2,
                workflowState = ThresholdWorkflowState.ACTIVE
            ),
            AlertThreshold(
                id = 2,
                observerId = 1,
                threshold = "5",
                alertType = AlertType.ASSIGNMENT_GRADE_HIGH,
                userId = 2,
                workflowState = ThresholdWorkflowState.ACTIVE
            )
        )

        coEvery { observerApi.getObserverAlertThresholds(1L, any()) } returns DataResult.Success(expected)

        val result = alertsRepository.getAlertThresholdForStudent(1L, false)

        assertEquals(expected, result)
    }

    @Test
    fun `get alert thresholds returns empty list if call fails`() = runTest {
        coEvery { observerApi.getObserverAlertThresholds(1L, any()) } returns DataResult.Fail()

        val result = alertsRepository.getAlertThresholdForStudent(1L, false)

        assertEquals(emptyList<AlertThreshold>(), result)
    }

    @Test
    fun `update alert workflow state`() = runTest {
        val alert = Alert(
            id = 1,
            actionDate = Date.from(
                Instant.parse("2024-01-01T00:00:00Z")
            ),
            title = "Alert 1",
            workflowState = AlertWorkflowState.READ,
            alertType = AlertType.ASSIGNMENT_MISSING,
            htmlUrl = "https://example.com/alert1",
            contextId = 1L,
            contextType = "Course",
            lockedForUser = false,
            observerAlertThresholdId = 1L,
            observerId = 1L,
            userId = 2L
        )
        val expected = alert.copy(workflowState = AlertWorkflowState.UNREAD)

        coEvery { observerApi.updateAlertWorkflow(1L, "unread", any()) } returns DataResult.Success(expected)

        val result = alertsRepository.updateAlertWorkflow(1L, AlertWorkflowState.UNREAD)

        coVerify {
            observerApi.updateAlertWorkflow(1L, "unread", any())
        }

        assertEquals(expected, result)
    }

    @Test(expected = IllegalStateException::class)
    fun `update alert workflow state throws exception if call fails`() = runTest {
        coEvery { observerApi.updateAlertWorkflow(1L, "unread", any()) } returns DataResult.Fail()

        alertsRepository.updateAlertWorkflow(1L, AlertWorkflowState.UNREAD)
    }

    @Test
    fun `get alerts count`() = runTest {
        val alerts = listOf(
            Alert(
                id = 1,
                actionDate = Date.from(
                    Instant.parse("2024-01-03T00:00:00Z")
                ),
                title = "Alert 1",
                workflowState = AlertWorkflowState.READ,
                alertType = AlertType.ASSIGNMENT_MISSING,
                htmlUrl = "https://example.com/alert1",
                contextId = 1L,
                contextType = "Course",
                lockedForUser = false,
                observerAlertThresholdId = 1L,
                observerId = 1L,
                userId = 2L
            ),
            Alert(
                id = 1,
                actionDate = Date.from(
                    Instant.parse("2024-01-01T00:00:00Z")
                ),
                title = "Alert 2",
                workflowState = AlertWorkflowState.UNREAD,
                alertType = AlertType.ASSIGNMENT_MISSING,
                htmlUrl = "https://example.com/alert2",
                contextId = 1L,
                contextType = "Course",
                lockedForUser = false,
                observerAlertThresholdId = 1L,
                observerId = 1L,
                userId = 2L
            ),
            Alert(
                id = 1,
                actionDate = Date.from(
                    Instant.parse("2024-01-02T00:00:00Z")
                ),
                title = "Alert 3",
                workflowState = AlertWorkflowState.UNREAD,
                alertType = AlertType.ASSIGNMENT_MISSING,
                htmlUrl = "https://example.com/alert3",
                contextId = 1L,
                contextType = "Course",
                lockedForUser = false,
                observerAlertThresholdId = 1L,
                observerId = 1L,
                userId = 2L
            ),
        )
        coEvery { observerApi.getObserverAlerts(any(), any()) } returns DataResult.Success(alerts)

        val result = alertsRepository.getUnreadAlertCount(1L)

        assertEquals(2, result)
    }

    @Test
    fun `alert count set to 0 if call fails`() = runTest {

        coEvery { observerApi.getObserverAlerts(any(), any()) } returns DataResult.Fail()

        val result = alertsRepository.getUnreadAlertCount(1L)

        assertEquals(0, result)
    }

    @Test
    fun `filter quantitative data if restricted`() = runTest {
        val alerts = listOf(
            Alert(
                id = 1,
                actionDate = Date.from(
                    Instant.parse("2024-01-03T00:00:00Z")
                ),
                title = "Alert 1",
                workflowState = AlertWorkflowState.READ,
                alertType = AlertType.COURSE_GRADE_LOW,
                htmlUrl = "https://example.com/alert1",
                contextId = 1L,
                contextType = "Course",
                lockedForUser = false,
                observerAlertThresholdId = 1L,
                observerId = 1L,
                userId = 2L
            ),
            Alert(
                id = 2,
                actionDate = Date.from(
                    Instant.parse("2024-01-01T00:00:00Z")
                ),
                title = "Alert 2",
                workflowState = AlertWorkflowState.UNREAD,
                alertType = AlertType.ASSIGNMENT_MISSING,
                htmlUrl = "https://example.com/alert2",
                contextId = 2L,
                contextType = "Course",
                lockedForUser = false,
                observerAlertThresholdId = 2L,
                observerId = 1L,
                userId = 2L
            ),
            Alert(
                id = 3,
                actionDate = Date.from(
                    Instant.parse("2024-01-02T00:00:00Z")
                ),
                title = "Alert 3",
                workflowState = AlertWorkflowState.UNREAD,
                alertType = AlertType.COURSE_GRADE_HIGH,
                htmlUrl = "https://example.com/alert3",
                contextId = 2L,
                contextType = "Course",
                lockedForUser = false,
                observerAlertThresholdId = 3L,
                observerId = 1L,
                userId = 2L
            ),
        )

        coEvery { observerApi.getObserverAlerts(any(), any()) } returns DataResult.Success(alerts)
        coEvery { courseApi.getCourseSettings(1L, any()) } returns DataResult.Success(CourseSettings(restrictQuantitativeData = false))
        coEvery { courseApi.getCourseSettings(2L, any()) } returns DataResult.Success(CourseSettings(restrictQuantitativeData = true))

        val result = alertsRepository.getAlertsForStudent(1L, false)

        assertEquals(2, result.size)
        assertEquals(alerts.subList(0, 2), result)
    }

    private fun createRepository() {
        alertsRepository = AlertsRepository(observerApi, courseApi)
    }
}