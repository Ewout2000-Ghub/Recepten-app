package com.ewout.recepten.data

import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val naam: String,
    val hoeveelheid: Double? = null,
    val eenheid: String? = null
)
