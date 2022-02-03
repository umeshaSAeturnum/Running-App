package com.example.runningapp.ui.fragments

import android.content.Intent
import android.graphics.Camera
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.runningapp.R
import com.example.runningapp.db.Run
import com.example.runningapp.other.Constants.ACTION_PAUSE_SERVICE
import com.example.runningapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningapp.other.Constants.ACTION_STOP_SERVICE
import com.example.runningapp.other.Constants.MAP_ZOOM
import com.example.runningapp.other.Constants.POLYLINE_COLOR
import com.example.runningapp.other.Constants.POLYLINE_WIDTH
import com.example.runningapp.other.TrackingUtility
import com.example.runningapp.services.Polyline
import com.example.runningapp.services.Polylines
import com.example.runningapp.services.TrackingService
import com.example.runningapp.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.*
import kotlinx.android.synthetic.main.item_run.*
import java.util.*
import javax.inject.Inject
import kotlin.math.round

const val CANCEL_TRACKING_DIALOG_TAG= "Cancel Tracking"

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {
    private val viewModel: MainViewModel by viewModels()
    private var map: GoogleMap? = null
    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private var curTimeInMillis = 0L


    @set:Inject
    var weight = 80f

    private var menu : Menu? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //mapVIew also has a lifecycle methods
        mapView.onCreate(savedInstanceState )

        btnToggleRun.setOnClickListener{
           toggleRun()
        }

//        whenever Android destroys and recreates your Activity for orientation change,
//        it calls onSaveInstanceState() before destroying and calls onCreate() after recreating.
//        Whatever you save in the bundle in onSaveInstanceState, you can get back from the onCreate() parameter.
        if(savedInstanceState !=null){
            val cancelTrackingDialog = parentFragmentManager.findFragmentByTag(
                CANCEL_TRACKING_DIALOG_TAG) as CancelTrackingDialog?

            cancelTrackingDialog?.setYesListener {
                stopRun()
            }
        }
        btnFinishRun.setOnClickListener {
            zoomToSeeWholeTrack()
            endRunAndSaveToDb()
        }

        //this is called when the fragment is created
        // so even when the device is rotated this is calling
        //so addAllPolylines() method to this
        mapView.getMapAsync {
            map = it
            addAllPolylines()
        }

        subscribeToObservers()
    }

    private fun sendCommandToService(action:String) =
        Intent(requireContext(),TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    //if the device is in onLow memory
    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    //function to subscribe to observers to subscribe the servers to  livedata objects in the service
    private fun subscribeToObservers(){
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        })

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            curTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis,true)
            tvTimer.text = formattedTime
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_tracking_menu, menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if(curTimeInMillis > 0L){
            this.menu?.getItem(0)?.isVisible = true
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.miCancelTracking -> {
                showCancelTrackingDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //initially when we rotate the device the dialog box still popup
    //but when we click yes for the first time the run will not delete
    //bcz in screen rotation the yesListener of the cancel tracking dialog set to null
//    private fun showCancelTrackingDialog(){
//        CancelTrackingDialog().apply {
//            setYesListener {
//                stopRun()
//            }
//        }.show(parentFragmentManager, null)
//    }

//    therefore do the following changes
    private fun showCancelTrackingDialog(){
        CancelTrackingDialog().apply {
                setYesListener {
                    stopRun()
                }
        }.show(parentFragmentManager, CANCEL_TRACKING_DIALOG_TAG)
    }

    private fun stopRun(){
        tvTimer.text = "00:00:00:00"
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }
    //function to start the tracking service if it is in stop or pause state
    private fun toggleRun(){
        if(isTracking){
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(ACTION_PAUSE_SERVICE)
        }
        else{
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    //this function is to observe the data from the servers and  react to those changes
    private fun updateTracking(isTracking : Boolean){
        this.isTracking = isTracking
        if(!isTracking && curTimeInMillis > 0L){
            btnToggleRun.text  = "Start"
            btnFinishRun.visibility = View.VISIBLE
        }
        else if(isTracking){
            btnToggleRun.text = "Stop"
            menu?.getItem(0)?.isVisible = true
            btnFinishRun.visibility = View.GONE
        }
    }

    //we need to get the clear picture of the polylines
    private fun zoomToSeeWholeTrack(){
        val bounds = LatLngBounds.Builder()
        for(polyline in pathPoints){
            for(position in polyline){
                bounds.include(position)
            }
        }

        //it is important to use moveCamera instead of animate camera because it takes some time to zoom
        //we need to take a screenshot of the position
        //moveCamera zoom in quickly
        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                mapView.width,
                mapView.height,
                (mapView.height * 0.05f).toInt()
            )
        )

    }

    private fun endRunAndSaveToDb(){
        map?.snapshot { bmp ->
            var distanceInMeters = 0
            for(polyline in pathPoints){
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }

            val avgSpeed = round((distanceInMeters / 1000f) / (curTimeInMillis / 1000f / 60 / 60) * 10) / 10f
            val dateTimeStamp = Calendar.getInstance().timeInMillis
            val caloriesBurnt = ((distanceInMeters / 1000f) * weight). toInt()
            val run = Run(bmp, dateTimeStamp,avgSpeed,distanceInMeters, curTimeInMillis,caloriesBurnt)
            viewModel.insertRun(run)
            Snackbar.make(
                requireActivity().findViewById(R.id.rootView),
                "Run saved successfully",
                Snackbar.LENGTH_LONG
            ).show()
            stopRun()
        }
    }

    private fun moveCameraToUser(){
        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()){
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM

                )
            )
        }
    }

    //when the device is rotated all the data lost
    //however in MVVM it is handled by LiveData
    //but to connect all the polylines we need to have this function
    private fun addAllPolylines(){
        for(polyline in pathPoints){
            val polylineOptions= PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    //function to connect last 2 points in last polyline
    //this wont connect all the polylines when the device is rotated
    private fun addLatestPolyline(){
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1){
            //get the coordinates of second last point
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]

            //get the last coordinate of the last polyline
            val lastLatLng = pathPoints.last().last()

            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .add(preLastLatLng)
                .add(lastLatLng)
            map?.addPolyline(polylineOptions)
        }
    }

    //onSaveInstanceState function helps to save the map so that we do not want to load every time
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

}