package com.ewout.recepten.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ewout.recepten.data.Bron
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY naam COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecipeEntity?

    @Query("SELECT id FROM recipes WHERE bron = :bron")
    suspend fun idsByBron(bron: Bron): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteById(id: String)
}
