package com.ciphervault.app.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

enum class VaultTab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Outlined.Home),
    Vault("vault", "Vault", Icons.Outlined.Lock),
    Encrypt("encrypt", "Encrypt", Icons.Outlined.Key),
    Settings("settings", "Settings", Icons.Outlined.Tune);

    companion object {
        fun fromRoute(route: String?): VaultTab = entries.firstOrNull { it.route == route } ?: Home
    }
}

private val PillHeight = 64.dp
private val PillBottomGap = 24.dp

@Composable
fun rememberNavPillClearance(): Dp {
    val inset = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    return PillHeight + PillBottomGap + 16.dp + inset
}

@Composable
fun FloatingNavPill(
    selected: VaultTab,
    onSelect: (VaultTab) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    tabs: List<VaultTab> = VaultTab.entries,
) {
    val shape = RoundedCornerShape(32.dp)
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = PillBottomGap)
            .fillMaxWidth(0.8f)
            .widthIn(max = 320.dp)
            .height(PillHeight)
            .shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .then(
                if (blurSupported) {
                    Modifier.hazeChild(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = VaultColors.Ground,
                            tint = HazeTint(VaultColors.GlassFill),
                            blurRadius = 24.dp,
                            noiseFactor = 0f,
                        ),
                    )
                } else {
                    Modifier.background(VaultColors.GlassFallback)
                }
            )
            .border(1.dp, VaultColors.GlassBorder, shape)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            NavPillItem(
                tab = tab,
                selected = tab == selected,
                onClick = { onSelect(tab) },
            )
        }
    }
}

@Composable
private fun NavPillItem(tab: VaultTab, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) VaultColors.Accent else Color.Transparent,
        animationSpec = tween(250), label = "navBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) VaultColors.OnAccent else VaultColors.TextSecondary,
        animationSpec = tween(250), label = "navFg",
    )
    val hPad by animateDpAsState(
        targetValue = if (selected) 18.dp else 0.dp,
        animationSpec = tween(250), label = "navPad",
    )

    Row(
        modifier = Modifier
            .height(48.dp)
            .defaultMinSize(minWidth = 48.dp)
            .clip(CircleShape)
            .background(bg)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = tab.label }
            .padding(horizontal = hPad),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.width(20.dp).height(20.dp),
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(250)) + expandHorizontally(tween(250)),
            exit = fadeOut(tween(150)) + shrinkHorizontally(tween(250)),
        ) {
            Row {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tab.label,
                    style = VaultType.Chip,
                    color = fg,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}
