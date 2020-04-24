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

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

    /*
    Job to provide when we use coroutines to query the database. We want to use coroutines because
    we don't want potentially long running processes on the main UI thread, and coroutines run asynchronously
    and are non-blocking
     */
    private var viewModelJob = Job()

    /*
    Override just to make sure we clear any job that is possible in the middle of running when the view
    model is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var tonight = MutableLiveData<SleepNight?>()
    val nights = database.getAllNights()

    val nightsString = Transformations.map(nights) {nights ->
        formatNights(nights, application.resources)
    }

    val startButtonVisible = Transformations.map(tonight) {
        it == null
    }

    val stopButtonVisible = Transformations.map(tonight) {
        it != null
    }

    val clearButtonVisible = Transformations.map(nights) {
        it?.isNotEmpty()
    }

    private var _showSnackBarEvent = MutableLiveData<Boolean>()
    val showSnackBarEvent: LiveData<Boolean>
        get() = _showSnackBarEvent

    fun doneShowingSnackbar() {
        _showSnackBarEvent.value = false
    }

    /*
    State variable that the Sleep Tracker fragment can observe and know when it should navigate to
    the Sleep Quality screen.
     */
    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()
    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    /*
    General coroutine pattern

    1. launch coroutine (via the scope) that runs on the UI/main thread (because result affects UI

    2. Inside the launch we call a suspend function which does the long running work (this prevents
    blocking of the UI thread while waiting for result

    3. In the suspend function we switch to run in the IO context (since this long running function has
    nothing to do with the UI), and the IO context has threads in its pool that are optimized for these
    kinds of operations

    4. Call database function within the IO context from previous to do the work.
     */

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        uiScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun getTonightFromDatabase(): SleepNight? {
        return withContext(Dispatchers.IO) {
            var night = database.getTonight()
            if (night?.endTimeMilli != night?.startTimeMilli) {
                night = null
            }
            night
        }
    }

    fun onStartTracking() {
        uiScope.launch {
            val newNight = SleepNight()
            insert(newNight)
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun insert(sleepNight: SleepNight) {
        return withContext(Dispatchers.IO) {
            database.insert(sleepNight)
        }
    }

    fun onStopTracking() {
        uiScope.launch {

            /*
            The @launch annotation specifies that we return from the launch uiScope.launch function
            instead of the lambda.
             */

            val oldNight = tonight.value ?: return@launch

            oldNight.endTimeMilli = System.currentTimeMillis()

            update(oldNight)

            // Notifies Sleep Tracker fragment that it can navigate to Sleep Quality fragment
            _navigateToSleepQuality.value = oldNight
        }
    }

    private suspend fun update(sleepNight: SleepNight) {
        return withContext(Dispatchers.IO) {
            database.update(sleepNight)
        }
    }

    fun onClear() {
        uiScope.launch {
            clear()
            tonight.value = null
            _showSnackBarEvent.value = true
        }
    }

    private val _navigateToSleepDataQuality = MutableLiveData<Long>()
    val navigateToSleepDataQuality
        get() = _navigateToSleepDataQuality

    fun onSleepNightClicked(id: Long) {
        _navigateToSleepDataQuality.value = id
    }

    fun onSleepDataQualityNavigated() {
        _navigateToSleepDataQuality.value = null
    }

    private suspend fun clear() {
        return withContext(Dispatchers.IO) {
            database.clear()
        }
    }
}

