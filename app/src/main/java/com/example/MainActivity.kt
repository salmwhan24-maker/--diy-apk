package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AbuIshaqNavigationApp
import com.example.ui.GoldShopViewModel
import com.example.ui.theme.AbuIshaqGoldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = AppContainer(applicationContext)
        val viewModel = ViewModelProvider(
            this,
            GoldShopViewModel.Factory(container.repository),
        )[GoldShopViewModel::class.java]

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            AbuIshaqGoldTheme(darkTheme = settings.isDarkTheme) {
                AbuIshaqNavigationApp(viewModel = viewModel)
            }
        }
    }
}
