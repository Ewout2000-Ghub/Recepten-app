package com.ewout.recepten.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ewout.recepten.AppContainer
import com.ewout.recepten.data.Bron
import com.ewout.recepten.data.Ingredient
import com.ewout.recepten.data.Recipe
import com.ewout.recepten.data.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Eén regel in het ingrediënten-formulier. Hoeveelheid blijft als string-buffer
 * voor invoer; bij opslaan parsen we hem naar Double (of null = "naar smaak").
 */
data class IngredientDraft(
    val naam: String = "",
    val hoeveelheid: String = "",
    val eenheid: String = ""
)

data class RecipeEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val recipeId: String? = null,
    val sourceBron: Bron = Bron.USER,
    val naam: String = "",
    val categorie: String = "",
    val porties: String = "",
    val ingredienten: List<IngredientDraft> = listOf(IngredientDraft()),
    val stappen: List<String> = listOf(""),
    val categorieSuggesties: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val savedId: String? = null,
    val foutmelding: String? = null
)

class RecipeEditViewModel(
    private val repository: RecipeRepository,
    private val recipeId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeEditUiState(isLoading = recipeId != null))
    val state: StateFlow<RecipeEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val firstList = try {
                repository.observeAll().first()
            } catch (_: NoSuchElementException) {
                emptyList()
            }
            val suggesties = firstList
                .map { it.categorie }
                .distinct()
                .sortedBy { it.lowercase() }

            if (recipeId == null) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isNew = true,
                    categorieSuggesties = suggesties
                )
            } else {
                val existing = repository.getById(recipeId)
                if (existing == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isNew = true,
                        categorieSuggesties = suggesties,
                        foutmelding = "Recept niet gevonden"
                    )
                } else {
                    _state.value = RecipeEditUiState(
                        isLoading = false,
                        isNew = false,
                        recipeId = existing.id,
                        sourceBron = existing.bron,
                        naam = existing.naam,
                        categorie = existing.categorie,
                        porties = existing.porties.orEmpty(),
                        ingredienten = existing.ingredienten.map { ing ->
                            IngredientDraft(
                                naam = ing.naam,
                                hoeveelheid = ing.hoeveelheid?.let { formatNumberForInput(it) }
                                    .orEmpty(),
                                eenheid = ing.eenheid.orEmpty()
                            )
                        }.ifEmpty { listOf(IngredientDraft()) },
                        stappen = existing.bereidingswijze.ifEmpty { listOf("") },
                        categorieSuggesties = suggesties
                    )
                }
            }
        }
    }

    fun onNaamChange(value: String) { _state.update { it.copy(naam = value) } }
    fun onCategorieChange(value: String) { _state.update { it.copy(categorie = value) } }
    fun onPortiesChange(value: String) { _state.update { it.copy(porties = value) } }

    fun onIngredientChange(index: Int, draft: IngredientDraft) {
        _state.update {
            it.copy(
                ingredienten = it.ingredienten.toMutableList().also { list ->
                    if (index in list.indices) list[index] = draft
                }
            )
        }
    }

    fun addIngredient() {
        _state.update { it.copy(ingredienten = it.ingredienten + IngredientDraft()) }
    }

    fun removeIngredient(index: Int) {
        _state.update {
            val list = it.ingredienten.toMutableList()
            if (index in list.indices) list.removeAt(index)
            it.copy(ingredienten = if (list.isEmpty()) listOf(IngredientDraft()) else list)
        }
    }

    fun onStapChange(index: Int, value: String) {
        _state.update {
            it.copy(
                stappen = it.stappen.toMutableList().also { list ->
                    if (index in list.indices) list[index] = value
                }
            )
        }
    }

    fun addStap() {
        _state.update { it.copy(stappen = it.stappen + "") }
    }

    fun removeStap(index: Int) {
        _state.update {
            val list = it.stappen.toMutableList()
            if (index in list.indices) list.removeAt(index)
            it.copy(stappen = if (list.isEmpty()) listOf("") else list)
        }
    }

    fun save() {
        val s = _state.value
        val naam = s.naam.trim()
        val categorie = s.categorie.trim()
        if (naam.isEmpty()) {
            _state.value = s.copy(foutmelding = "Naam is verplicht")
            return
        }
        if (categorie.isEmpty()) {
            _state.value = s.copy(foutmelding = "Categorie is verplicht")
            return
        }

        val ingredienten = s.ingredienten
            .filter { it.naam.isNotBlank() }
            .map { draft ->
                Ingredient(
                    naam = draft.naam.trim(),
                    hoeveelheid = parseHoeveelheid(draft.hoeveelheid),
                    eenheid = draft.eenheid.trim().ifEmpty { null }
                )
            }
        val stappen = s.stappen.map { it.trim() }.filter { it.isNotEmpty() }
        val porties = s.porties.trim().ifEmpty { null }

        val id = s.recipeId ?: generateId(naam)
        val recipe = Recipe(
            id = id,
            naam = naam,
            categorie = categorie,
            porties = porties,
            ingredienten = ingredienten,
            bereidingswijze = stappen,
            bron = Bron.USER
        )

        _state.value = s.copy(isSaving = true, foutmelding = null)
        viewModelScope.launch {
            // SEED → USER promotie gebeurt automatisch omdat bron = USER.
            repository.saveUserEdit(recipe)
            _state.value = _state.value.copy(isSaving = false, savedId = id)
        }
    }

    private fun parseHoeveelheid(raw: String): Double? {
        val cleaned = raw.trim().replace(',', '.')
        if (cleaned.isEmpty()) return null
        return cleaned.toDoubleOrNull()
    }

    private fun formatNumberForInput(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString()
        else value.toBigDecimal().stripTrailingZeros().toPlainString().replace('.', ',')
    }

    private fun generateId(naam: String): String {
        val base = naam.lowercase(Locale.ROOT)
            .replace("ë", "e").replace("é", "e").replace("è", "e")
            .replace("ï", "i").replace("í", "i")
            .replace("ó", "o").replace("ö", "o")
            .replace("ü", "u").replace("ú", "u")
            .replace("à", "a").replace("á", "a").replace("â", "a")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "recept" }
        return "$base-${System.currentTimeMillis()}"
    }

    private inline fun MutableStateFlow<RecipeEditUiState>.update(
        block: (RecipeEditUiState) -> RecipeEditUiState
    ) {
        value = block(value)
    }

    companion object {
        fun factory(container: AppContainer, recipeId: String?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RecipeEditViewModel(container.repository, recipeId) as T
                }
            }
    }
}
