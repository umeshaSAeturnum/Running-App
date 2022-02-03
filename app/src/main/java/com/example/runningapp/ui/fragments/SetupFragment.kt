package com.example.runningapp.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.runningapp.R
import com.example.runningapp.other.Constants.KEY_FIRST_TIME_TOGGLE
import com.example.runningapp.other.Constants.KEY_NAME
import com.example.runningapp.other.Constants.KEY_WEIGHT
import com.example.runningapp.ui.MainActivity
import com.example.runningapp.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_setup.*
import javax.inject.Inject

@AndroidEntryPoint
class SetupFragment: Fragment(R.layout.fragment_setup) {

    @Inject
    lateinit var sharedPreferences : SharedPreferences
    lateinit var name: String

    @set:Inject
    // as boolean is a primitive datatype we cant initialize it as lateinitvar
    var isFirstAppOpen = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //default behaviour is to keep the setup fragment in the back stack of the RunFragment
        //(disable navigate back to the setup fragment from RunFragment
        // bcz if we set values then we do not require that option
        //!isFirstAppOpen -  not the first time app is opened
        if(!isFirstAppOpen){
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.setupFragment, true)
                .build()

            findNavController().navigate(
                R.id.action_setupFragment_to_runFragment,
                savedInstanceState,
                navOptions
            )
        }

        tvContinue.setOnClickListener {
            val success = writePersonalDataToSharedPref()
            val toolBaRText = "Let's go $name!"
            (activity as MainActivity?)?.setActionBarTitle(toolBaRText)

            if(success){
                findNavController().navigate(R.id.action_setupFragment_to_runFragment)

            }else{
                Snackbar.make(requireView(), "Please enter all the fields", Snackbar.LENGTH_SHORT).show()
            }
        }

    }

    private fun writePersonalDataToSharedPref(): Boolean{
        name = etName.text.toString()
        val weight = etWeight.text.toString()

        if(name.isEmpty() || weight.isEmpty()){
            return false
        }

        sharedPreferences.edit()
            .putString(KEY_NAME,name)
            .putFloat(KEY_WEIGHT, weight.toFloat()) // no need to check whether it can be convert to float as we used type of the input as decimals
            .putBoolean(KEY_FIRST_TIME_TOGGLE, false) // not the first time of launching the application
            .apply()

        return true
    }

}