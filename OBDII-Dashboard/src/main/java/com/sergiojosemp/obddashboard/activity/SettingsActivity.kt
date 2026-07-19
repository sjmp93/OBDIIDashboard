package com.sergiojosemp.obddashboard.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material3.MaterialTheme
import com.sergiojosemp.obddashboard.ui.settings.SettingsScreen
import com.sergiojosemp.obddashboard.vm.SettingsViewModel

class SettingsActivity : AppCompatActivity() {

    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onNavigateBack = { finish() },
                    viewModel = viewModel
                )
            }
        }
    }
}
