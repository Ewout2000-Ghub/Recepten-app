package com.ewout.recepten.data

import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
    val id: String,
    val naam: String,
    val categorie: String,
    val porties: String? = null,
    val ingredienten: List<Ingredient>,
    val bereidingswijze: List<String>,
    val bron: Bron = Bron.USER
)
