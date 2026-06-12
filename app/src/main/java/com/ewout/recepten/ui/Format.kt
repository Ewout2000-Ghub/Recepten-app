package com.ewout.recepten.ui

import com.ewout.recepten.data.Ingredient

/**
 * Formatteert een Double volgens Nederlandse conventie, afgerond op maximaal
 * twee decimalen (relevant bij geschaalde hoeveelheden):
 *  - 1.0 → "1"
 *  - 0.5 → "0,5"
 *  - 466.6667 → "466,67"
 */
fun formatNumberNl(value: Double): String {
    val afgerond = value.toBigDecimal()
        .setScale(2, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros()
    return afgerond.toPlainString().replace('.', ',')
}

/**
 * Formatteert de hoeveelheid + eenheid die naast de naam getoond worden,
 * vermenigvuldigd met [factor] (porties schalen). Lege string betekent:
 * niets tonen.
 */
fun formatHoeveelheid(ingredient: Ingredient, factor: Double = 1.0): String {
    val getal = ingredient.hoeveelheid ?: return ""
    val getalText = formatNumberNl(getal * factor)
    val eenheid = ingredient.eenheid?.trim().orEmpty()
    return if (eenheid.isEmpty()) getalText else "$getalText $eenheid"
}
