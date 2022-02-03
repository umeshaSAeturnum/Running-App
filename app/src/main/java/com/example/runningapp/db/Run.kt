package com.example.runningapp.db

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running-table")
data class Run(
    var img: Bitmap? = null,
    //when we start the run
    var timestamp: Long = 0L,
    var avgSpeedInKMH: Float = 0f,
    var distanceInMeters: Int = 0,
    //total time taken for the run
    var timeInMillis: Long = 0L,
    var caloriesBurned: Int = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}