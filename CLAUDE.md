# CLAUDE.md

Instructies voor Claude bij het werken in deze repo. Zie ook [README.md](README.md)
voor de mensvriendelijke versie.

## Wat dit is

Persoonlijke Android-app om recepten op te zoeken, te bewerken en toe te voegen.
**Puur lokaal** op Ewouts Samsung Galaxy S24+ — geen cloud, geen backend, geen
distributie, geen Play Store. Alle data staat in een lokale Room/SQLite-database
op het toestel. Communicatie met de gebruiker gaat in het **Nederlands**.

## Tech

- Kotlin + Jetpack Compose (Material 3), één Activity + Navigation Compose
- Min SDK 28, target/compile SDK 35, JVM 17, `applicationId = com.ewout.recepten`
- Room (SQLite) via KSP, kotlinx.serialization voor JSON
- MVVM, handmatige DI via `AppContainer` op Application-niveau
- Alleen portret; op de detailpagina blijft het scherm aan

## Builden — belangrijk

**Op deze machine kan NIET gebouwd worden.** Er is geen Gradle wrapper JAR en geen
lokale Gradle-installatie. Compileren/verifiëren kan alleen de gebruiker, via
**Android Studio** (File → Open → deze map → Run 'app' op het aangesloten toestel).

Claude dus: geen `gradle`/`./gradlew` proberen. Wijzigingen zorgvuldig en
compileerbaar aanleveren; de gebruiker test op het toestel.

## Datamodel

`Recipe` ([Recipe.kt](app/src/main/java/com/ewout/recepten/data/Recipe.kt)):

| Veld | Type | Opmerking |
|---|---|---|
| `id` | String | uniek, kebab-case |
| `naam` | String | |
| `categorie` | String | zie categorieën |
| `porties` | String? | bv. `"4 personen"`; eerste getal = basis voor portie-schalen (default 4) |
| `ingredienten` | List\<Ingredient\> | |
| `bereidingswijze` | List\<String\> | één stap per element, mag leeg zijn |
| `bron` | Bron | `SEED` of `USER` — bij JSON weglaten, SeedLoader zet 'm op `SEED` |
| `groepId` | String? | recepten met dezelfde `groepId` zijn versies van één gerecht |
| `versieNaam` | String? | korte versienaam, bv. `"Origineel"` |
| `vega` | Boolean | vegetarisch (geen vlees/vis); default `false`. Zet bij vegan óók `true` |
| `vegan` | Boolean | veganistisch (geen dierlijke producten); default `false` |

`vega`/`vegan` voeden de dieet-filterknoppen ("Vega"/"Vegan") bovenaan de lijst
([RecipeListViewModel.kt](app/src/main/java/com/ewout/recepten/ui/list/RecipeListViewModel.kt),
`DietFilter`). Het zijn **geen** categorieën — recepten blijven in hun eigen
categorie; het filter staat standaard uit. Opgeslagen als Room-kolommen (DB v3);
bij een nieuw dieet-veld een migratie toevoegen zoals `MIGRATION_2_3` in
[RecipeDatabase.kt](app/src/main/java/com/ewout/recepten/data/local/RecipeDatabase.kt).

`Ingredient` ([Ingredient.kt](app/src/main/java/com/ewout/recepten/data/Ingredient.kt)):
`naam: String`, `hoeveelheid: Double? = null`, `eenheid: String? = null`.
`hoeveelheid: null` = "naar smaak" → UI toont alleen de naam. `eenheid` is vrije
tekst (`g`, `kg`, `ml`, `l`, `stuk`, `teen`, `el`, `tl`, `takjes`, `bol`, …).

### Categorieën (data-gedreven, op koolhydraatbasis)

`categorie` is vrije tekst. Er is **geen** vaste lijst in code — de app leidt de
categorieën dynamisch af uit de `categorie`-velden van alle recepten
([RecipeListViewModel.kt](app/src/main/java/com/ewout/recepten/ui/list/RecipeListViewModel.kt),
`allCategories`). Een nieuwe waarde verschijnt vanzelf als groep, filterchip en
invoersuggestie; er is geen categorie-specifieke kleur/icoon/volgorde (alfabetisch
gesorteerd). Gangbare waarden: `Pasta`, `Aardappel`, `Rijst`, `Couscous`, `Anders`.
Kies op basis van de zetmeelbasis; hergebruik een bestaande waarde tenzij het
gerecht echt een nieuwe basis heeft. Twijfelgevallen: benoem de keuze richting de
gebruiker.

### Versies van hetzelfde gerecht

