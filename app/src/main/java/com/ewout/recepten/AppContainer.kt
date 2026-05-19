package com.ewout.recepten

import android.content.Context
import com.ewout.recepten.data.RecipeRepository
import com.ewout.recepten.data.local.RecipeDatabase
import com.ewout.recepten.data.seed.SeedLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val database: RecipeDatabase by lazy { RecipeDatabase.getInstance(appContext) }

    val repository: RecipeRepository by lazy { RecipeRepository(database.recipeDao()) }
    private val seedLoader: SeedLoader by lazy { SeedLoader(appContext, database.recipeDao()) }

    private val _seedReady = MutableStateFlow(false)
    val seedReady: StateFlow<Boolean> = _seedReady.asStateFlow()

    suspend fun runSeedIfNeeded() {
        seedLoader.maybeSeed()
        _seedReady.value = true
    }
}
