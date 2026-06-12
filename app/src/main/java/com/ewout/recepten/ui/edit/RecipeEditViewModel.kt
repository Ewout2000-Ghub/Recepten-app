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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Eén regel in het ingrediënten-formulier. Hoeveelheid blijft als string-buffer
 * voor invoer; bij opslaan parsen we hem naar Double (of null = "naar smaak").
 * [key] is stabiel per rij zodat Compose focus/IME-state niet kwijtraakt bij
 * het verwijderen van een tussenliggende rij.
 */
data class IngredientDraft(
    val key: Long,
    val naam: String = "",
    val hoeveelheid: String = "",
    val eenheid: String = ""
)

data class StapDraft(
    val key: Long,
    val tekst: String = ""
)

/**
 * [volgnummer] maakt elke melding uniek, zodat dezelfde fout bij een tweede
 * druk op opslaan opnieuw een snackbar triggert.
 */
data class Foutmelding(
    val tekst: String,
    val volgnummer: Long
)

data class RecipeEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val recipeId: String? = null,
    val sourceBron: Bron = Bron.USER,
    val naam: String = "",
    val categorie: String = "",
    val porties: String = "",
    val ingredienten: List<IngredientDraft> = emptyList(),
    val stappen: List<StapDraft> = emptyList(),
    val categorieSuggesties: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val savedId: String? = null,
    val foutmelding: Foutmelding? = null
)

class RecipeEditViewModel(
    private val repository: RecipeRepository,
    private val recipeId: String?
) : ViewModel() {

    private var volgendeKey = 0L
    private var volgendeMelding = 0L

    private fun nieuweKey(): Long = volgendeKey++

    private val _state = MutableStateFlow(
        RecipeEditUiState(
            isLoading = recipeId != null,
            ingredienten = listOf(IngredientDraft(key = nieuweKey())),
            stappen = listOf(StapDraft(key = nieuweKey()))
        )
    )
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
                _state.update {
                    it.copy(
                        isLoading = false,
                        isNew = true,
                        categorieSuggesties = suggesties
                    )
                }
            } else {
                val existing = repository.getById(recipeId)
                if (existing == null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isNew = true,
                            categorieSuggesties = suggesties,
                            foutmelding = melding("Recept niet gevonden")
                        )
                    }
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
                                key = nieuweKey(),
                                naam = ing.naam,
                                hoeveelheid = ing.hoeveelheid?.let { formatNumberForInput(it) }
                                    .orEmpty(),
                                eenheid = ing.eenheid.orEmpty()
                            )
                        }.ifEmpty { listOf(IngredientDraft(key = nieuweKey())) },
                        stappen = existing.bereidingswijze
                            .map { StapDraft(key = nieuweKey(), tekst = it) }
                            .ifEmpty { listOf(StapDraft(key = nieuweKey())) },
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
        _state.update { it.copy(ingredienten = it.ingredienten + IngredientDraft(key = nieuweKey())) }
    }

    fun removeIngredient(index: Int) {
        _state.update {
            val list = it.ingredienten.toMutableList()
            if (index in list.indices) list.removeAt(index)
            it.copy(
                ingredienten = list.ifEmpty { listOf(IngredientDraft(key = nieuweKey())) }
            )
        }
    }

    fun onStapChange(index: Int, value: String) {
        _state.update {
            it.copy(
                stappen = it.stappen.toMutableList().also { list ->
                    if (index in list.indices) list[index] = list[index].copy(tekst = value)
                }
            )
        }
    }

    fun addStap() {
        _state.update { it.copy(stappen = it.stappen + StapDraft(key = nieuweKey())) }
    }

    fun removeStap(index: Int) {
        _state.update {
            val list = it.stappen.toMutableList()
            if (index in list.indices) list.removeAt(index)
            it.copy(stappen = list.ifEmpty { listOf(StapDraft(key = nieuweKey())) })
        }
    }

    fun save() {
        val s = _state.value
        val naam = s.naam.trim()
        val categorie = s.categorie.trim()
        if (naam.isEmpty()) {
            _state.update { it.copy(foutmelding = melding("Naam is verplicht")) }
            return
        }
        if (categorie.isEmpty()) {
            _state.update { it.copy(foutmelding = melding("Categorie is verplicht")) }
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
        val stappen = s.stappen.map { it.tekst.trim() }.filter { it.isNotEmpty() }
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

        _state.update { it.copy(isSaving = true, foutmelding = null) }
        viewModelScope.launch {
            // SEED → USER promotie gebeurt automatisch omdat bron = USER.
            repository.saveUserEdit(recipe)
            _state.update { it.copy(isSaving = false, savedId = id) }
        }
    }

    private fun melding(tekst: String): Foutmelding =
        Foutmelding(tekst = tekst, volgnummer = volgendeMelding++)

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
