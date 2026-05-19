@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ewout.recepten.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ewout.recepten.data.Ingredient
import com.ewout.recepten.data.Recipe
import com.ewout.recepten.ui.formatHoeveelheid
import com.ewout.recepten.ui.theme.BrandCream
import com.ewout.recepten.ui.theme.BrandOrange
import com.ewout.recepten.ui.theme.BrandSurface
import com.ewout.recepten.ui.theme.BrandSurfaceMuted
import com.ewout.recepten.ui.theme.TextPrimary
import com.ewout.recepten.ui.theme.TextSecondary

@Composable
fun RecipeDetailScreen(
    viewModel: RecipeDetailViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onDeleted()
    }

    Scaffold(
        containerColor = BrandCream,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.recipe?.naam.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        color = BrandSurface,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Terug",
                            tint = BrandSurface
                        )
                    }
                },
                actions = {
                    val recipe = state.recipe
                    if (recipe != null) {
                        IconButton(onClick = { onEdit(recipe.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Bewerken", tint = BrandSurface)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Verwijderen", tint = BrandSurface)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandOrange,
                    titleContentColor = BrandSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BrandCream)
        ) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandOrange)
                }
                state.recipe == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Recept niet gevonden",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> DetailContent(state.recipe!!)
            }
        }
    }

    if (showDeleteDialog) {
        val recipe = state.recipe
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Recept verwijderen?") },
            text = {
                Text(
                    text = recipe?.let { "\"${it.naam}\" wordt definitief verwijderd." }
                        ?: "Dit recept wordt definitief verwijderd."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) {
                    Text("Verwijderen", color = BrandOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuleren", color = TextSecondary)
                }
            },
            containerColor = BrandSurface
        )
    }
}

@Composable
private fun DetailContent(recipe: Recipe) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderCard(recipe)
        }
        item {
            SectionTitle("Ingrediënten")
        }
        items(recipe.ingredienten) { ingredient ->
            IngredientRow(ingredient)
        }
        item {
            Spacer(Modifier.height(4.dp))
            SectionTitle("Bereidingswijze")
        }
        if (recipe.bereidingswijze.isEmpty()) {
            item { EmptyStepsState() }
        } else {
            itemsIndexed(recipe.bereidingswijze) { index, step ->
                StepRow(index + 1, step)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HeaderCard(recipe: Recipe) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = recipe.naam,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategorieChip(recipe.categorie)
                if (!recipe.porties.isNullOrBlank()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = recipe.porties,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun IngredientRow(ingredient: Ingredient) {
    val hoeveelheid = formatHoeveelheid(ingredient)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = BrandOrange,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text = ingredient.naam,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (hoeveelheid.isNotEmpty()) {
            Text(
                text = hoeveelheid,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(BrandOrange),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = BrandSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptyStepsState() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BrandSurfaceMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "Bereidingswijze nog niet ingevuld",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun CategorieChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = BrandSurfaceMuted
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
