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

## Een recept toevoegen/wijzigen (de standaardtaak)

1. Bewerk [app/src/main/assets/recepten_seed.json](app/src/main/assets/recepten_seed.json).
   Zelfde structuur als bestaande entries. `bron` weglaten. Sausonderdelen zonder
   eigen groepsveld: markeer met een suffix in de naam, bv. `"mosterd (sausje)"`.
2. Verhoog `SEED_VERSION` met 1 in
   [SeedVersion.kt](app/src/main/java/com/ewout/recepten/data/seed/SeedVersion.kt).
   **Dit is verplicht** — zonder ophoging leest de app de nieuwe JSON niet in.
3. De gebruiker build/installeert. Bij start vergelijkt
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
