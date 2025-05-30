/*
 * Copyright (C) 2025 - present Instructure, Inc.
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
 */package com.instructure.pandautils.analytics.pageview.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PageViewDao {

    @Insert
    suspend fun insert(pageViewEvent: PageViewEvent)

    @Update
    suspend fun update(pageViewEvent: PageViewEvent)

    @Delete
    suspend fun delete(events: List<PageViewEvent>)

    @Delete
    suspend fun delete(event: PageViewEvent)

    @Query("SELECT * FROM PageViewEvent")
    suspend fun getAllPageViewEvents(): List<PageViewEvent>
}