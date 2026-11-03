package com.ciphervault.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.R
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import java.util.Locale
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableIntStateOf

/** The two languages the app will offer. */
enum class AppLanguage(val tag: String, val label: String) {
    English("en", "English"),
    Arabic("ar", "العربية");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { tag != null && tag.startsWith(it.tag) } ?: English

        fun fromSystem(): AppLanguage = fromTag(Locale.getDefault().language)
    }
}

private val PagerHeight = 172.dp

/**
 * First-time welcome screen, styled after the "Noir / Portal" design. Colours and type come
 * from the app theme (VaultColors and VaultType).
 *
 * An arched portal holding the Key Zone "K", a swipeable set of headlines about what the app
 * does, and the two ways in: create a new vault or import an existing one.
 *
 * The English / Arabic switch is a placeholder: it reports the choice through
 * [onLanguageChange] but doesn't translate or mirror anything yet.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onCreateVault: () -> Unit,
    onImportVault: () -> Unit,
    initialLanguage: AppLanguage = AppLanguage.English,
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    val context = LocalContext.current
    var backPressCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(backPressCount) {
        if (backPressCount > 0) {
            delay(2000)
            backPressCount = 0
        }
    }
    
    BackHandler {
        if (backPressCount == 0) {
            backPressCount++
            Toast.makeText(context, "Press back button again to exit", Toast.LENGTH_SHORT).show()
        } else {
            val activity = context as? android.app.Activity
            activity?.finish()
        }
    }

    // PLACEHOLDER: the switch only remembers which side was tapped and reports it through
    // onLanguageChange. Nothing on this screen changes language yet; the copy is English only.
    var languageTag by rememberSaveable { mutableStateOf(initialLanguage.tag) }
    val language = AppLanguage.fromTag(languageTag)

    val pages = remember { welcomePages() }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    // The portal builds itself first, then the text and actions follow.
    var portalEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { portalEntered = true }
    val entrance = remember { MutableTransitionState(false).apply { targetState = true } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Ground)
            .systemBarsPadding()
            .padding(horizontal = 36.dp)
    ) {
        // ---- Name and language ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Key Zone".uppercase(),
                style = VaultType.Wordmark,
                color = VaultColors.Accent
            )
            LanguageSwitch(
                selected = language,
                onSelect = {
                    languageTag = it.tag
                    onLanguageChange(it)
                }
            )
        }

        // ---- Portal: takes whatever height is left, up to the design's 300dp ----
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val archHeight = (maxHeight - 32.dp).coerceIn(120.dp, 300.dp)
            PortalArch(height = archHeight, entered = portalEntered)
        }

        // ---- Headlines: swipe for what Key Zone does ----
        AnimatedVisibility(
            visibleState = entrance,
            enter = fadeIn(tween(600, delayMillis = 500)) +
                    slideInVertically(tween(600, delayMillis = 500)) { it / 8 }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PagerHeight)
                ) { index ->
                    HeadlinePage(page = pages[index])
                }

                Spacer(modifier = Modifier.height(4.dp))

                PagerDashes(count = pages.size, current = pagerState.currentPage)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ---- Actions ----
        AnimatedVisibility(
            visibleState = entrance,
            enter = fadeIn(tween(500, delayMillis = 700)) +
                    slideInVertically(tween(500, delayMillis = 700)) { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Button(
                    onClick = onCreateVault,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaultColors.TextPrimary,
                        contentColor = VaultColors.Ground
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Create new vault", style = VaultType.Button.copy(fontSize = 16.sp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onImportVault,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Import existing vault",
                        style = VaultType.ItemTitle,
                        color = VaultColors.TextPrimary
                    )
                }
            }
        }
    }
}

// =====================================================================
//  Pieces
// =====================================================================

/**
 * Three nested arches, like a doorway. The innermost is solid gold and holds the "K", drawn in
 * the ground colour so its keyhole lets the gold show through.
 */
