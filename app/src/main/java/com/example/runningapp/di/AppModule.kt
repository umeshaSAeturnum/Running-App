package com.example.runningapp.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.room.Room
import com.example.runningapp.db.RunningDatabase
import com.example.runningapp.other.Constants.KEY_FIRST_TIME_TOGGLE
import com.example.runningapp.other.Constants.KEY_NAME
import com.example.runningapp.other.Constants.KEY_WEIGHT
import com.example.runningapp.other.Constants.RUNNING_DATABASE_NAME
import com.example.runningapp.other.Constants.SHARED_PREFERENCES_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
//using @InstallIn(ApplicationComponent::class) we are telling the app that this module should install in our app
//the objects, dependancies we declare in the AppModule that need to be inject are created in the onCreate
// that means it will remain in the whole life cycle of the app until the user quit the application (when the application destroy the dependencies also destroy)

// When we use ActivityComponent the dependencies will remain until the activity is destroyed

//We are using application component because we need to create single database object for whole application
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton // make sure that it creates only one database instant
    //By annotating with @Provides - Telling dagger that the results of this function can be used to create other dependencies and
    //can be used to inject to other classes
    @Provides
    fun providingRunningDatabase(
        //by annotating as ApplicationContext Dagger knows that it needs to take the application as the context
        @ApplicationContext app:Context
    ) = Room.databaseBuilder(
        app,
        RunningDatabase::class.java,
        RUNNING_DATABASE_NAME

    ).build() // creating a database object

    @Singleton
    @Provides
    //here we are just passing a RunningDatabase object as parameter
    //however dagger call the above method and create that object for us
    fun provideRunDao(db:RunningDatabase) = db.getRunDao()

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext app: Context) =
        app.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideName(sharedPreferences: SharedPreferences) = sharedPreferences.getString(KEY_NAME,"")?: ""

    @Singleton
    @Provides
    fun provideWeight(sharedPreferences: SharedPreferences) = sharedPreferences.getFloat(KEY_WEIGHT,80f)

    @Singleton
    @Provides
    fun provideFirstTimeToggle(sharedPreferences: SharedPreferences) = sharedPreferences.getBoolean(
        KEY_FIRST_TIME_TOGGLE,true)




}