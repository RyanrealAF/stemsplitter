package com.ryanrealaf.stemsplitter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.MainScreen
import com.example.ui.StemsplitterViewModel
import com.example.ui.theme.StemsplitterTheme

/**
 * Main entry point for Stemsplitter matching applicationId com.ryanrealaf.stemsplitter.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: StemsplitterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StemsplitterTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
