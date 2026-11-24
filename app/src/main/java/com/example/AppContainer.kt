package com.example

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.remote.GoldPriceService
import com.example.data.repository.GoldRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val preferences = AppPreferences(appContext)
    private val goldPriceService = GoldPriceService(appContext)

    val repository = GoldRepository(
        db = database,
        prefs = preferences,
        service = goldPriceService,
    )
}
