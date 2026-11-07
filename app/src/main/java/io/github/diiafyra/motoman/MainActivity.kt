package io.github.diiafyra.motoman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.diiafyra.motoman.ui.MotoManApp
import io.github.diiafyra.motoman.ui.designsystem.theme.MotoManTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotoManTheme {
                MotoManApp()
            }
        }
    }
}
