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
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import android.util.Log
import java.io.File
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
        // Gather all three layouts before asserting so the first failure does not hide evidence.
        val rendered = texts.map { text ->
            val node = compose.onNodeWithText(text, useUnmergedTree = true)
            node.assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                assertTrue("Text must expose its rendered layout: $text", action(layouts))
            }
            assertEquals(1, layouts.size)
            Triple(text, layouts.single(), node.fetchSemanticsNode().boundsInRoot)
        }
        val diagnostics = buildString {
            appendLine("fontScale=$fontScale dark=$dark hero=$hero")
            rendered.forEach { (text, layout, rect) ->
                appendLine("text=$text bounds=$rect size=${layout.size} constraints=${layout.layoutInput.constraints} density=${layout.layoutInput.density}")
                appendLine("paragraphWidth=${layout.multiParagraph.width} paragraphHeight=${layout.multiParagraph.height} overflowWidth=${layout.didOverflowWidth} overflowHeight=${layout.didOverflowHeight} visualOverflow=${layout.hasVisualOverflow} maxLines=${layout.layoutInput.maxLines} style=${layout.layoutInput.style}")
                for (line in 0 until layout.lineCount) {
                    appendLine("line=$line top=${layout.getLineTop(line)} bottom=${layout.getLineBottom(line)} baseline=${layout.getLineBaseline(line)} left=${layout.getLineLeft(line)} right=${layout.getLineRight(line)} ellipsized=${layout.isLineEllipsized(line)}")
                }
            }
        }
        Log.i("AuditAccessibility", diagnostics)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val artifactDir = File(instrumentation.targetContext.getExternalFilesDir(null), "audit-accessibility").apply { mkdirs() }
        val artifactName = "hero-${fontScale}-${if (dark) "dark" else "light"}"
        File(artifactDir, "$artifactName.txt").writeText(diagnostics)
        instrumentation.uiAutomation.takeScreenshot()?.let { screenshot ->
            File(artifactDir, "$artifactName.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
            screenshot.recycle()
        }
        val bounds = rendered.map { (text, layout, rect) ->
            assertFalse("Critical text must not overflow at ${fontScale}x: $text\n$diagnostics", layout.hasVisualOverflow)
            for (line in 0 until layout.lineCount) {
                assertFalse("Critical text must not be ellipsized: $text\n$diagnostics", layout.isLineEllipsized(line))
            }
            assertTrue("Critical text must fit horizontally inside the hero\n$diagnostics", rect.left >= hero.left && rect.right <= hero.right)
            assertTrue("Critical text must fit vertically inside the hero\n$diagnostics", rect.top >= hero.top && rect.bottom <= hero.bottom)
            rect
        }
        assertTrue("Title and amount must not overlap", bounds[0].bottom <= bounds[1].top)
        assertTrue("Amount and explanation must not overlap", bounds[1].bottom <= bounds[2].top)
    }
}
