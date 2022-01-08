package com.example.runningapp.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RunDao {

    @Insert(onConflict  = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run : Run)

    @Delete
    suspend fun deleteRun(run:Run)

    @Query("SELECT * FROM `running-table` ORDER BY timestamp DESC")
    fun getAllRunsSortedByDate(): LiveData<List<Run>>

    @Query("SELECT * FROM `running-table` ORDER BY timeInMillis DESC")
    fun getAllRunsSortedByTimeInMilis(): LiveData<List<Run>>

    @Query("SELECT * FROM `running-table` ORDER BY caloriesBurned DESC")
    fun getAllRunsSortedByCaloriesBurnt(): LiveData<List<Run>>

    @Query("SELECT * FROM `running-table` ORDER BY avgSpeedInKMH DESC")
    fun getAllRunsSortedByAvgSpeed(): LiveData<List<Run>>

    @Query("SELECT * FROM `running-table` ORDER BY distanceInMeters DESC")
    fun getAllRunsSortedByDistance(): LiveData<List<Run>>

    @Query("SELECT SUM(timeInMillis) FROM `running-table`")
    fun getTotalTimeInMilis(): LiveData<Long>

    @Query("SELECT SUM(caloriesBurned) FROM `running-table`")
    fun getTotalCaloriesBurned(): LiveData<Int>

    @Query("SELECT SUM(distanceInMeters) FROM `running-table`")
    fun getTotalDistance(): LiveData<Int>

    @Query("SELECT AVG(avgSpeedInKMH) FROM `running-table`")
    fun getTotalAvgSpeed(): LiveData<Long>



}