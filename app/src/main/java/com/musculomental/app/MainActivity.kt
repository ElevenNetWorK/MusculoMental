package com.musculomental.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.musculomental.app.feature.profile.MentalApp
import com.musculomental.app.feature.profile.ProfileViewModel
import com.musculomental.app.feature.library.LibraryViewModel
import com.musculomental.app.feature.workout.WorkoutPlanViewModel
import com.musculomental.app.feature.session.WorkoutSessionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: ProfileViewModel = viewModel(factory = viewModelFactory {
                initializer {
                    ProfileViewModel((application as MentalApplication).profileRepository, createSavedStateHandle())
                }
            })
            val libraryModel: LibraryViewModel = viewModel(factory = viewModelFactory {
                initializer { LibraryViewModel((application as MentalApplication).exerciseLibraryRepository) }
            })
            val workoutModel: WorkoutPlanViewModel = viewModel(factory = viewModelFactory {
                initializer { WorkoutPlanViewModel((application as MentalApplication).workoutPlanRepository) }
            })
            val sessionModel: WorkoutSessionViewModel = viewModel(factory = viewModelFactory {
                initializer {
                    val app = application as MentalApplication
                    WorkoutSessionViewModel(app.workoutSessionRepository, app.restTimerRepository)
                }
            })
            MentalApp(model, libraryModel, workoutModel, sessionModel)
        }
    }
}
