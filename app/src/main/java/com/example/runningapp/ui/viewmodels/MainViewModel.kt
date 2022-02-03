package com.example.runningapp.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runningapp.db.Run
import com.example.runningapp.other.SortType
import com.example.runningapp.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

//it is not required to explicitly define provides function for MainRepository
//as the parameter for MainRepository is a runDAo object and there is provides function for it
//dagger automatically knows how to creates MainRepository
@HiltViewModel
class MainViewModel @Inject constructor(
    val mainRepository: MainRepository
) : ViewModel() {

    //get the livedata in repository
    private val runSortedByDate = mainRepository.getAllRunsSortedByDate()
    private val runSortedByDistance = mainRepository.getAllRunsSortedByDistance()
    private val runSortedByCaloriesBurned = mainRepository.getAllRunsSortedByCaloriesBurnt()
    private val runSortedByTimeInMillis = mainRepository.getAllRunsSortedByTimeInMilis()
    private val runSortedByAvgSpeed = mainRepository.getAllRunsSortedByAvgSpeed()

    //it is a type livedata which allows to merge different livedata together
    // write logic to sort the data
    //so make the above livedata objects private
    val runs = MediatorLiveData<List<Run>>()

    var sortType = SortType.DATE

    //merge all the livedata and emit them according to th sort type
    init{
        //the lambda function will execute each time when the runSortedByDate livedata is changed
        //the actual results of the MediatorLiveData will be runSortedByDate
        runs.addSource(runSortedByDate){ result ->
            if(sortType == SortType.DATE)
                result?.let {
                    runs.value = it
                }
        }

        runs.addSource(runSortedByAvgSpeed){ result ->
            if(sortType == SortType.AVG_SPEED)
                result?.let {
                    runs.value = it
                }
        }

        runs.addSource(runSortedByCaloriesBurned){ result ->
            if(sortType == SortType.CALORIES_BURNED)
                result?.let {
                    runs.value = it
                }
        }

        runs.addSource(runSortedByDistance){ result ->
            if(sortType == SortType.DISTANCE)
                result?.let {
                    runs.value = it
                }
        }

        runs.addSource(runSortedByTimeInMillis){ result ->
            if(sortType == SortType.RUNNING_TIME)
                result?.let {
                    runs.value = it
                }
        }
    }

    fun sortRuns(sortType:SortType) = when(sortType){
        SortType.DATE -> runSortedByDate.value?.let { runs.value = it }
        SortType.RUNNING_TIME -> runSortedByTimeInMillis.value?.let { runs.value = it }
        SortType.CALORIES_BURNED -> runSortedByCaloriesBurned.value?.let { runs.value = it }
        SortType.DISTANCE -> runSortedByDistance.value?.let { runs.value = it }
        SortType.AVG_SPEED -> runSortedByAvgSpeed.value?.let { runs.value = it }
    }.also {
        this.sortType = sortType
    }

    fun insertRun(run: Run) = viewModelScope.launch {
        mainRepository.insertRun(run)
    }


}


