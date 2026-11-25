package com.projet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.projet.app.ui.detail.ProjectDetailScreen
import com.projet.app.ui.list.ProjectListScreen
import com.projet.app.ui.theme.ProjetTheme
import com.projet.app.ui.theme.ThemePrefs
import com.projet.app.ui.trash.TrashScreen
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val themePrefs: ThemePrefs by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val theme by themePrefs.settings.collectAsStateWithLifecycle()
            ProjetTheme(theme) { ProjetNavHost() }
        }
    }
}

@Composable
private fun ProjetNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "list") {
        composable("list") {
            ProjectListScreen(
                onOpenProject = { id -> nav.navigate("detail/$id") },
                onOpenTrash = { nav.navigate("trash") }
            )
        }
        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            ProjectDetailScreen(projectId = id, onBack = { nav.popBackStack() })
        }
        composable("trash") {
            TrashScreen(onBack = { nav.popBackStack() })
        }
    }
}
