package com.example.rapidquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.rapidquiz.navigation.AppNavHost
import com.example.rapidquiz.ui.common.ProvideServerState
import com.example.rapidquiz.ui.theme.RapidQuizTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as RapidQuizApp).container
        setContent {
            RapidQuizTheme {
                ProvideServerState(container.serverState) {
                    AppNavHost()
                }
            }
        }
    }
}
