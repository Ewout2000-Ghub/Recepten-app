package com.ewout.recepten.data.seed

import android.content.Context
import com.ewout.recepten.data.Bron
import com.ewout.recepten.data.Recipe
import com.ewout.recepten.data.local.RecipeDao
import com.ewout.recepten.data.local.toEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SeedPayload(val recepten: List<Recipe>)

class SeedLoader(
    private val context: Context,
    private val dao: RecipeDao
) {

    private val prefs by lazy {
        context.getSharedPreferences("seed_prefs", Context.MODE_PRIVATE)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Roept seeding aan als de huidige SEED_VERSION hoger is dan de waarde
     * die we lokaal hebben opgeslagen. USER-records worden nooit geraakt.
     */
    suspend fun maybeSeed() {
        val applied = prefs.getInt(KEY_APPLIED_VERSION, 0)
        if (applied >= SEED_VERSION) return
        runSeed()
        prefs.edit().putInt(KEY_APPLIED_VERSION, SEED_VERSION).apply()
    }

    private suspend fun runSeed() {
        val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val payload = json.decodeFromString(SeedPayload.serializer(), raw)

        val seedRecipes = payload.recepten.map { it.copy(bron = Bron.SEED) }
        val newSeedIds = seedRecipes.map { it.id }.toSet()

        val existingSeedIds = dao.idsByBron(Bron.SEED).toSet()
        val obsolete = existingSeedIds - newSeedIds
        obsolete.forEach { dao.deleteById(it) }

        seedRecipes.forEach { dao.upsert(it.toEntity()) }
    }

    companion object {
        private const val KEY_APPLIED_VERSION = "applied_seed_version"
        private const val ASSET_NAME = "recepten_seed.json"
    }
}
