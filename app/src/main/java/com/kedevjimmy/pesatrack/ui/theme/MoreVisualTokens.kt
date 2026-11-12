package com.kedevjimmy.pesatrack.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared More-page palette and geometry for the approved November 2026 design.
 * A single source of truth for the More hero, workspace tiles and grouped rows.
 * Numeric values are confined to theme tokens; screens consume the tokens only.
 */
object MoreVisualTokens {
    // Keep legacy names for call sites; all identity colours share the app tokens.
    val ink = PremiumVisualTokens.ink
    val muted = PremiumVisualTokens.muted
    val purple = PremiumVisualTokens.coffee
    val lavender = PremiumVisualTokens.sand
    val blue = PremiumVisualTokens.coffee
    val mint = PremiumVisualTokens.leafReadable
    val orange = PremiumVisualTokens.coffee

    val canvasTop = PremiumVisualTokens.cream
    val canvasMiddle = PremiumVisualTokens.cream
    val canvasBottom = PremiumVisualTokens.cream
    val iconStatement = PremiumVisualTokens.coffee
    val iconPurpleSoft = PremiumVisualTokens.sand
    val iconBlueSoft = PremiumVisualTokens.sand
    val iconMintSoft = PremiumVisualTokens.sand

    val heroBase = PremiumVisualTokens.coffee
    val heroBright = Color(0xFF845236)
    val heroDeep = Color(0xFF482411)
    val heroBlue = PremiumVisualTokens.coffee
    val wavePurple = PremiumVisualTokens.sand
    val wavePink = PremiumVisualTokens.leaf
    val waveCyan = PremiumVisualTokens.card
    val avatarBackground = PremiumVisualTokens.card
    val avatarInk = PremiumVisualTokens.coffee
    val actionInk = PremiumVisualTokens.coffee
    val workspaceBorder = PremiumVisualTokens.sandBorder
    val groupBorder = PremiumVisualTokens.sandBorder
    val greenSoft = PremiumVisualTokens.sand
    val green = PremiumVisualTokens.leafReadable
    val orangeSoft = PremiumVisualTokens.sand
    val orangeBright = PremiumVisualTokens.coffee
    val blueSoft = PremiumVisualTokens.sand
    val blueBright = PremiumVisualTokens.coffee
    val purpleSoft = PremiumVisualTokens.sand
    val purpleBright = PremiumVisualTokens.coffee
    val arrowInk = PremiumVisualTokens.coffee
    val rowDivider = PremiumVisualTokens.sandBorder
    val crownGold = PremiumVisualTokens.leaf

    val dividerStroke = .7.dp
    val space1 = 1.dp
    val waveStroke = 1.3.dp
    val space2 = 2.dp
    val space3 = 3.dp
    val space4 = 4.dp
    val space5 = 5.dp
    val space6 = 6.dp
    val space7 = 7.dp
    val space8 = 8.dp
    val space10 = 10.dp
    val space11 = 11.dp
    val space12 = 12.dp
    val space13 = 13.dp
    val space14 = 14.dp
    val space15 = 15.dp
    val space16 = 16.dp
    val space17 = 17.dp
    val space18 = 18.dp
    val space20 = 20.dp
    val space21 = 21.dp
    val space23 = 23.dp
    val space24 = 24.dp
    val space28 = 28.dp
    val iconSmall = 36.dp
    val iconMedium = 36.dp
    val touchTarget = 48.dp
    val listRowHeight = 58.dp
    val avatarSize = 70.dp
    val heroHeight = 104.dp
    val workspaceTileHeight = 96.dp
}
