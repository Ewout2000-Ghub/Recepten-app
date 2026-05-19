# Recepten App — Claude Code Prompt

Bouw een Android app voor persoonlijk gebruik op een Samsung Galaxy S24+. De app wordt niet gepubliceerd, alleen lokaal geïnstalleerd via Android Studio. Hieronder de volledige specs. Volg ze nauwkeurig.

## Tech stack

- **Taal**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Min SDK**: 28, **Target SDK**: 35
- **Architectuur**: MVVM met een `RecipeRepository` als single source of truth
- **Persistentie**: Room database (SQLite) voor alle recepten + JSON seed file in `assets/` als bron voor SEED-recepten
- **Navigatie**: Navigation Compose (één Activity)
- **DI**: Geen Hilt nodig, simpele manual DI via een `AppContainer` op Application-niveau
- **JSON-parsing**: `kotlinx.serialization` (geen Gson of Moshi)
- **Build**: Gradle Kotlin DSL, Compose BOM

## Functionaliteit (v1)

1. **Receptenlijst** — startscherm met alle recepten, gegroepeerd op categorie (collapsible sections). Per receptkaart: naam, categorie-chip, en max. 3 ingrediënten als preview. Loading-state tonen tot Room klaar is met seeden bij eerste start.
2. **Detailpagina** — naam, porties (indien aanwezig), categorie-chip, ingrediëntenlijst (met hoeveelheid + eenheid net naast de naam), en de bereidingswijze als genummerde stappen. Als `bereidingswijze` leeg is: toon "Bereidingswijze nog niet ingevuld" in een subtiele lege staat. Edit- en delete-actie in de top app bar (zie punt 6).
3. **Zoeken/filteren op ingrediënt** — zoekbalk bovenaan het lijstscherm. Filtert recepten waarbij minstens één ingrediënt-naam (case-insensitive, substring match) overeenkomt. Toon ook waarop gematcht is.
4. **Categorieën** — recepten hebben een categorie-veld (bijv. "Pasta", "Kip", "Ovenschotel", "Aziatisch", "Stamppot"). Op de lijst kunnen categorieën worden in-/uitgeklapt. Aparte categorie-filter via chips bovenaan (multi-select). Categorie-volgorde in de lijst is alfabetisch.
5. **Recept handmatig toevoegen** — FAB op het lijstscherm opent een formulier: naam, categorie (dropdown met bestaande + vrije invoer), porties (optioneel), ingrediëntenlijst (rijen met +/− knop, per rij: naam, hoeveelheid, eenheid), bereidingswijze (lijst van stappen, +/− knop). Opslaan schrijft naar Room met `bron = USER`.
6. **Recept bewerken en verwijderen** — vanuit de detailpagina. Edit opent hetzelfde formulier als bij toevoegen, gevuld met de huidige waarden. Delete vraagt om bevestiging (AlertDialog). **Beide acties zijn voor zowel USER als SEED recepten beschikbaar.** Een bewerkte SEED wordt automatisch een USER-record (bron-veld bijgewerkt naar USER), zodat een re-seed je wijzigingen niet overschrijft.

Handmatig toegevoegde recepten en seed-recepten worden in de UI gemixt; het verschil is alleen waar ze vandaan komen.

## Data model

Eén `Recipe` data class, identiek voor seed JSON en Room. Velden:

```kotlin
@Serializable
data class Recipe(
    val id: String,                              // slug, bv. "tikka-massala-curry"
    val naam: String,
    val categorie: String,
    val porties: String? = null,                 // bv. "4-6 personen", optioneel
    val ingredienten: List<Ingredient>,
    val bereidingswijze: List<String>,           // genummerde stappen
    val bron: Bron = Bron.USER                   // standaardwaarde voor JSON-parsing
)

@Serializable
data class Ingredient(
    val naam: String,
    val hoeveelheid: Double? = null,             // null = "naar smaak" / niet gespecificeerd
    val eenheid: String? = null                  // vrije tekst, GEEN enum (zie hieronder)
)

@Serializable
enum class Bron { SEED, USER }
```

