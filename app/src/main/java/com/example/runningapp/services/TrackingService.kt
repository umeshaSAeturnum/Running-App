package com.example.runningapp.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.LocationRequest
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.runningapp.R
import com.example.runningapp.other.Constants.ACTION_PAUSE_SERVICE
import com.example.runningapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningapp.other.Constants.ACTION_STOP_SERVICE
import com.example.runningapp.other.Constants.FASTEST_LOCATION_INTERVAL
import com.example.runningapp.other.Constants.LOCATION_UPDATE_INTERVAL
import com.example.runningapp.other.Constants.NOTIFICATION_CHANNEl_ID
import com.example.runningapp.other.Constants.NOTIFICATION_CHANNEl_NAME
import com.example.runningapp.other.Constants.NOTIFICATION_ID
import com.example.runningapp.other.Constants.TIMER_UPDATE_INTERVAL
import com.example.runningapp.other.TrackingUtility
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.ArrayList

//Normally a service class is inherit from Service or InheritService
//in here it is inherit from LifecycleService
// that is bcz we want to observe from live data object inside this class
//observer function of the LiveData objects need the lifecycle owner
// if we do not use LifecycleService we can't give the instance of this service as a lifecycle owner to the observer function

//to send data from activity to service we use intents
//to send coordinates from service to activity we can use 2 options
// 1. making the service a bound service (act as a server) client - fragment
// 2. singleton pattern - using companion objects

typealias Polyline = MutableList<LatLng>
typealias  Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService : LifecycleService() {
// there are 3 actions we can send to the service - start/resume, pause, stop

    var isFirstRun = true
    var serviceKill = false

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder : NotificationCompat.Builder

    //to update the text in the notification need to post a notification with the same id
    //have slightly different configurations than the baseNotificationBuilder
    lateinit var curNotificationBuilder: NotificationCompat.Builder

    //use 2 MutableLiveData objects 1. in milis and 2. in seconds as we need to get more accurate data for operations
    // and  seconds to send notifications we do not want much more accurate time as it is less user friendly
    private val timeRunInSeconds = MutableLiveData<Long>()

    companion object{
        val timeRunInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
    }

    //initializing MutableLiveData variables
    private fun postInitialValues(){
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeRunInSeconds.postValue(0L)
        timeRunInMillis.postValue(0L)
    }

    override fun onCreate() {
        super.onCreate()
        //initialize witht eh baseNotificationBUilder and sometimes passes change the text pf the curNotificationBuilder and shows the notification
        curNotificationBuilder = baseNotificationBuilder
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        isTracking.observe(this, Observer {
            //we want to perform this when the tracking state changes
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        })
    }

    //function to kill the service while inside the service
    private fun killService(){
        serviceKill = true
        isFirstRun  = false
        pauseService()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    //use when sending intents to the service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{
            when(it.action){
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(isFirstRun){
                        startForegroundService()
                        isFirstRun = false
                    }
                    else{
                        Timber.d("Resuming Service .....")
                        startTimer()
                    }

                }
            }

            when(it.action){
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused Service")
                    pauseService()
                }
            }

            when(it.action){
                ACTION_STOP_SERVICE -> {
                    Timber.d("Stopped Service")
                    killService()
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }
    //a callback that gives the location updates consistently
    val locationCallback = object: LocationCallback(){
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if(isTracking.value!!){
                result?.locations?.let {locations ->
                    for(location in locations){
                        addPathPoint(location)
                        Timber.d("NEW LOCATION: ${location.latitude}. ${location.longitude}")
                    }

                }
            }
        }
    }

    private var isTimerEnabled = false
    private var lapTime = 0L

    //total of all lapTime
    private var timeRun = 0L

    //timestamp when the timer started
    private var timeStarted = 0L

    private var lastSecondTimeStamp = 0L

    //function to start timer
    //function to track actual time and trigger the livedata observers on time changes
    //this is called when starting and resuming the timer
    private fun startTimer() {
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        //handling starting and stopping of the current time tracking
        //implement using coroutine as we do not want to call the obeservers all the time - bad practice
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!){
                //time difference between now and timeStarted
                lapTime = System.currentTimeMillis() - timeStarted

                timeRunInMillis.postValue(timeRun + lapTime)

                // timeRunInMillis = 1550 and lastScondTimeStamp = lastSecond pass in milli seconds (1s)
                //check 1550 >= 1000L if true we need to add 1 to lastSecondTimeStamp
                if(timeRunInMillis.value!! >= lastSecondTimeStamp + 1000L ){
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
                    lastSecondTimeStamp += 1000L

                }

                //update each 50ms
                delay(TIMER_UPDATE_INTERVAL)
            }

            timeRun += lapTime
        }

    }

    //add a coordinate to the last polyline of the polyline list
    private fun addPathPoint(location : Location?){
        location?.let {
            val position = LatLng(location.latitude, location.longitude)
            //need to add the position to the last polyline of polylines list

            pathPoints.value?.apply {
                last().add(position)
                pathPoints.postValue(this)

            }
        }
    }
    //add and empty polyline to the list o
    private fun addEmptyPolyline() = pathPoints.value?.apply {
            add(mutableListOf())
            pathPoints.postValue(this)

    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    private fun pauseService(){
        isTracking.postValue(false)
        isTimerEnabled = false
    }

    private fun updateNotificationTrackingState(isTracking: Boolean){
        val notificationActionText = if(isTracking) "Pause" else "resume"
        //require a pending intend to convey the servers what should happen when we click the action

        val pendingIntent = if(isTracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, FLAG_UPDATE_CURRENT)
        }
        else{
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, FLAG_UPDATE_CURRENT)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        curNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(curNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }

        if(!serviceKill){
            curNotificationBuilder = baseNotificationBuilder
                .addAction(R.drawable.ic_pause_black_24dp,notificationActionText,pendingIntent)

            notificationManager.notify(NOTIFICATION_ID,curNotificationBuilder.build())
        }
    }

    //if isTracking true we need to obtain location updates
    //else we should stop getting location updates
    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking:Boolean){
        if(isTracking){
            //check whether we are allow to track the location
            if(TrackingUtility.hasLocationPermissions(this)){
                //request location tracking
                val request = LocationRequest().apply {
                    //how often we want to get the location request
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FASTEST_LOCATION_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }

                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        }
        else{
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun startForegroundService() {
        startTimer()
        isTracking.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel(notificationManager)
        }

        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        timeRunInSeconds.observe(this, Observer {
            if(!serviceKill) {
                val notification = curNotificationBuilder
                    .setContentText(TrackingUtility.getFormattedStopWatchTime(it * 1000L))
                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
        })
    }


    //making  the service a foreground service
    //we need to show notifications which require a channel
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channel  = NotificationChannel(
            NOTIFICATION_CHANNEl_ID,
            NOTIFICATION_CHANNEl_NAME,
            //In this application we send notifications in every second.
            // so if we set higher than IMPORTANCE_LOW, each time of the notification popup the phone will ring
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

}