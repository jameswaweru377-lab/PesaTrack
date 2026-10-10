package com.kedevjimmy.pesatrack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.components.ConceptEmeraldHero
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** F16: verify actual text layout, not just semantic presence, in a narrow hero. */
@RunWith(AndroidJUnit4::class)
class AuditAccessibilityUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narrowHeroAt130PercentInLightTheme() = assertHeroReadable(1.3f, false)
    @Test fun narrowHeroAt130PercentInDarkTheme() = assertHeroReadable(1.3f, true)
    @Test fun narrowHeroAt200PercentInLightTheme() = assertHeroReadable(2f, false)
    @Test fun narrowHeroAt200PercentInDarkTheme() = assertHeroReadable(2f, true)

    private fun assertHeroReadable(fontScale: Float, dark: Boolean) {
        val texts = listOf("Money spent", "KES 12,345,678", "Fees are tracked separately from spending")
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                PesaTrackTheme(darkTheme = dark) {
                    Box(Modifier.width(320.dp)) {
                        ConceptEmeraldHero(
                            title = texts[0], amount = texts[1], subtitle = texts[2],
                            artwork = false, modifier = Modifier.testTag("audit-hero")
                        )
                    }
                }
            }
        }
        val hero = compose.onNodeWithTag("audit-hero").fetchSemanticsNode().boundsInRoot
        val bounds = texts.map { text ->
            val node = compose.onNodeWithText(text, useUnmergedTree = true)
            node.assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                assertTrue("Text must expose its rendered layout: $text", action(layouts))
            }
            assertEquals(1, layouts.size)
            val layout = layouts.single()
            assertFalse("Critical text must not overflow at ${fontScale}x: $text", layout.hasVisualOverflow)
            for (line in 0 until layout.lineCount) {
                assertFalse("Critical text must not be ellipsized: $text", layout.isLineEllipsized(line))
            }
            node.fetchSemanticsNode().boundsInRoot.also { rect ->
                assertTrue("Critical text must fit horizontally inside the hero", rect.left >= hero.left && rect.right <= hero.right)
                assertTrue("Critical text must fit vertically inside the hero", rect.top >= hero.top && rect.bottom <= hero.bottom)
            }
        }
        assertTrue("Title and amount must not overlap", bounds[0].bottom <= bounds[1].top)
        assertTrue("Amount and explanation must not overlap", bounds[1].bottom <= bounds[2].top)
    }
}