**Belangrijke regels**:

- `eenheid` is **vrije tekst**, geen enum. Gebruikte waarden tot nu toe: `g`, `kg`, `ml`, `l`, `stuk`, `teen`, `el`, `tl`. Nieuwe waarden mogen er gewoon bij komen zonder code-wijziging.
- `hoeveelheid: null` betekent "geen specifieke hoeveelheid" (bv. "kokosmelk", "olijfolie", "naar smaak"). In de UI: toon alleen de ingrediëntnaam, zonder getal, zonder streepje, zonder placeholder.
- `hoeveelheid` met `eenheid: null` betekent een puur getal (zeldzaam). Toon dan alleen het getal.
- Getallen mooi formatteren: `1.0` → "1", `0.5` → "0,5", `1.5` → "1,5" (Nederlandse decimaal met komma). Consistent door de hele app.
- Het JSON seed-bestand bevat **geen** `bron`-veld. Tijdens parsing in `SeedLoader` wordt elk Recipe-object opnieuw opgebouwd met `bron = Bron.SEED` voor het in Room wordt opgeslagen.

## Seed-mechanisme — manier waarop nieuwe recepten worden toegevoegd

Plaats `recepten_seed.json` in `app/src/main/assets/`. Het seed-mechanisme werkt zo:

1. In code staat een constant `SEED_VERSION: Int` (begin op `1`).
2. In SharedPreferences wordt de laatst toegepaste versie bewaard onder key `applied_seed_version`.
3. Bij elke app-start vergelijkt `SeedLoader` deze twee. Is de constant hoger dan de opgeslagen waarde, dan wordt de JSON opnieuw ingeladen.
4. Bij re-seed: SEED-records worden overschreven op basis van `id` (upsert). USER-records worden nooit aangeraakt. SEED-records die niet meer in de JSON staan worden verwijderd uit Room.
5. Na succesvolle seed wordt `applied_seed_version` op de huidige `SEED_VERSION` gezet.

**Dit is de uitbreidingsweg voor mij/Claude Code**: nieuwe recepten worden toegevoegd door een entry toe te voegen aan `recepten_seed.json` met exact dezelfde structuur, en `SEED_VERSION` met 1 te verhogen. Geen andere code-wijzigingen nodig. Het JSON-formaat:

```json
{
  "recepten": [
    {
      "id": "broccoli-ovenschotel",
      "naam": "Broccoli ovenschotel",
      "categorie": "Ovenschotel",
      "porties": null,
      "ingredienten": [
        { "naam": "broccoli (roosjes)", "hoeveelheid": 500, "eenheid": "g" },
        { "naam": "ui (gesnipperd)", "hoeveelheid": 1, "eenheid": "stuk" },
        { "naam": "olijfolie", "hoeveelheid": null, "eenheid": null }
      ],
      "bereidingswijze": [
        "Verwarm de oven voor op 175 graden boven- en onderwarmte.",
        "Kook de broccoli in ca. 5 minuten beetgaar."
      ]
    }
  ]
}
```

Het volledige seed-bestand met 14 recepten lever ik separaat aan (bijgevoegd als `recepten_seed.json`). Gebruik dat bestand als initiële inhoud van `app/src/main/assets/recepten_seed.json`.

## Design / styling

- **Achtergrond**: zachte crème, `#FAF6EF`
- **Primaire kleur (Claude oranje)**: `#D97757` voor knoppen, chips, accenten, FAB, top app bar
- **Secundair / oppervlak verhoogd**: `#FFFFFF` of `#F3ECDF` voor kaarten
- **Tekst**: `#2B2A27` (donker warm grijs), secundair `#6B665C`
- **Typografie**: Material 3 standaard, maar headings (recept-naam) in een serif font (gebruik `Playfair Display` of fallback `serif`). Body in default sans.
- **Vormgeving**: ruime padding (16-20dp), kaarten met afgeronde hoeken (16dp), zachte schaduwen, geen harde lijnen
- **Iconen**: Material Icons Extended
- **Donkere modus**: niet nodig voor v1, alleen light theme

