package com.ewout.recepten.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ewout.recepten.data.Bron
import com.ewout.recepten.data.Ingredient
import com.ewout.recepten.data.Recipe

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val naam: String,
    val categorie: String,
    val porties: String?,
    val ingredienten: List<Ingredient>,
    val bereidingswijze: List<String>,
    val bron: Bron,
    val groepId: String? = null,
    val versieNaam: String? = null
)

fun RecipeEntity.toDomain(): Recipe = Recipe(
    id = id,
    naam = naam,
    categorie = categorie,
    porties = porties,
    ingredienten = ingredienten,
    bereidingswijze = bereidingswijze,
    bron = bron,
    groepId = groepId,
    versieNaam = versieNaam
)

fun Recipe.toEntity(): RecipeEntity = RecipeEntity(
    id = id,
    naam = naam,
    categorie = categorie,
    porties = porties,
    ingredienten = ingredienten,
    bereidingswijze = bereidingswijze,
    bron = bron,
    groepId = groepId,
    versieNaam = versieNaam
)
