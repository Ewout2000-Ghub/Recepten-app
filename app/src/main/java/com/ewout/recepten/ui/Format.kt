package com.ewout.recepten.ui

import com.ewout.recepten.data.Ingredient

/**
 * Formatteert een Double volgens Nederlandse conventie:
 *  - 1.0 → "1"
 *  - 0.5 → "0,5"
 *  - 1.5 → "1,5"
 *  - 0.25 → "0,25"
 */
fun formatNumberNl(value: Double): String {
    if (value % 1.0 == 0.0) return value.toLong().toString()
    // Verwijder onnodige nullen achteraan en gebruik komma als decimaalteken.
    val raw = value.toBigDecimal().stripTrailingZeros().toPlainString()
    return raw.replace('.', ',')
}

/**
 * Formatteert de hoeveelheid + eenheid die naast de naam getoond worden.
 * Lege string betekent: niets tonen.
 */
fun formatHoeveelheid(ingredient: Ingredient): String {
    val getal = ingredient.hoeveelheid ?: return ""
    val getalText = formatNumberNl(getal)
    val eenheid = ingredient.eenheid?.trim().orEmpty()
    return if (eenheid.isEmpty()) getalText else "$getalText $eenheid"
}
