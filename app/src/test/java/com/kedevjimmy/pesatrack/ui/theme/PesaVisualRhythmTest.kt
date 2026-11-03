package com.kedevjimmy.pesatrack.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class PesaVisualRhythmTest {
    @Test
    fun commonSpacingAndControlTargetsUseCalmRhythm() {
        assertEquals(16.dp, PesaDesignTokens.screenHorizontalPadding)
        assertEquals(12.dp, PesaDesignTokens.sectionGap)
        assertEquals(16.dp, PesaDesignTokens.cardContentPadding)
        assertEquals(48.dp, PesaDesignTokens.minimumTouchTarget)
        assertEquals(64.dp, PesaDesignTokens.bottomBarHeight)
        assertEquals(48.dp, PesaDesignTokens.selectedTabHeight)
    }

    @Test
    fun neutralCardsStayLightAndHeadingsStayReadable() {
        assertEquals(1.dp, PesaDesignTokens.lightCardElevation)
        assertEquals(20.dp, PesaDesignTokens.cardRadius)
        assertEquals(FontWeight.Bold, PesaTypography.displaySmall.fontWeight)
        assertEquals(FontWeight.Bold, PesaTypography.headlineLarge.fontWeight)
        assertEquals(FontWeight.SemiBold, PesaTypography.titleMedium.fontWeight)
        assertEquals(FontWeight.Normal, PesaTypography.bodyMedium.fontWeight)
    }

    @Test
    fun moreWorkspaceTilesKeepCompactButTappableGeometry() {
        assertEquals(96.dp, MoreVisualTokens.workspaceTileHeight)
        assertEquals(36.dp, MoreVisualTokens.iconMedium)
        assertEquals(48.dp, MoreVisualTokens.touchTarget)
    }
}
