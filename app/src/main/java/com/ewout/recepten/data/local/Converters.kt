package com.ewout.recepten.data.local

import androidx.room.TypeConverter
import com.ewout.recepten.data.Bron
import com.ewout.recepten.data.Ingredient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class Converters {

    @TypeConverter
    fun ingredientsToJson(value: List<Ingredient>): String =
        json.encodeToString(ListSerializer(Ingredient.serializer()), value)

    @TypeConverter
    fun jsonToIngredients(value: String): List<Ingredient> =
        json.decodeFromString(ListSerializer(Ingredient.serializer()), value)

    @TypeConverter
    fun stringsToJson(value: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun jsonToStrings(value: String): List<String> =
        json.decodeFromString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun bronToString(value: Bron): String = value.name

    @TypeConverter
    fun stringToBron(value: String): Bron = Bron.valueOf(value)
}
