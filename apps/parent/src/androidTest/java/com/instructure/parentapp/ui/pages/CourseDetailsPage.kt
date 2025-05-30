/*
 * Copyright (C) 2024 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.instructure.parentapp.ui.pages

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.instructure.canvasapi2.models.Course
import com.instructure.dataseeding.model.CourseApiModel
import com.instructure.espresso.assertTextColor


class CourseDetailsPage(private val composeTestRule: ComposeTestRule) {

    fun assertCourseDetailsDisplayed(course: Course) {
        assertCourseNameDisplayed(course)
        composeTestRule.onNodeWithText("GRADES")
            .assertIsDisplayed()
            .assertIsSelected()
        composeTestRule.onNodeWithText("SYLLABUS").assertIsDisplayed()
        composeTestRule.onNodeWithText("SUMMARY").assertIsDisplayed()
    }

    fun assertCourseNameDisplayed(course: Course) {
        composeTestRule.onNodeWithText(course.name).assertIsDisplayed()
    }

    fun assertCourseNameDisplayed(course: CourseApiModel) {
        composeTestRule.onNodeWithText(course.name).assertIsDisplayed()
    }

    fun selectTab(tabName: String) {
        composeTestRule.onNodeWithText(tabName).performClick()
    }

    fun assertTabSelected(tabName: String) {
        composeTestRule.onNodeWithText(tabName).assertIsSelected()
    }

    fun clickAssignment(assignmentName: String) {
        composeTestRule.onNode(hasTestTag("assignmentItem") and hasText(assignmentName)).performClick()
    }

    fun assertAssignmentLabelTextColor(assignmentName: String, expectedTextColor: Long) {
        composeTestRule.onNodeWithText(assignmentName).assertTextColor(Color(expectedTextColor))
    }

    fun clickComposeMessageFAB() {
        composeTestRule.onNodeWithContentDescription("Send a message about this course").performClick()
    }
}
