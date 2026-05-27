package org.mirai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import org.mirai.app.ui.screen.*
import org.mirai.app.ui.theme.MyApplicationTheme
import org.mirai.app.ui.viewmodel.MangaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MangaViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeColor by viewModel.settingsManager.themeColor.collectAsStateWithLifecycle()

            MyApplicationTheme(seedColorVal = themeColor) {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Display bottom bar on root feeds ("home", "library", "settings")
                        val rootTabs = listOf("home", "library", "settings")
                        if (currentRoute in rootTabs) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute == "home",
                                    onClick = {
                                        if (currentRoute != "home") {
                                            navController.navigate("home") {
                                                popUpTo("home") { inclusive = false }
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                                            contentDescription = "Home"
                                        )
                                    },
                                    label = { Text("Home") }
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "library",
                                    onClick = {
                                        if (currentRoute != "library") {
                                            navController.navigate("library") {
                                                popUpTo("home") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == "library") Icons.Filled.Book else Icons.Outlined.Book,
                                            contentDescription = "Library"
                                        )
                                    },
                                    label = { Text("Library") }
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "settings",
                                    onClick = {
                                        if (currentRoute != "settings") {
                                            navController.navigate("settings") {
                                                popUpTo("home") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                                            contentDescription = "Settings"
                                        )
                                    },
                                    label = { Text("Settings") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                navController = navController,
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }

                        composable("library") {
                            LibraryScreen(
                                viewModel = viewModel,
                                navController = navController
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "detail/{source}/{slug}",
                            arguments = listOf(
                                navArgument("source") { type = NavType.StringType },
                                navArgument("slug") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val source = backStackEntry.arguments?.getString("source") ?: ""
                            val slug = backStackEntry.arguments?.getString("slug") ?: ""
                            MangaDetailScreen(
                                source = source,
                                slug = slug,
                                viewModel = viewModel,
                                navController = navController
                            )
                        }

                        composable(
                            route = "reader/{source}/{slug}/{chapterId}",
                            arguments = listOf(
                                navArgument("source") { type = NavType.StringType },
                                navArgument("slug") { type = NavType.StringType },
                                navArgument("chapterId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val source = backStackEntry.arguments?.getString("source") ?: ""
                            val slug = backStackEntry.arguments?.getString("slug") ?: ""
                            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                            MangaReaderScreen(
                                source = source,
                                slug = slug,
                                chapterId = chapterId,
                                viewModel = viewModel,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}
