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
    val bron: Bron = Bron.USER,
    /**
     * Recepten met dezelfde [groepId] zijn versies van hetzelfde gerecht.
     * null = dit recept staat op zichzelf (effectieve groep = [id]).
     * De versie waarvan [id] gelijk is aan [groepId] is de "anker"-versie
     * die de groep in de lijst vertegenwoordigt.
     */
    val groepId: String? = null,
    /** Korte naam van deze versie, bijv. "Origineel". Null = toon [naam]. */
    val versieNaam: String? = null,
    /** Vegetarisch (geen vlees/vis). Vegan-recepten zijn ook vega. */
    val vega: Boolean = false,
    /** Veganistisch (geen dierlijke producten). */
    val vegan: Boolean = false
) {
    /** De sleutel waarop versies van hetzelfde gerecht worden gegroepeerd. */
    val groepSleutel: String get() = groepId ?: id
}
