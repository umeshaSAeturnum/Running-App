package com.example.runningapp.other

import android.content.Context
import android.location.Location

import android.os.Build
import com.example.runningapp.services.Polyline
import pub.devrel.easypermissions.EasyPermissions
import java.util.jar.Manifest
import java.util.concurrent.TimeUnit


object TrackingUtility {
    fun hasLocationPermissions(context: Context) =
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            EasyPermissions.hasPermissions(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        else{
            EasyPermissions.hasPermissions(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }

    //includeMillis is to pass wther the milli secondsa are required or not
    fun getFormattedStopWatchTime(ms : Long , includeMillis : Boolean = false ) : String{
        var milliseconds = ms
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)

        val minutes = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)

        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        if(!includeMillis){
            return "${if (hours < 10) "0" else ""}$hours:" +
            "${if(minutes <10) "0" else ""}$minutes:" +
            "${if(seconds <10) "0" else ""}$seconds"
        }
        milliseconds -= TimeUnit.SECONDS.toMillis(seconds)
        //as we ned 2 digit number for milliseconds instead of 3 digits
        milliseconds /= 10

        return "${if (hours < 10) "0" else ""}$hours:" +
                "${if(minutes <10) "0" else ""}$minutes:" +
                "${if(seconds <10) "0" else ""}$seconds:" +
                "${if(milliseconds < 10) "0" else ""}$milliseconds"



    }

    fun calculatePolylineLength(polyline: Polyline) : Float{
        var distance = 0f
        //loop through the position of polyline
        //here -2 because we get i+1 and if it is the last position then i + 1 is invalid
        for(i in 0..polyline.size - 2){
            val pos1 = polyline[i]
            val pos2 = polyline[i+1]

            val result = FloatArray(1)
            Location.distanceBetween(
                pos1.latitude,
                pos2.longitude,
                pos2.latitude,
                pos2.longitude,
                result
            )
            distance += result[0]
        }

        return distance

    }
}