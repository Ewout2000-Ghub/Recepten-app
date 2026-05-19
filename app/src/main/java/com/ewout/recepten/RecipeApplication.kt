package com.ewout.recepten

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class RecipeApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope: CoroutineScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        applicationScope.launch(Dispatchers.IO) {
            container.runSeedIfNeeded()
        }
    }
}
