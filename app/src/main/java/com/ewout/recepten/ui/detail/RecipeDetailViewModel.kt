package com.ewout.recepten.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ewout.recepten.AppContainer
import com.ewout.recepten.data.Recipe
import com.ewout.recepten.data.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val recipe: Recipe? = null,
    val isDeleted: Boolean = false
)

class RecipeDetailViewModel(
    private val repository: RecipeRepository,
    private val recipeId: String
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeDetailUiState())
    val state: StateFlow<RecipeDetailUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val recipe = repository.getById(recipeId)
            _state.value = RecipeDetailUiState(
                isLoading = false,
                recipe = recipe
            )
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(recipeId)
            _state.value = _state.value.copy(isDeleted = true)
        }
    }

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
