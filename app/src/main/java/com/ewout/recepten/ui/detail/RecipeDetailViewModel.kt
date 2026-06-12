package com.ewout.recepten.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ewout.recepten.AppContainer
import com.ewout.recepten.data.Recipe
import com.ewout.recepten.data.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val recipe: Recipe? = null,
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

class RecipeDetailViewModel(
    private val repository: RecipeRepository,
    private val recipeId: String
) : ViewModel() {

    private val deleted = MutableStateFlow(false)

    // null = nog niet aangepast door de gebruiker → volg de basis van het recept.
    private val gekozenPersonen = MutableStateFlow<Int?>(null)
    private val afgevinkteIngredienten = MutableStateFlow<Set<Int>>(emptySet())
    private val afgevinkteStappen = MutableStateFlow<Set<Int>>(emptySet())

    val state: StateFlow<RecipeDetailUiState> = combine(
        repository.observeById(recipeId),
        deleted,
        gekozenPersonen,
        afgevinkteIngredienten,
        afgevinkteStappen
    ) { recipe, isDeleted, personen, ingChecks, stapChecks ->
        val basis = basisPersonen(recipe)
        RecipeDetailUiState(
            isLoading = false,
            recipe = recipe,
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
        viewModelScope.launch {
            // Eerst markeren, zodat het scherm wegnavigeert voordat de
            // database-flow null voor het verwijderde recept emit.
            deleted.value = true
            repository.delete(recipeId)
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
