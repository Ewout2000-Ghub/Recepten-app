@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ewout.recepten.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ewout.recepten.data.Recipe
import com.ewout.recepten.ui.theme.BrandCream
import com.ewout.recepten.ui.theme.BrandOrange
import com.ewout.recepten.ui.theme.BrandSurface
import com.ewout.recepten.ui.theme.BrandSurfaceMuted
import com.ewout.recepten.ui.theme.TextPrimary
import com.ewout.recepten.ui.theme.TextSecondary

@Composable
fun RecipeListScreen(
    viewModel: RecipeListViewModel,
    onRecipeClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BrandCream,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Recepten",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BrandSurface
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BrandOrange,
                    titleContentColor = BrandSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = BrandOrange,
                contentColor = BrandSurface
            ) {
                Icon(Icons.Default.Add, contentDescription = "Recept toevoegen")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SearchBar(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    onClear = viewModel::clearQuery
                )
                if (state.allCategories.isNotEmpty()) {
                    CategoryFilterRow(
                        categories = state.allCategories,
                        selected = state.selectedCategories,
                        onToggle = viewModel::toggleCategoryFilter,
                        onClear = viewModel::clearCategoryFilters
                    )
                }

                when {
                    state.isLoading -> LoadingState()
                    state.groups.isEmpty() -> EmptyState(query = state.query)
                    else -> RecipeGroups(
                        state = state,
                        onToggleCollapsed = viewModel::toggleCollapsed,
                        onRecipeClick = onRecipeClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Zoek op ingrediënt") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Zoekopdracht wissen")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = BrandSurface,
            unfocusedContainerColor = BrandSurface,
            focusedBorderColor = BrandOrange,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected.isNotEmpty()) {
            AssistChip(
                onClick = onClear,
                label = { Text("Alles") },
                leadingIcon = {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = BrandSurface,
                    labelColor = TextPrimary
                )
            )
        }
        categories.forEach { cat ->
            val isSelected = cat in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(cat) },
                label = { Text(cat) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = BrandSurface,
                    selectedContainerColor = BrandOrange,
                    labelColor = TextPrimary,
                    selectedLabelColor = BrandSurface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = BrandOrange
                )
            )
        }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = BrandOrange)
            Spacer(Modifier.height(12.dp))
            Text(
                "Recepten worden geladen…",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyState(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (query.isBlank()) "Nog geen recepten" else "Geen recepten gevonden",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun RecipeGroups(
    state: RecipeListUiState,
    onToggleCollapsed: (String) -> Unit,
    onRecipeClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp)
    ) {
        state.groups.forEach { group ->
            val collapsed = group.categorie in state.collapsedCategories
            item(key = "header-${group.categorie}") {
                CategoryHeader(
                    categorie = group.categorie,
                    count = group.recepten.size,
                    collapsed = collapsed,
                    onClick = { onToggleCollapsed(group.categorie) }
                )
            }
            if (!collapsed) {
                items(group.recepten, key = { it.recipe.id }) { item ->
                    RecipeCard(
                        recipe = item.recipe,
                        matchedIngredient = item.matchedIngredient,
                        onClick = { onRecipeClick(item.recipe.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    categorie: String,
    count: Int,
    collapsed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = categorie,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Icon(
            imageVector = if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = if (collapsed) "Uitklappen" else "Inklappen",
            tint = TextSecondary
        )
    }
}

@Composable
private fun RecipeCard(
    recipe: Recipe,
    matchedIngredient: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = recipe.naam,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            CategorieChip(recipe.categorie)
            Spacer(Modifier.height(10.dp))
            val preview = recipe.ingredienten.take(3).joinToString(separator = " · ") { it.naam }
            if (preview.isNotEmpty()) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AnimatedVisibility(visible = matchedIngredient != null) {
                if (matchedIngredient != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Match: $matchedIngredient",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorieChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = BrandSurfaceMuted,
        modifier = Modifier.clip(RoundedCornerShape(50))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            modifier = Modifier
                .background(BrandSurfaceMuted)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
