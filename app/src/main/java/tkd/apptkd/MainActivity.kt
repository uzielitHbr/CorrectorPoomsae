package tkd.apptkd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import tkd.apptkd.ui.navigation.AppNavigation
import tkd.apptkd.ui.theme.TaegeukCoachTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaegeukCoachTheme {
                AppNavigation()
            }
        }
    }
}
