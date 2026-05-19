package com.ewout.recepten.data

import com.ewout.recepten.data.local.RecipeDao
import com.ewout.recepten.data.local.toDomain
import com.ewout.recepten.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecipeRepository(private val dao: RecipeDao) {

    fun observeAll(): Flow<List<Recipe>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): Recipe? = dao.getById(id)?.toDomain()

    suspend fun upsert(recipe: Recipe) {
        dao.upsert(recipe.toEntity())
    }

    suspend fun saveUserEdit(recipe: Recipe) {
        dao.upsert(recipe.copy(bron = Bron.USER).toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}
