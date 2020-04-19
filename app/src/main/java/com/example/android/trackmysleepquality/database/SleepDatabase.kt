/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SleepNight::class], version = 1, exportSchema = false)
abstract class SleepDatabase: RoomDatabase() {

    abstract val sleepDatabaseDao: SleepDatabaseDao

    /*
    Allows clients to access methods for creating/getting database without instantiating class.
    Whole point of this class is to provide a database, there's no reason to ever instantiate.
     */
    companion object {

        /*
        Our reference to the database once we have one. Prevents repeatedly opening connections to the
        database.

        @Volatile annotation ensures value is always up-to-date, and it's the same value across all
        execution threads. NEver cached and all writes and read done to and from main memory.
        Therefore, changes made in on thread or instance are visible to all other threads immediately.
        Helps prevent race conditions (e.g., two threads trying to update the same entity in the cache
        at the same time)
         */
        @Volatile
        private var INSTANCE: SleepDatabase? = null

        /*
        Returs reference to sleep database. Will use a Database Builder
         */
        fun getInstance(context: Context): SleepDatabase {

            /*
            Need in case multiple threads ask for the database instance at the same time. Not likely
            in this app, but still good practice. Only one thread will have access at a time.
             */
            synchronized(this) {
                var instance = INSTANCE

                /*
                Use Room's database builder to actually create the database if it doesn't exist yet.
                We aren't doing any smart migration cause it's a toy app
                 */
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.applicationContext,
                            SleepDatabase::class.java,
                            "sleep_history_database"
                    )
                            .fallbackToDestructiveMigration()
                            .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}