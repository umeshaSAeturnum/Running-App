package com.example.runningapp.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.runningapp.R
import com.example.runningapp.db.RunDao
import com.example.runningapp.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    //telling dagger that we require a runDao object from the modules
    //dagger looking for modules and discover the function in the AppModule that return RunDao object
//    @Inject and @AndroidEntryPoint responsible for that
//    @Inject
//    lateinit var runDao: RunDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //if the main activity was destroyed and the service is running
        //and when we send the pending intent the main activity would be relaunched
        //in that case onCreate method will execute
        navigateToTrackingFragmentIfNeeded(intent)

        bottomNavigationView.setupWithNavController(navHostFragment.findNavController())
        //when we click the same item from the bottom navigation when we are already in the activity will reload
        // to avoid that use the following coding
        bottomNavigationView.setOnNavigationItemReselectedListener { /*No-Operations override the default behaviour of the function*/}

        //there are 5 fragments however we only need to display the bottom navigation
        navHostFragment.findNavController()
            .addOnDestinationChangedListener{ _, destination, _ ->
                when(destination.id){
                    R.id.settingsFragment, R.id.runFragment, R.id.statisticsFragment ->
                        bottomNavigationView.visibility = View.VISIBLE

                    else -> bottomNavigationView.visibility = View.GONE
                }
            }

    }

    //when the main activity was not detroyed and when we end a pending intent
    //it will not call the onCreate() method
    //instead call this
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent : Intent?){
        if(intent?.action == ACTION_SHOW_TRACKING_FRAGMENT){
            navHostFragment.findNavController().navigate(R.id.action_global_trackingFragment)
        }
    }

    fun setActionBarTitle(title: String?) {
        supportActionBar!!.title = title
    }
}