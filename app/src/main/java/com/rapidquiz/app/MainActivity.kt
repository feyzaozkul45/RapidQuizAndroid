package com.rapidquiz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rapidquiz.app.navigation.AppNavHost
import com.rapidquiz.app.ui.common.ProvideServerState
import com.rapidquiz.app.ui.theme.RapidQuizTheme

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
