# Recepten

Persoonlijke Android-app voor het opzoeken, bewerken en toevoegen van recepten.
Niet bedoeld voor distributie — alleen lokaal builden en installeren via Android
Studio op een Samsung Galaxy S24+.

## Tech

- Kotlin + Jetpack Compose (Material 3)
- Min SDK 28, Target SDK 35
- AGP 9 / Gradle 9.1 / Kotlin 2.2 / Room 2.8
- Room (SQLite) voor persistentie, kotlinx.serialization voor JSON
- MVVM met manual DI via `AppContainer` op Application-niveau
- Navigation Compose (één Activity)
- Alleen staande modus (portrait); op de detailpagina blijft het scherm aan

## Functies op de detailpagina

- **Porties schalen** — met de +/− stepper pas je het aantal personen aan;
  alle hoeveelheden schalen mee. De basis is het eerste getal in het
  `porties`-veld (standaard 4 als het veld ontbreekt).
- **Afvinken tijdens het koken** — tik op een ingrediënt of stap om hem af te
  strepen. Vinkjes resetten zodra je het recept verlaat.

Zoeken op het lijstscherm matcht zowel de receptnaam als ingrediëntnamen
(case-insensitive, substring).

## Builden en installeren via Android Studio

1. Open Android Studio Hedgehog of nieuwer.
2. Kies **File → Open** en selecteer deze projectmap.
3. Laat Gradle synchroniseren. Eerste sync downloadt SDK-componenten en
   dependencies; dit kan een paar minuten duren.
4. Sluit een Samsung Galaxy S24+ aan via USB met *USB-debugging* ingeschakeld
   (Ontwikkelaarsopties).
5. Kies het apparaat als deployment target en klik **Run 'app'**
   (⇧F10 / Shift+F10).

> Geen Gradle Wrapper JAR meegeleverd. Android Studio gebruikt zijn eigen
> meegeleverde Gradle voor builds in de IDE. Als je vanaf de command line wilt
> bouwen, voer dan in de projectroot eenmalig `gradle wrapper` uit (vereist een
> lokale Gradle ≥ 8.10).

## SEED vs USER

Recepten hebben een veld `bron`:

- **SEED** — kwam uit `app/src/main/assets/recepten_seed.json`.
- **USER** — handmatig toegevoegd of bewerkt via de UI.

USER-recepten worden nooit overschreven door een re-seed. Een SEED-recept dat je
via de UI bewerkt wordt automatisch een USER-recept, zodat een latere update van
`recepten_seed.json` jouw wijzigingen niet kapot maakt.

### Een recept toevoegen via JSON (door mij/Claude)

1. Voeg een entry toe aan `app/src/main/assets/recepten_seed.json` met dezelfde
   structuur als bestaande recepten. Het `bron`-veld laat je weg; SeedLoader zet
   het tijdens parsing op `SEED`.
2. Verhoog `SEED_VERSION` met 1 in
   `app/src/main/java/com/ewout/recepten/data/seed/SeedVersion.kt`.
3. Build en installeer. Bij start vergelijkt `SeedLoader` de constant met
   `applied_seed_version` in SharedPreferences. Is hij hoger, dan worden alle
   SEED-records ge-upsert op `id`. SEED-records die niet meer in de JSON staan
   worden verwijderd. USER-records (incl. bewerkte SEED → USER) blijven intact.

JSON-formaat per recept:

```json
{
  "id": "broccoli-ovenschotel",
  "naam": "Broccoli ovenschotel",
  "categorie": "Aardappel",
  "porties": "4 personen",
  "ingredienten": [
    { "naam": "broccoli (roosjes)", "hoeveelheid": 500, "eenheid": "g" },
    { "naam": "ui (gesnipperd)", "hoeveelheid": 1, "eenheid": "stuk" },
    { "naam": "olijfolie", "hoeveelheid": null, "eenheid": null }
  ],
  "bereidingswijze": [
    "Verwarm de oven voor op 175 graden boven- en onderwarmte."
  ]
}
```

Regels:

- `categorie`: vrije tekst, ingedeeld naar koolhydraatbasis. Categorieën worden
  dynamisch afgeleid uit de recepten zelf (geen vaste lijst in code). Gangbare
  waarden: **Pasta**, **Aardappel**, **Rijst**, **Couscous**, **Anders**. Gebruik
  een bestaande waarde tenzij een gerecht echt een nieuwe basis heeft; een nieuwe
  `categorie`-waarde verschijnt automatisch als groep, filter en suggestie.
- `hoeveelheid: null` = "naar smaak" / niet gespecificeerd → UI toont alleen
  de naam.
- `eenheid` is **vrije tekst** — niet beperkt tot een enum. Veelgebruikte
  waarden: `g`, `kg`, `ml`, `l`, `stuk`, `teen`, `el`, `tl`.
- `porties` is optioneel; gebruik standaard `"4 personen"`. Het eerste getal
  erin is de basis voor het schalen van hoeveelheden op de detailpagina
  (ontbreekt het veld, dan geldt 4).

### Toevoegen, bewerken en verwijderen via de UI

- **Toevoegen** — tik op de oranje **+** floating action button op de
  receptenlijst. Vul het formulier in en tik op het ✓ icoon.
- **Bewerken** — open een recept en tik op het potloodicoon in de top app bar.
  Wijzigingen op een SEED-recept maken er automatisch een USER-recept van.
- **Verwijderen** — open een recept en tik op het prullenbak-icoon. Bevestig in
  het dialoogvenster.

## Project structuur

```
app/src/main/java/com/ewout/recepten/
├── RecipeApplication.kt
├── AppContainer.kt
├── MainActivity.kt
├── data/
│   ├── Recipe.kt, Ingredient.kt, Bron.kt
│   ├── RecipeRepository.kt
│   ├── local/   (Room: Database, DAO, Entity, Converters)
│   └── seed/    (SeedLoader, SeedVersion)
└── ui/
    ├── theme/   (Color, Theme, Type)
    ├── list/    (RecipeListScreen + ViewModel)
    ├── detail/  (RecipeDetailScreen + ViewModel)
    └── edit/    (RecipeEditScreen + ViewModel — voor add én edit)
```
