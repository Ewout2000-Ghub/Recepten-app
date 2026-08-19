package com.ewout.recepten.data

import com.ewout.recepten.data.local.RecipeDao
import com.ewout.recepten.data.local.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecipeRepository(private val dao: RecipeDao) {

    fun observeAll(): Flow<List<Recipe>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeById(id: String): Flow<Recipe?> =
        dao.observeById(id).map { it?.toDomain() }

    /** Alle versies van het gerecht waartoe [groepSleutel] behoort. */
    fun observeGroup(groepSleutel: String): Flow<List<Recipe>> =
        dao.observeGroup(groepSleutel).map { list -> list.map { it.toDomain() } }
}
