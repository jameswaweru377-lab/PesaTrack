"""Fast, dependency-free guard for the approved premium Compose palette.

This is a source contract, not an Android rendering or accessibility test.
"""

from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[1]
THEME = ROOT / "app/src/main/java/com/kedevjimmy/pesatrack/ui/theme"
SCREENS = ROOT / "app/src/main/java/com/kedevjimmy/pesatrack/ui/screens"
COMPONENTS = ROOT / "app/src/main/java/com/kedevjimmy/pesatrack/ui/components"


def source(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def contrast_ratio(hex_a: str, hex_b: str) -> float:
    def luminance(hex_rgb: str) -> float:
        values = [int(hex_rgb[offset:offset + 2], 16) / 255 for offset in (0, 2, 4)]
        rgb = [v / 12.92 if v <= .04045 else ((v + .055) / 1.055) ** 2.4 for v in values]
        return sum(v * w for v, w in zip(rgb, (.2126, .7152, .0722)))

    light, dark = sorted((luminance(hex_a), luminance(hex_b)), reverse=True)
    return (light + .05) / (dark + .05)


class PremiumUiContractTest(unittest.TestCase):
    def test_one_reference_palette_owns_approved_profile_colours(self):
        self.assertTrue((THEME / "PremiumVisualTokens.kt").exists(), "PremiumVisualTokens.kt is missing")
        palette = source(THEME / "PremiumVisualTokens.kt")
        for colour in ("0xFF6B381A", "0xFFFCF0E7", "0xFFFFF8F3", "0xFFF3E3D6", "0xFFE3D0C0", "0xFF94B95B"):
            with self.subTest(colour=colour):
                self.assertIn(colour, palette)
        profile = source(THEME / "ProfileVisualColors.kt")
        self.assertIn("PremiumVisualTokens.coffee", profile)
        self.assertIn("PremiumVisualTokens.cream", profile)

    def test_material_light_theme_uses_shared_neutral_palette(self):
        theme = source(THEME / "Theme.kt")
        self.assertIn("primary = PremiumVisualTokens.coffee", theme)
        self.assertIn("background = PremiumVisualTokens.cream", theme)
        self.assertIn("surface = PremiumVisualTokens.card", theme)
        self.assertNotIn("primary = AuroraIndigo,", theme)

    def test_more_and_plan_no_longer_override_the_premium_identity(self):
        more = source(THEME / "MoreMonochromeTheme.kt")
        self.assertNotIn("primary = Color.Black", more)
        plan = source(SCREENS / "PlanVibrantComponents.kt")
        self.assertIn("val violet = PremiumVisualTokens.coffee", plan)
        self.assertIn("val canvas = PremiumVisualTokens.cream", plan)
        self.assertNotIn("0xFF6416F5", plan)

    def test_shared_card_geometry_remains_compact_and_tappable(self):
        tokens = source(THEME / "PesaDesignTokens.kt")
        self.assertIn("val minimumTouchTarget = 48.dp", tokens)
        self.assertIn("val sectionGap", tokens)
        self.assertIn("val innerRadius", tokens)

    def test_active_navigation_and_plan_use_the_unified_palette(self):
        navigation = source(SCREENS / "BrownHomeNavigation.kt")
        self.assertIn("val canvas = PremiumVisualTokens.cream", navigation)
        self.assertIn("val coffee = PremiumVisualTokens.coffee", navigation)
        shell = source(ROOT / "app/src/main/java/com/kedevjimmy/pesatrack/ui/components/AppScaffold.kt")
        self.assertIn("containerColor = MaterialTheme.colorScheme.background", shell)
        self.assertNotIn("currentTab == MainTab.ACCOUNTS -> BrownHomePalette.canvas", shell)
        plan = source(SCREENS / "PlanVibrantComponents.kt")
        self.assertNotIn("0xFF790FFF", plan)
        self.assertNotIn("0xFF6920EF", plan)

    def test_statement_header_and_activity_account_review_have_no_purple_surface(self):
        statements = source(SCREENS / "StatementUnifiedOverviewPage.kt")
        self.assertNotIn("0xFF8B43FF", statements)
        self.assertNotIn("0xFFEDEBFF", statements)
        activity = source(SCREENS / "ActivityScreen.kt")
        self.assertNotIn("0xFFEDE6FF", activity)
        self.assertNotIn("0xFFE9E2FF", activity)

    def test_paycheck_import_action_uses_coffee_instead_of_legacy_purple(self):
        paycheck = source(SCREENS / "PaycheckDashboard.kt")
        self.assertNotIn("0xFF6833DB", paycheck)
        self.assertNotIn("0xFF4B319C", paycheck)
        self.assertIn("PremiumVisualTokens.coffee", paycheck)

    def test_reachable_concept_hero_and_tiles_use_shared_premium_palette(self):
        concepts = source(COMPONENTS / "ReferenceConceptComponents.kt")
        for legacy in ("0xFF5B55E7", "0xFF7C6FF2", "0xFF3933A8", "0xFFA69CFF", "0xFFF0EFFF"):
            with self.subTest(legacy=legacy):
                self.assertNotIn(legacy, concepts)
        self.assertIn("PremiumVisualTokens.coffee", concepts)
        self.assertIn("PremiumVisualTokens.cream", concepts)

    def test_bills_and_goals_illustrated_progress_no_longer_reuses_indigo(self):
        bills = source(SCREENS / "BillsReferenceScreen.kt")
        goals = source(SCREENS / "GoalReferenceScreen.kt")
        self.assertNotIn("0xFF3933A8", bills)
        for legacy in ("0xFF7C6FF2", "0xFF5B55E7", "0xFF4F46D8"):
            with self.subTest(legacy=legacy):
                self.assertNotIn(legacy, goals)

    def test_statement_filters_and_cards_share_premium_surface(self):
        unified = source(SCREENS / "StatementUnifiedOverviewPage.kt")
        overview = source(SCREENS / "StatementOverviewPage.kt")
        self.assertNotIn("Surface(color = Color.White, shape = RoundedCornerShape(14.dp)", unified)
        self.assertNotIn("color = Color.White,\n        shape = RoundedCornerShape(22.dp)", overview)
        self.assertNotIn("color = Color.White,\n        shape = RoundedCornerShape(19.dp)", overview)

    def test_core_text_colours_reach_wcag_aa_contrast(self):
        palette = source(THEME / "PremiumVisualTokens.kt")
        values = dict(re.findall(r"val (\w+) = Color\(0xFF([0-9A-F]{6})\)", palette))
        pairs = (
            ("coffee", "card"), ("ink", "cream"),
            ("muted", "cream"), ("leafReadable", "cream"),
            ("darkText", "darkCanvas"), ("darkCoffee", "darkCanvas")
        )
        for foreground, background in pairs:
            with self.subTest(foreground=foreground, background=background):
                self.assertGreaterEqual(contrast_ratio(values[foreground], values[background]), 4.5)
        # The lighter leaf is only a decorative accent, never small text.
        self.assertLess(contrast_ratio(values["leaf"], values["card"]), 4.5)


if __name__ == "__main__":
    unittest.main()