## Project structuur

```
app/src/main/java/com/ewout/recepten/
├── RecipeApplication.kt          (geregistreerd in AndroidManifest.xml als android:name)
├── AppContainer.kt
├── data/
│   ├── Recipe.kt
│   ├── Ingredient.kt
│   ├── Bron.kt
│   ├── local/
│   │   ├── RecipeDatabase.kt
│   │   ├── RecipeDao.kt
│   │   ├── RecipeEntity.kt        (+ converters voor List<Ingredient>/List<String>)
│   │   └── Converters.kt
│   ├── seed/
│   │   ├── SeedLoader.kt          (leest assets/recepten_seed.json en upsert in Room)
│   │   └── SeedVersion.kt         (SEED_VERSION constant)
│   └── RecipeRepository.kt
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── list/
│   │   ├── RecipeListScreen.kt
│   │   └── RecipeListViewModel.kt
│   ├── detail/
│   │   ├── RecipeDetailScreen.kt
│   │   └── RecipeDetailViewModel.kt
│   └── edit/                       (zelfde scherm voor add én edit)
│       ├── RecipeEditScreen.kt
│       └── RecipeEditViewModel.kt
└── MainActivity.kt
```

## Acceptatiecriteria

- App start, laadt 14 seed-recepten in op eerste run, toont ze gegroepeerd per categorie.
- Loading-state zichtbaar tijdens initiële seed; daarna content.
- Tikken op een recept opent detail; de broccoli ovenschotel, tikka massala, Oer-Leidse hutspot, Spaanse tomatenstoof, orzo, quiche, zuurkoolstamppot en gele rijst met chorizo hebben een volledige bereidingswijze.
- Ingrediënten zonder hoeveelheid (bv. "kokosmelk", "olijfolie") tonen alleen de naam.
- Zoeken op "knoflook" toont alle recepten met knoflook in de ingrediëntenlijst.
- Categoriefilter-chips filteren correct.
- FAB → formulier → opslaan → recept verschijnt in lijst onder de juiste categorie als USER.
- Vanuit detail: edit werkt voor USER en SEED (een bewerkte SEED wordt automatisch USER).
- Vanuit detail: delete vraagt om bevestiging en verwijdert het recept.
- `SEED_VERSION` verhogen + JSON aangepast → bij volgende app-start verschijnen wijzigingen, USER-recepten en bewerkte (voorheen) SEED-recepten blijven intact.
- Alle teksten in de UI zijn Nederlands.
- Build draait zonder warnings op Android Studio Hedgehog of nieuwer, target Galaxy S24+ (API 34/35).

## Wat ik níet wil

- Geen cloud sync, geen accounts, geen analytics, geen crashlytics.
- Geen externe afbeeldingen of placeholder images van internet.
- Geen "ik" of AI-achtige flavor text in de UI. Korte, neutrale Nederlandse labels.
- Geen unit tests of UI tests; persoonlijk gebruik.

## Levering

1. Volledige projectstructuur, ready to open in Android Studio.
2. `recepten_seed.json` in `app/src/main/assets/` met de 14 recepten die ik meegeef.
3. Correct geregistreerde `RecipeApplication` in `AndroidManifest.xml`.
4. Korte `README.md` in de root met:
   - Hoe builden en installeren via Android Studio
   - Hoe een recept toevoegen via JSON (incl. `SEED_VERSION` verhogen)
   - Hoe via de UI toevoegen, bewerken, verwijderen
   - Korte uitleg van het SEED vs USER mechanisme

Begin met de Gradle setup en projectstructuur, daarna data laag (Room + seed loader), dan UI. Stel vragen als iets onduidelijk is voordat je grote keuzes maakt.