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
 */
package com.instructure.pandautils.compose.features.settings

import android.content.Context
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.instructure.espresso.retry
import com.instructure.pandautils.R
import com.instructure.pandautils.features.settings.SettingsItem
import com.instructure.pandautils.features.settings.SettingsItemUiState
import com.instructure.pandautils.features.settings.SettingsScreen
import com.instructure.pandautils.features.settings.SettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun testSettingsItems() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val items = mapOf(
            R.string.preferences to listOf(
                SettingsItemUiState(SettingsItem.APP_THEME),
                SettingsItemUiState(SettingsItem.PROFILE_SETTINGS),
                SettingsItemUiState(SettingsItem.PUSH_NOTIFICATIONS),
                SettingsItemUiState(SettingsItem.EMAIL_NOTIFICATIONS),
            ),
            R.string.inboxSettingsTitle to listOf(
                SettingsItemUiState(SettingsItem.INBOX_SIGNATURE, R.string.inboxSignatureEnabled),
            ),
            R.string.offlineContent to listOf(
                SettingsItemUiState(SettingsItem.OFFLINE_SYNCHRONIZATION, R.string.daily)
            ),
            R.string.legal to listOf(
                SettingsItemUiState(SettingsItem.ABOUT),
            )
        )

        val uiState = SettingsUiState(
            items = items,
            homeroomView = true,
            appTheme = R.string.appThemeLight,
            actionHandler = {}
        )
        composeTestRule.setContent {
            SettingsScreen(uiState = uiState) {}
        }

        items.forEach { (title, items) ->
            composeTestRule.onNodeWithText(context.getString(title)).assertExists()
            items.forEach { item ->
                retry(catchBlock = {
                    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                    val y = device.displayHeight / 2
                    val x = device.displayWidth / 2
                    device.swipe(
                        x,
                        y,
                        x,
                        0,
                        10
                    )
                }) {
                    composeTestRule.onNode(
                        hasTestTag("settingsItem").and(
                            hasAnyDescendant(
                                hasText(
                                    context.getString(item.item.res)
                                )
                            )
                        ), useUnmergedTree = true
                    ).assertExists()
                }
            }
        }
    }
}