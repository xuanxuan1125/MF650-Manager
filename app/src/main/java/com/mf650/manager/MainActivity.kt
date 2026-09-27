package com.mf650.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mf650.manager.ui.navigation.MainAppScaffold
import com.mf650.manager.ui.theme.MF650ManagerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MF650Application

        setContent {
            MF650ManagerTheme {
                MainAppScaffold(repository = app.repository)
            }
        }
    }
}
