@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ewout.recepten.ui.edit

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ewout.recepten.ui.theme.BrandCream
import com.ewout.recepten.ui.theme.BrandOrange
import com.ewout.recepten.ui.theme.BrandSurface
import com.ewout.recepten.ui.theme.BrandSurfaceMuted
import com.ewout.recepten.ui.theme.TextPrimary
import com.ewout.recepten.ui.theme.TextSecondary

@Composable
fun RecipeEditScreen(
    viewModel: RecipeEditViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.savedId) {
        state.savedId?.let { onSaved(it) }
    }

    LaunchedEffect(state.foutmelding) {
        state.foutmelding?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = BrandCream,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = TextPrimary,
                    contentColor = BrandSurface
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isNew) "Recept toevoegen" else "Recept bewerken",
                        style = MaterialTheme.typography.titleLarge,
                        color = BrandSurface
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
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = !state.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Opslaan", tint = BrandSurface)
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
                else -> EditForm(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun EditForm(state: RecipeEditUiState, viewModel: RecipeEditViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FormCard {
                LabeledField(label = "Naam") {
                    OutlinedTextField(
                        value = state.naam,
                        onValueChange = viewModel::onNaamChange,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors()
                    )
                }
                Spacer(Modifier.height(12.dp))
                LabeledField(label = "Categorie") {
                    OutlinedTextField(
                        value = state.categorie,
                        onValueChange = viewModel::onCategorieChange,
                        singleLine = true,
                        placeholder = { Text("Bijv. Pasta, Kip, Ovenschotel") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors()
                    )
                    if (state.categorieSuggesties.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.categorieSuggesties.forEach { cat ->
                                AssistChip(
                                    onClick = { viewModel.onCategorieChange(cat) },
                                    label = { Text(cat) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = BrandSurfaceMuted,
                                        labelColor = TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                LabeledField(label = "Porties (optioneel)") {
                    OutlinedTextField(
                        value = state.porties,
                        onValueChange = viewModel::onPortiesChange,
                        singleLine = true,
                        placeholder = { Text("Bijv. 4 personen") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors()
                    )
                }
            }
        }

        item {
            SectionTitle("Ingrediënten")
        }

        itemsIndexed(state.ingredienten, key = { idx, _ -> "ing-$idx" }) { index, draft ->
            FormCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        modifier = Modifier.width(28.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(
                        value = draft.naam,
                        onValueChange = { viewModel.onIngredientChange(index, draft.copy(naam = it)) },
                        placeholder = { Text("Naam") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors()
                    )
                    IconButton(onClick = { viewModel.removeIngredient(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Ingrediënt verwijderen", tint = TextSecondary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft.hoeveelheid,
                        onValueChange = { viewModel.onIngredientChange(index, draft.copy(hoeveelheid = it)) },
                        placeholder = { Text("Hoeveelheid") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors()
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = draft.eenheid,
                        onValueChange = { viewModel.onIngredientChange(index, draft.copy(eenheid = it)) },
                        placeholder = { Text("Eenheid") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors()
                    )
                }
            }
        }

        item {
            AddRowButton(text = "Ingrediënt toevoegen", onClick = viewModel::addIngredient)
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionTitle("Bereidingswijze")
        }

        itemsIndexed(state.stappen, key = { idx, _ -> "step-$idx" }) { index, step ->
            FormCard {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        modifier = Modifier
                            .width(28.dp)
                            .padding(top = 16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(
                        value = step,
                        onValueChange = { viewModel.onStapChange(index, it) },
                        placeholder = { Text("Stap omschrijven") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        colors = textFieldColors()
                    )
                    IconButton(onClick = { viewModel.removeStap(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Stap verwijderen", tint = TextSecondary)
                    }
                }
            }
        }

        item {
            AddRowButton(text = "Stap toevoegen", onClick = viewModel::addStap)
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun AddRowButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = BrandOrange)
        Spacer(Modifier.width(8.dp))
        Text(text, color = BrandOrange, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = BrandSurface,
    unfocusedContainerColor = BrandSurface,
    focusedBorderColor = BrandOrange,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    cursorColor = BrandOrange,
    focusedLabelColor = BrandOrange
)
