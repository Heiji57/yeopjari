package com.yeopjari.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.yeopjari.app.ui.YeopjariApp
import com.yeopjari.app.ui.YeopjariTheme
import com.yeopjari.core.model.DriverMode

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YeopjariTheme(senior = vm.profile.mode == DriverMode.SENIOR) {
                YeopjariApp(vm)
            }
        }
    }
}
