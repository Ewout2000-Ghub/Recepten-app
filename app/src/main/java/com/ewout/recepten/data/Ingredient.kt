package com.ewout.recepten.data

import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val naam: String,
    val hoeveelheid: Double? = null,
    val eenheid: String? = null,
    /** Optionele ingrediënten staan in de UI onder een apart kopje "Optioneel". */
    val optioneel: Boolean = false
)
