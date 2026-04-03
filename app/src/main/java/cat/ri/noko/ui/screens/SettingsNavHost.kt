package cat.ri.noko.ui.screens

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cat.ri.noko.model.PersonaType

@Composable
fun SettingsNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "settings",
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable(
            route = "personas/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
        ) { backStack ->
            val type = PersonaType.valueOf(backStack.arguments!!.getString("type")!!)
            PersonaListScreen(
                type = type,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("persona_edit/${type.name}?id=$id") },
                onCreate = { navController.navigate("persona_edit/${type.name}") },
                onImport = if (type == PersonaType.CHARACTER) { uri ->
                    navController.navigate("character_import?uri=${Uri.encode(uri.toString())}")
                } else null,
            )
        }
        composable(
            route = "persona_edit/{type}?id={id}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStack ->
            val type = PersonaType.valueOf(backStack.arguments!!.getString("type")!!)
            val id = backStack.arguments?.getString("id")
            PersonaEditScreen(
                type = type,
                editId = id,
                onBack = { navController.popBackStack() },
            )
        }
        composable("models") {
            ModelListScreen(onBack = { navController.popBackStack() })
        }
        composable("prompt_builder") {
            AiSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("noko_polkit") {
            NokoPolkitScreen(onBack = { navController.popBackStack() })
        }
        composable("providers") {
            ProviderListScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "character_import?uri={uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { backStack ->
            val uri = Uri.parse(backStack.arguments!!.getString("uri")!!)
            CharacterImportScreen(
                uri = uri,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
