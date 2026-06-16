package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.LaunchControlHub
import com.example.ui.LaunchViewModel
import com.example.ui.LaunchViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Instantiate our manual guidance launch simulator viewModel
        val viewModel = ViewModelProvider(
            this,
            LaunchViewModelFactory(application)
        )[LaunchViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Load the core ballistic control hub layout
                    LaunchControlHub(viewModel = viewModel)
                }
            }
        }
    }
}
