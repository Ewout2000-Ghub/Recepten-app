package com.ewout.recepten.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ewout.recepten.AppContainer
import com.ewout.recepten.data.Recipe
import com.ewout.recepten.data.RecipeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Eén keuzeoptie in de versie-switcher van een gerecht. */
data class VersieOptie(
    val id: String,
    val label: String
)

data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val recipe: Recipe? = null,
    val versies: List<VersieOptie> = emptyList(),
    val geselecteerdeVersieId: String? = null,
    val isDeleted: Boolean = false,
    val basisPersonen: Int = STANDAARD_PERSONEN,
    val personen: Int = STANDAARD_PERSONEN,
    val afgevinkteIngredienten: Set<Int> = emptySet(),
    val afgevinkteStappen: Set<Int> = emptySet()
) {
    val schaalFactor: Double
        get() = personen.toDouble() / basisPersonen.toDouble()
}

/** Recepten zonder expliciete porties gaan uit van 4 personen. */
const val STANDAARD_PERSONEN = 4

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailViewModel(
    private val repository: RecipeRepository,
    private val recipeId: String
) : ViewModel() {

    private val deleted = MutableStateFlow(false)

    // null = nog niet gekozen → toon de oorspronkelijk geopende versie.
    private val gekozenVersieId = MutableStateFlow<String?>(null)
    // null = nog niet aangepast door de gebruiker → volg de basis van het recept.
    private val gekozenPersonen = MutableStateFlow<Int?>(null)
    private val afgevinkteIngredienten = MutableStateFlow<Set<Int>>(emptySet())
    private val afgevinkteStappen = MutableStateFlow<Set<Int>>(emptySet())

    // Volg het geopende recept om zijn groepsleutel te vinden en observeer dan
    // de volledige versiegroep, zodat schakelen tussen versies reactief is.
    private val groep: StateFlow<List<Recipe>> = repository.observeById(recipeId)
        .flatMapLatest { recipe ->
            repository.observeGroup(recipe?.groepSleutel ?: recipeId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val checks = combine(afgevinkteIngredienten, afgevinkteStappen) { ing, stap ->
        ing to stap
    }

    val state: StateFlow<RecipeDetailUiState> = combine(
        groep,
        gekozenVersieId,
        deleted,
        gekozenPersonen,
        checks
    ) { groepVersies, gekozenId, isDeleted, personen, (ingChecks, stapChecks) ->
        val huidig = groepVersies.firstOrNull { it.id == gekozenId }
            ?: groepVersies.firstOrNull { it.id == recipeId }
            ?: groepVersies.firstOrNull()
        val basis = basisPersonen(huidig)
        RecipeDetailUiState(
            isLoading = false,
            recipe = huidig,
            versies = groepVersies.map { VersieOptie(it.id, it.versieNaam ?: it.naam) },
            geselecteerdeVersieId = huidig?.id,
            isDeleted = isDeleted,
            basisPersonen = basis,
            personen = personen ?: basis,
            afgevinkteIngredienten = ingChecks,
            afgevinkteStappen = stapChecks
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        RecipeDetailUiState()
    )

    fun selecteerVersie(id: String) {
        if (id == state.value.geselecteerdeVersieId) return
        gekozenVersieId.value = id
        // Ingrediënten en stappen verschillen per versie; vinkjes en het
        // gekozen aantal personen horen bij de vorige versie, dus resetten.
        afgevinkteIngredienten.value = emptySet()
        afgevinkteStappen.value = emptySet()
        gekozenPersonen.value = null
    }

    fun wijzigPersonen(delta: Int) {
        val nieuw = (state.value.personen + delta).coerceIn(1, 20)
        gekozenPersonen.value = nieuw
    }

    fun toggleIngredient(index: Int) {
        afgevinkteIngredienten.update { it.toggled(index) }
    }

    fun toggleStap(index: Int) {
        afgevinkteStappen.update { it.toggled(index) }
    }

    fun delete() {
        // Verwijder de versie die nu getoond wordt (niet per se de geopende).
        val teVerwijderen = state.value.recipe?.id ?: recipeId
        viewModelScope.launch {
            // Eerst markeren, zodat het scherm wegnavigeert voordat de
            // database-flow null voor het verwijderde recept emit.
            deleted.value = true
            repository.delete(teVerwijderen)
        }
    }

    private fun Set<Int>.toggled(index: Int): Set<Int> =
        if (index in this) this - index else this + index

    private fun basisPersonen(recipe: Recipe?): Int =
        recipe?.porties
            ?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() }
            ?.coerceIn(1, 20)
            ?: STANDAARD_PERSONEN

    companion object {
        fun factory(container: AppContainer, recipeId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RecipeDetailViewModel(container.repository, recipeId) as T
                }
            }
    }
}
