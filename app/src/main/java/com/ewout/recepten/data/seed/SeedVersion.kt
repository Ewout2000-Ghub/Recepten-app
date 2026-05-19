package com.ewout.recepten.data.seed

/**
 * Verhoog deze waarde wanneer recepten_seed.json wijzigt. Bij de volgende
 * app-start zal SeedLoader het bestand opnieuw inlezen en SEED-records
 * upserten. USER-records (en SEED-records die naar USER zijn gepromoveerd
 * door een bewerking) blijven ongemoeid.
 */
const val SEED_VERSION: Int = 1
