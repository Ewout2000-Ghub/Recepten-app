package com.ewout.recepten

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ewout.recepten.ui.detail.RecipeDetailScreen
import com.ewout.recepten.ui.detail.RecipeDetailViewModel
import com.ewout.recepten.ui.list.RecipeListScreen
import com.ewout.recepten.ui.list.RecipeListViewModel
import com.ewout.recepten.ui.theme.BrandCream
import com.ewout.recepten.ui.theme.ReceptenTheme

object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{recipeId}"

    fun detail(id: String) = "detail/$id"
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReceptenTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BrandCream),
                    color = BrandCream
                ) {
                    ReceptenNavGraph()
                }
            }
        }
    }
}

@Composable
private fun ReceptenNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val container = (context.applicationContext as RecipeApplication).container

    NavHost(navController = navController, startDestination = Routes.LIST) {

        composable(Routes.LIST) {
            val vm: RecipeListViewModel = viewModel(
                factory = RecipeListViewModel.factory(container)
            )
            RecipeListScreen(
                viewModel = vm,
                onRecipeClick = { id -> navController.navigate(Routes.detail(id)) }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
        ) { backStack ->
            val recipeId = backStack.arguments?.getString("recipeId").orEmpty()
            val vm: RecipeDetailViewModel = viewModel(
                key = "detail-$recipeId",
                factory = RecipeDetailViewModel.factory(container, recipeId)
            )
            RecipeDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
