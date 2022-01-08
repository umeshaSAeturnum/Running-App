package com.example.runningapp.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.runningapp.R
import com.example.runningapp.other.Constants
import com.example.runningapp.ui.viewmodels.StatisticsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject
@AndroidEntryPoint
class SettingsFragment: Fragment(R.layout.fragment_settings) {
        @Inject
        lateinit var sharedPreference : SharedPreferences

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            loadFieldsFromSharedPreferences()
            btnApplyChanges.setOnClickListener {
                val success = applyChangesToSharedPref()
                if(success){
                    Snackbar.make(view, "Saved Changes", Snackbar.LENGTH_LONG).show()
                }
                else{
                    Snackbar.make(view, "Please fill all the fields", Snackbar.LENGTH_LONG).show()
                }


            }
        }

        private fun loadFieldsFromSharedPreferences() {
            val name = sharedPreference.getString(Constants.KEY_NAME, "")
            var weight = sharedPreference.getFloat(Constants.KEY_WEIGHT, 80f)
            etName.setText(name)
            etWeight.setText(weight.toString())
        }
        private fun applyChangesToSharedPref() :Boolean{
            val nameText = etName.text.toString()
            val weightText = etWeight.text.toString()

            if(nameText.isEmpty() || weightText.isEmpty()){
                return false
            }

            sharedPreference.edit()
                .putString(Constants.KEY_NAME, nameText)
                .putFloat(Constants.KEY_WEIGHT, weightText.toFloat())
                .apply()



            return true

        }

    }