@Composable
private fun PortalArch(height: Dp, entered: Boolean, modifier: Modifier = Modifier) {
    val width = height * (220f / 300f)
    val arch = RoundedCornerShape(
        topStartPercent = 50,
        topEndPercent = 50,
        bottomEndPercent = 0,
        bottomStartPercent = 0
    )

    val outerAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(700),
        label = "archOuter"
    )
    val middleAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(700, delayMillis = 150),
        label = "archMiddle"
    )
    val innerAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(700, delayMillis = 300),
        label = "archInner"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(600, delayMillis = 550),
        label = "archLogo"
    )

    val innerWidth = width * (132f / 220f)

    Box(
        modifier = modifier
            .size(width, height)
            .alpha(outerAlpha)
            .border(1.dp, VaultColors.Accent, arch),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .size(width * (176f / 220f), height * (262f / 300f))
                .alpha(middleAlpha)
                .border(1.dp, VaultColors.Accent.copy(alpha = 0.45f), arch),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .size(innerWidth, height * (224f / 300f))
                    .alpha(innerAlpha)
                    .clip(arch)
                    .background(VaultColors.Accent),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.keyzone_logo),
                    contentDescription = "Key Zone",
                    colorFilter = ColorFilter.tint(VaultColors.Ground),
                    modifier = Modifier
                        .padding(top = innerWidth * 0.14f)
                        .size(innerWidth * 0.52f)
                        .alpha(logoAlpha)
                )
            }
        }
    }
}

/** A two-line headline in the design's style: cream first line, italic gold second line. */
@Composable
private fun HeadlinePage(page: WelcomePage) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = page.lineOne,
            style = VaultType.Headline,
            color = VaultColors.TextPrimary
        )
        Text(
            text = page.lineTwo,
            style = VaultType.Headline.copy(fontStyle = FontStyle.Italic),
            color = VaultColors.Accent
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = page.body,
            style = VaultType.Meta.copy(fontSize = 14.sp, lineHeight = 21.sp),
            color = VaultColors.TextSecondary,
            maxLines = 3
        )
    }
}

@Composable
private fun PagerDashes(count: Int, current: Int) {
    Row(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val active = index == current
            val width by animateDpAsState(
                targetValue = if (active) 24.dp else 8.dp,
                animationSpec = tween(250),
                label = "dashWidth$index"
            )
            val color by animateColorAsState(
                targetValue = if (active) VaultColors.Accent else VaultColors.Accent.copy(alpha = 0.25f),
                animationSpec = tween(250),
                label = "dashColor$index"
            )
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** PLACEHOLDER switch: shows the choice and reports it, but doesn't change the language yet. */
@Composable
private fun LanguageSwitch(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, VaultColors.Accent.copy(alpha = 0.35f), CircleShape)
            .padding(3.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLanguage.entries.forEach { language ->
            val isSelected = language == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) VaultColors.Accent else Color.Transparent,
                animationSpec = tween(200),
                label = "languageBg${language.tag}"
            )
            Text(
                text = language.label,
                style = VaultType.Chip.copy(fontSize = 12.sp),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) VaultColors.Ground else VaultColors.TextSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(background)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(language) }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

// =====================================================================
//  Copy
// =====================================================================

private class WelcomePage(val lineOne: String, val lineTwo: String, val body: String)

private fun welcomePages(): List<WelcomePage> = listOf(
    WelcomePage(
        lineOne = "Locked away,",
        lineTwo = "on your device.",
        body = "Every file is encrypted with AES-256-GCM before it goes into the vault."
    ),
    WelcomePage(
        lineOne = "One key,",
        lineTwo = "only yours.",
        body = "Your PIN or password derives the master key using PBKDF2 (150k rounds). Fingerprint unlock is optional."
    ),
    WelcomePage(
        lineOne = "A second lock,",
        lineTwo = "if you want it.",
        body = "Add an authenticator app for a 6-digit code, plus 8 emergency recovery codes."
    ),
    WelcomePage(
        lineOne = "Files, notes,",
        lineTwo = "and logins.",
        body = "Keep them together, then search, sort, tag and preview them without leaving the app."
    ),
    WelcomePage(
        lineOne = "Take it",
        lineTwo = "with you.",
        body = "Back up to a .cvault archive and import it on any new phone. Files stay encrypted inside."
    )
)