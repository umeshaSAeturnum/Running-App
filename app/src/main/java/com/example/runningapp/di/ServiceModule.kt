package com.example.runningapp.di

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.runningapp.R
import com.example.runningapp.other.Constants
import com.example.runningapp.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
//the dependencies in the service are only live until the service is live.
//So, we use service component
@InstallIn(ServiceComponent::class)
object ServiceModule {
    //unique to services
    //for the lifecycle of the service only single instance of the FuseLocationProviderClient

    @ServiceScoped
    @Provides
    fun provideFuseLocationProviderClient(
        @ApplicationContext app : Context
    ) = FusedLocationProviderClient(app)

    @ServiceScoped
    @Provides
    //when the user click the notification the main activity will be open
    //but user wants to open the tracking fragment
    //when we get a new intent in the main activity we check whether it has the below action attach to that intent
    // if it we need to navigate to tracking fragment
    //but there is no option in the nav_graph to navigate to tracking fragment from where ever we are
    //in order to do that we need to have a global action in nav_graph
    fun provideMainActivityPendingIntent(
        @ApplicationContext app : Context
    ) = PendingIntent.getActivity(
     app,
    0,
     Intent(app, MainActivity::class.java).also{
        it.action = Constants.ACTION_SHOW_TRACKING_FRAGMENT
        },

        //if we launch the pending intent and if it is already exist
        // it will update instead of restarting it
         FLAG_UPDATE_CURRENT
    )

    @ServiceScoped
    @Provides
    //we need to keep the notification even if the user click it - setAutoCancel to false
    //notification can't be swipe away - setOngoing to true
    fun provideBaseNotificationBuilder(
        @ApplicationContext app: Context,
        pendingIntent: PendingIntent
    ) = NotificationCompat.Builder(app, Constants.NOTIFICATION_CHANNEl_ID)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
        .setContentTitle("Running App")
        .setContentText("00:00:00")
        .setContentIntent(pendingIntent) //sending a pending intent

}