Meerdere `Recipe`-entries met dezelfde `groepId` vormen één gerecht met een
versie-switcher op de detailpagina. De versie waarvan `id == groepId` is de
ankerversie die de groep in de lijst representeert. Geef elke versie een
`versieNaam`. Voorbeeld: de twee "Pasta bolognese"-entries in de seed.

## Recepten toevoegen (de meest voorkomende taak)

Dit is verreweg de vaakst gevraagde taak: de gebruiker vindt leuke recepten
(als losse tekst of als **link(s)** naar receptensites) en vraagt ze toe te
voegen. Werk dan dit stappenplan af:

1. **Verzamel de bron.** Bij een URL: haal de pagina op met `WebFetch` en vraag
   naam, porties, ingrediënten (met hoeveelheid + eenheid) en de stappen. Blokkeert
   een site `WebFetch` met **HTTP 403** (bv. `eefkooktzo.nl`), haal de HTML dan op
   met `curl -sL -A "<browser user-agent>"` naar een tijdelijk bestand en lees het
   recept uit de JSON-LD (`recipeIngredient`, `recipeInstructions`, `recipeYield`).
   Ruim het tijdelijke bestand daarna op. **Nooit** recepten verzinnen — ontbreekt
   de inhoud, vraag er expliciet om.
2. **Zet per recept een JSON-entry** in
   [recepten_seed.json](app/src/main/assets/recepten_seed.json), zelfde structuur en
   stijl als bestaande entries (2-spaces, veldvolgorde `id, naam, categorie, porties,
   ingredienten, bereidingswijze`). `bron` **weglaten** (SeedLoader zet 'm op SEED).
   - `id`: kebab-case, uniek, afgeleid van de naam (accenten normaliseren).
   - `categorie`: op koolhydraatbasis (zie categorieën). Twijfel? **Benoem de keuze
     richting de gebruiker** en gebruik bij voorkeur een bestaande waarde.
   - `porties`: neem over van de bron (bv. `"2 personen"`, `"4 personen"`).
   - Ingrediënten: `hoeveelheid` als getal, of `null` = "naar smaak"/onbepaald;
     `eenheid` vrije tekst. Sausonderdelen zonder eigen groepsveld: suffix in de
     naam, bv. `"mosterd (sausje)"`.
   - `bereidingswijze`: één stap per array-element; herschrijf beknopt in nette,
     hele Nederlandse zinnen (getallen zoals oventemperatuur/tijden behouden).
   - `vega`/`vegan`: bepaal uit de ingrediënten. Vlees/vis → beide weglaten
     (default false). Vegetarisch (met kaas/ei/honing) → `"vega": true`. Zonder
     enig dierlijk product → `"vega": true` én `"vegan": true`.
3. **Verhoog `SEED_VERSION` met 1** in
   [SeedVersion.kt](app/src/main/java/com/ewout/recepten/data/seed/SeedVersion.kt).
   **Verplicht** — zonder ophoging leest de app de nieuwe JSON niet in.
4. **Commit & push** in het Nederlands (zie Conventies) — direct op `main`, zoals de
   git-historie laat zien. Data-commit en eventuele docs-commit gescheiden houden.
5. De gebruiker build/installeert via Android Studio. Bij start vergelijkt
   [SeedLoader.kt](app/src/main/java/com/ewout/recepten/data/seed/SeedLoader.kt)
   `SEED_VERSION` met `applied_seed_version` in SharedPreferences. Is hij hoger:
   alle SEED-records worden ge-upsert op `id`; SEED-records die niet meer in de
   JSON staan worden verwijderd; USER-records blijven intact.

### SEED vs USER

`SEED` = kwam uit de JSON. `USER` = handmatig toegevoegd/bewerkt via de UI. Een via
de UI bewerkt SEED-recept wordt automatisch USER, zodat re-seeding zijn wijzigingen
niet overschrijft. Re-seeding raakt USER-records nooit.

## Projectstructuur

```
app/src/main/
├── assets/recepten_seed.json          ← recepten-data
└── java/com/ewout/recepten/
    ├── RecipeApplication.kt, AppContainer.kt, MainActivity.kt
    ├── data/
    │   ├── Recipe.kt, Ingredient.kt, Bron.kt, RecipeRepository.kt
    │   ├── local/   (Database, DAO, Entity, Converters)
    │   └── seed/    (SeedLoader, SeedVersion)
    └── ui/
        ├── theme/, list/, detail/, edit/, Format.kt
```

## Conventies

- Alles wat de gebruiker ziet én commit messages in het **Nederlands**.
- JSON-stijl volgen van bestaande entries (2-spaces, veldvolgorde).
- Commit messages beschrijvend, in de stijl van de bestaande git-historie.
