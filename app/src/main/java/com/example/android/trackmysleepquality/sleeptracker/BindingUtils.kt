package com.example.android.trackmysleepquality.sleeptracker

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.example.android.trackmysleepquality.R
import com.example.android.trackmysleepquality.convertDurationToFormatted
import com.example.android.trackmysleepquality.convertNumericQualityToString
import com.example.android.trackmysleepquality.database.SleepNight

/**
 * Created by Nicholas Fragiskatos on 4/21/2020.
 */

/*
This annotation tells BindingView about the binding adapter.
 */
@BindingAdapter("sleepDurationFormatted")
fun TextView.setSleepDurationFormatted(sleepNight: SleepNight?) {
    /*
    Trying to decide between let and run scope functions. For this case the are going to do the same
    thing essentially, but run allows me to rescope item onto this, so I don't have to type the
    variable name.
     */
//    item?.let {
//        text = convertDurationToFormatted(item.startTimeMilli , item.endTimeMilli , context.resources)
//    }

    sleepNight?.run {
        text = convertDurationToFormatted(startTimeMilli, endTimeMilli, context.resources)
    }
}

@BindingAdapter("sleepQualityString")
fun TextView.setSleepQualityString(sleepNight: SleepNight?) {
    sleepNight?.run {
        text = convertNumericQualityToString(sleepQuality, context.resources)
    }
}

@BindingAdapter("sleepImage")
fun ImageView.setSleepImage(sleepNight: SleepNight?) {
    sleepNight?.run {
        setImageResource(when (sleepQuality) {
            0 -> R.drawable.ic_sleep_0
            1 -> R.drawable.ic_sleep_1
            2 -> R.drawable.ic_sleep_2
            3 -> R.drawable.ic_sleep_3
            4 -> R.drawable.ic_sleep_4
            5 -> R.drawable.ic_sleep_5
            else -> R.drawable.ic_sleep_active
        })
    }
}