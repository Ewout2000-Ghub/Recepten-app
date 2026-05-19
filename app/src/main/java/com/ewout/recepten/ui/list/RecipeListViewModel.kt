package com.ewout.recepten.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ewout.recepten.AppContainer
import com.ewout.recepten.data.Recipe
import com.ewout.recepten.data.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RecipeListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val selectedCategories: Set<String> = emptySet(),
    val allCategories: List<String> = emptyList(),
    val groups: List<CategoryGroup> = emptyList(),
    val collapsedCategories: Set<String> = emptySet()
)

data class CategoryGroup(
    val categorie: String,
    val recepten: List<RecipeListItem>
)

data class RecipeListItem(
    val recipe: Recipe,
    val matchedIngredient: String? = null
)

class RecipeListViewModel(
    private val repository: RecipeRepository,
    seedReady: StateFlow<Boolean>
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    private val collapsed = MutableStateFlow<Set<String>>(emptySet())

    private val recipes: StateFlow<List<Recipe>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<RecipeListUiState> = combine(
        recipes,
        seedReady,
        query,
        selectedCategories,
        collapsed
    ) { recipeList, ready, q, cats, coll ->
        val loading = !ready && recipeList.isEmpty()
        val allCats = recipeList.map { it.categorie }.distinct().sortedBy { it.lowercase() }

        val trimmed = q.trim()
        val filtered = recipeList.filter { recipe ->
            val matchesCategory = cats.isEmpty() || recipe.categorie in cats
            val matchesQuery = trimmed.isEmpty() || recipe.ingredienten.any {
                it.naam.contains(trimmed, ignoreCase = true)
            }
            matchesCategory && matchesQuery
        }

        val items = filtered.map { recipe ->
            val match = if (trimmed.isEmpty()) null else {
                recipe.ingredienten.firstOrNull { it.naam.contains(trimmed, ignoreCase = true) }?.naam
            }
            RecipeListItem(recipe, match)
        }

        val grouped = items
            .groupBy { it.recipe.categorie }
            .toList()
            .sortedBy { it.first.lowercase() }
            .map { (cat, list) ->
                CategoryGroup(
                    categorie = cat,
                    recepten = list.sortedBy { it.recipe.naam.lowercase() }
                )
            }

        RecipeListUiState(
            isLoading = loading,
            query = q,
            selectedCategories = cats,
            allCategories = allCats,
            groups = grouped,
            collapsedCategories = coll
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        RecipeListUiState()
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clearQuery() {
        query.value = ""
    }

    fun toggleCategoryFilter(categorie: String) {
        selectedCategories.value = selectedCategories.value.toMutableSet().apply {
            if (!add(categorie)) remove(categorie)
        }
    }

    fun clearCategoryFilters() {
        selectedCategories.value = emptySet()
    }

    fun toggleCollapsed(categorie: String) {
        collapsed.value = collapsed.value.toMutableSet().apply {
            if (!add(categorie)) remove(categorie)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RecipeListViewModel(container.repository, container.seedReady) as T
                }
            }
    }
}
