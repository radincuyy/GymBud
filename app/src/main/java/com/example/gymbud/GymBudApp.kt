package com.example.gymbud

import android.app.Application
import com.example.gymbud.utils.ThemeManager

class GymBudApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize and apply theme settings globally at application startup
        ThemeManager(this).applyTheme()
    }
}
