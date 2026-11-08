package io.github.diiafyra.motoman

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.diiafyra.motoman.firebase.FirebaseSetup
import io.github.diiafyra.motoman.firebase.RemoteConfigRefresher
import io.github.diiafyra.motoman.work.AndroidReminderNotifier
import io.github.diiafyra.motoman.work.ReminderWorker
import javax.inject.Inject

@HiltAndroidApp
class MotoManApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var remoteConfig: RemoteConfigRefresher

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.FIREBASE_ENABLED) {
            FirebaseSetup.configure(this, enabled = !BuildConfig.DEBUG)
            remoteConfig.refresh()
        }
        AndroidReminderNotifier.createChannel(this)
        ReminderWorker.schedule(this)
    }
}
