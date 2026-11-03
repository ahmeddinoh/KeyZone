package com.ciphervault.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ciphervault.app.ui.components.FloatingNavPill
import com.ciphervault.app.ui.components.VaultTab
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.ciphervault.app.ui.screens.BackupScreen
import com.ciphervault.app.ui.screens.EncryptScreen
import com.ciphervault.app.ui.screens.HomeScreen
import com.ciphervault.app.ui.screens.ImportScreen
import com.ciphervault.app.ui.screens.LockScreen
import com.ciphervault.app.ui.screens.SettingsScreen
import com.ciphervault.app.ui.screens.SetupScreen
import com.ciphervault.app.ui.screens.VaultScreen
import com.ciphervault.app.ui.screens.WelcomeScreen
import com.ciphervault.app.ui.theme.VaultColors
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

    val hazeState = remember { HazeState() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTab = VaultTab.fromRoute(currentRoute)

    val snackbarHostState = remember { SnackbarHostState() }
    val toastMessage by vaultViewModel.toastMessage.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            vaultViewModel.clearToast()
        }
    }

    val hasSelectedFiles = vaultViewModel.selectedFilesForEncryption.isNotEmpty()
    val showBottomBar = currentRoute in listOf("home", "vault", "encrypt", "settings") && !(currentRoute == "encrypt" && hasSelectedFiles)

    val isBottomBarVisible = remember { mutableStateOf(true) }
    
    // Reset visibility when navigating to a new screen or when selected files change
    // This ensures if the list shrinks (e.g., after encrypting) and can no longer scroll,
    // the nav bar isn't stuck hidden.
    LaunchedEffect(currentRoute, hasSelectedFiles) {
        isBottomBarVisible.value = true
    }
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            var scrollDistance = 0f
            
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                
                if (scrollDistance * delta < 0) {
                    scrollDistance = 0f
                }
                
                scrollDistance += delta
                
                // Give it some "head room" before hiding (e.g. scroll down at least 40px)
                if (scrollDistance < -40f) {
                    isBottomBarVisible.value = false
                    scrollDistance = -40f
                } 
                // Show it very quickly when scrolling up (only 10px needed)
                else if (scrollDistance > 10f) {
                    isBottomBarVisible.value = true
                    scrollDistance = 10f
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If scroll hits the top boundary (unconsumed pull-down), ensure nav bar is visible
                if (available.y > 0f) {
                    isBottomBarVisible.value = true
                    scrollDistance = 0f
                }
                return Offset.Zero
            }
        }
    }

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
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = VaultColors.Ground,
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .haze(state = hazeState)
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
                        onNavigateToEncrypt = { navigateToTab("encrypt") },
                        onNavigateToImagePreview = { id -> navController.navigate("image_preview/$id") },
                        onNavigateToMediaPreview = { id -> navController.navigate("media_preview/$id") }
                    )
                }
                composable("encrypt") {
                    EncryptScreen(vaultViewModel = vaultViewModel)
                }
                composable("settings") {
                    SettingsScreen(
                        authViewModel = authViewModel,
                        vaultViewModel = vaultViewModel,
                        onNavigateToBackup = { navController.navigate("backup") },
                        onNavigateToChangeKey = { navController.navigate("change_key") },
                        onNavigateToStorageInsights = { navController.navigate("storage_insights") }
                    )
                }
                composable("storage_insights") {
                    com.ciphervault.app.ui.screens.StorageInsightsScreen(
                        vaultViewModel = vaultViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("change_key") {
                    com.ciphervault.app.ui.screens.ChangeKeyScreen(
                        authViewModel = authViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("backup") {
                    BackupScreen(
                        backupViewModel = backupViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("image_preview/{fileId}") { backStackEntry ->
                    val fileId = backStackEntry.arguments?.getString("fileId")?.toLongOrNull()
                    if (fileId != null) {
                        com.ciphervault.app.ui.screens.ImagePreviewScreen(
                            vaultViewModel = vaultViewModel,
                            fileId = fileId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                composable("media_preview/{fileId}") { backStackEntry ->
                    val fileId = backStackEntry.arguments?.getString("fileId")?.toLongOrNull()
                    if (fileId != null) {
                        com.ciphervault.app.ui.screens.MediaPreviewScreen(
                            vaultViewModel = vaultViewModel,
                            fileId = fileId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showBottomBar && isBottomBarVisible.value,
                enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingNavPill(
                    selected = currentTab,
                    onSelect = { tab -> navigateToTab(tab.route) },
                    hazeState = hazeState,
                )
            }
        }
    }
}

