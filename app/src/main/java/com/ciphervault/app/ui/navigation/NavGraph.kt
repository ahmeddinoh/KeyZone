package com.ciphervault.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ciphervault.app.ui.screens.BackupScreen
import com.ciphervault.app.ui.screens.EncryptScreen
import com.ciphervault.app.ui.screens.HomeScreen
import com.ciphervault.app.ui.screens.ImportScreen
import com.ciphervault.app.ui.screens.LockScreen
import com.ciphervault.app.ui.screens.SettingsScreen
import com.ciphervault.app.ui.screens.SetupScreen
import com.ciphervault.app.ui.screens.VaultScreen
import com.ciphervault.app.ui.screens.WelcomeScreen
import com.ciphervault.app.ui.theme.DarkBackground
import com.ciphervault.app.ui.theme.DarkSurfaceVariant
import com.ciphervault.app.ui.theme.TextSecondary
import com.ciphervault.app.ui.theme.VaultGreen
import com.ciphervault.app.ui.theme.VaultGreenDark
import com.ciphervault.app.viewmodel.AuthViewModel
import com.ciphervault.app.viewmodel.BackupViewModel
import com.ciphervault.app.viewmodel.ImportViewModel
import com.ciphervault.app.viewmodel.VaultViewModel

/**
 * Navigation items for the bottom navigation bar.
 */
private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val navItems = listOf(
    NavItem("home", "Home", Icons.Filled.Home),
    NavItem("vault", "Vault", Icons.Filled.Lock),
    NavItem("encrypt", "Encrypt", Icons.Filled.Add),
    NavItem("settings", "Settings", Icons.Filled.Settings)
)

private enum class WelcomeDestination {
    WELCOME,
    SETUP,
    IMPORT
}

/**
 * Root navigation host that switches between:
 * 1. WELCOME (first launch: Create Vault or Import Vault)
 * 2. LOCKED (subsequent launches: asks for PIN/password key to unwrap master key)
 * 3. AUTHENTICATED (main app dashboard, vault, encrypt, settings, backup)
 */
@Composable
fun CipherVaultNavHost() {
    val authViewModel: AuthViewModel = viewModel()
    val appState by authViewModel.appState.collectAsState()

    var welcomeDestination by remember { mutableStateOf(WelcomeDestination.WELCOME) }

    AnimatedContent(
        targetState = appState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AppStateTransition"
    ) { state ->
        when (state) {
            AuthViewModel.AppState.WELCOME -> {
                when (welcomeDestination) {
                    WelcomeDestination.WELCOME -> {
                        WelcomeScreen(
                            onCreateVault = { welcomeDestination = WelcomeDestination.SETUP },
                            onImportVault = { welcomeDestination = WelcomeDestination.IMPORT }
                        )
                    }
                    WelcomeDestination.SETUP -> {
                        SetupScreen(
                            authViewModel = authViewModel,
                            onComplete = {
                                // Once setup completes, authViewModel sets state to AUTHENTICATED
                            }
                        )
                    }
                    WelcomeDestination.IMPORT -> {
                        val importViewModel: ImportViewModel = viewModel()
                        ImportScreen(
                            importViewModel = importViewModel,
                            onBack = { welcomeDestination = WelcomeDestination.WELCOME },
                            onImportSuccess = {
                                authViewModel.setAuthenticated()
                            }
                        )
                    }
                }
            }

            AuthViewModel.AppState.LOCKED -> {
                LockScreen(authViewModel = authViewModel)
            }

            AuthViewModel.AppState.AUTHENTICATED -> {
                MainAppScaffold(authViewModel = authViewModel)
            }
        }
    }
}

/**
 * Main app scaffold with bottom navigation bar and authenticated content.
 */
@Composable
private fun MainAppScaffold(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val vaultViewModel: VaultViewModel = viewModel()
    val backupViewModel: BackupViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val toastMessage by vaultViewModel.toastMessage.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            vaultViewModel.clearToast()
        }
    }

    val showBottomBar = currentRoute in listOf("home", "vault", "encrypt", "settings")

    // Clean tab navigation that preserves backstack integrity and avoids destination stacking
    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.ciphervault.app.CipherVaultApp
    val pendingUris by app.pendingSharedUris.collectAsState()

    LaunchedEffect(pendingUris) {
        if (pendingUris.isNotEmpty()) {
            navigateToTab("encrypt")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = DarkBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurfaceVariant,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = com.ciphervault.app.ui.theme.CardBorderSubtle
                    )
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navigateToTab(item.route)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VaultGreen,
                                selectedTextColor = VaultGreen,
                                indicatorColor = VaultGreenDark.copy(alpha = 0.15f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    vaultViewModel = vaultViewModel,
                    onNavigateToTab = navigateToTab,
                    onNavigateToBackup = { navController.navigate("backup") }
                )
            }
            composable("vault") {
                VaultScreen(
                    vaultViewModel = vaultViewModel,
                    onNavigateToEncrypt = { navigateToTab("encrypt") }
                )
            }
            composable("encrypt") {
                EncryptScreen(vaultViewModel = vaultViewModel)
            }
            composable("settings") {
                SettingsScreen(
                    authViewModel = authViewModel,
                    vaultViewModel = vaultViewModel,
                    onNavigateToBackup = { navController.navigate("backup") }
                )
            }
            composable("backup") {
                BackupScreen(
                    backupViewModel = backupViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
