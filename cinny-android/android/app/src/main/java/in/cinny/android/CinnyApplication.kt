package `in`.cinny.android

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import `in`.cinny.android.data.CinnyRepository
import uniffi.cinny_core.initCinnyLogging

class CinnyApplication : Application() {
    lateinit var repository: CinnyRepository
        private set

    override fun onCreate() {
        super.onCreate()
        initCinnyLogging()
        repository = CinnyRepository(applicationContext)
        repository.restoreSavedSessionIfAvailable()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                repository.resumeSync()
            }

            override fun onStop(owner: LifecycleOwner) {
                repository.pauseSync()
            }
        })
    }
}
