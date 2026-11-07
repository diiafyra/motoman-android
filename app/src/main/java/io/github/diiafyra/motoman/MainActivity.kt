package io.github.diiafyra.motoman

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.diiafyra.motoman.ui.MotoManApp
import io.github.diiafyra.motoman.ui.designsystem.theme.MotoManTheme

// AppCompatActivity rather than ComponentActivity: it applies the per-app language on Android 12 and below.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
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
