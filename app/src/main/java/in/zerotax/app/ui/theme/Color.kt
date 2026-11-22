package `in`.zerotax.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================================================
// ENTERPRISE FINTECH COLOR SYSTEM (Sovereign Slate & Deep Teal)
// ============================================================================

// ============================================================================
// ENTERPRISE FINTECH COLOR SYSTEM (Sovereign Slate & Deep Teal)
// Single Source of Truth: update variables here to re-theme the entire application.
// ============================================================================

// Sovereign Neutral Slate Foundation (Modern High-Contrast Architecture)
val Slate950 = Color(0xFF0A0F1D) // Deepest Midnight Obsidian backdrop
val Slate900 = Color(0xFF0F172A) // Rich Charcoal / Primary text on light
val Slate850 = Color(0xFF111827) // Elevated dark card surface
val Slate800 = Color(0xFF1E293B) // Dark surface container
val Slate700 = Color(0xFF334155) // Dark border / muted text on dark
val Slate600 = Color(0xFF475569) // Secondary text
val Slate500 = Color(0xFF64748B) // Tertiary / Muted labels
val Slate400 = Color(0xFF94A3B8) // Placeholder text / subtle borders
val Slate300 = Color(0xFFCBD5E1) // Light card border
val Slate200 = Color(0xFFE2E8F0) // Subtle dividers
val Slate100 = Color(0xFFF1F5F9) // Light surface variant
val Slate50  = Color(0xFFF8FAFC) // Primary light backdrop (clean porcelain)
val PureWhite = Color(0xFFFFFFFF) // Clean high-contrast card surface

// Modern Luxury Emerald & Sovereign Spruce Accent System (Authoritative, Luminous & Modern)
val EnterpriseTeal900 = Color(0xFF063326) // Deepest Luxury Spruce
val EnterpriseTeal800 = Color(0xFF064E3B) // Deep Forest
val EnterpriseTeal700 = Color(0xFF047857) // Rich British Racing Green
val EnterpriseTeal600 = Color(0xFF059669) // Modern Vivid Emerald (Brand Primary)
val EnterpriseTeal500 = Color(0xFF10B981) // Radiant Mint (Interactive CTA / Highlights)
val EnterpriseTeal400 = Color(0xFF34D399) // Luminous Glow Accent
val EnterpriseTeal300 = Color(0xFF6EE7B7) // Light Mint
val EnterpriseTeal200 = Color(0xFFA7F3D0) // Soft Pastel Mint
val EnterpriseTeal100 = Color(0xFFD1FAE5) // Delicate Tint Container
val EnterpriseTeal50  = Color(0xFFF0FDF4) // Ultra-clean subtle highlight

// Semantic Status Colors (High-Contrast, Vivid FinTech Badges)
val EmeraldSuccess     = Color(0xFF059669) // Verified / Zero Discrepancy
val EmeraldDark        = Color(0xFF064E3B)
val EmeraldSuccessBg   = Color(0xFFECFDF5)
val EmeraldSuccessBgDark = Color(0xFF064E3B)

val AmberWarning       = Color(0xFFD97706) // Attention / Review Needed
val AmberWarningBg     = Color(0xFFFFFBEB)
val AmberWarningBgDark = Color(0xFF451A03)
val AmberWarningDark   = Color(0xFFFDE68A)

val CrimsonDanger      = Color(0xFFDC2626) // Section 270A Penalty / Critical Error
val CrimsonDangerBg    = Color(0xFFFEF2F2)
val CrimsonDangerBgDark = Color(0xFF450A0A)
val CrimsonDangerDark  = Color(0xFFFCA5A5)

val IndigoStatutory    = Color(0xFF4338CA) // Modern Electric Indigo Act / Clause Ref
val IndigoStatutoryDark = Color(0xFF818CF8) // Radiant Indigo
val IndigoStatutoryBg  = Color(0xFFEEF2FF)
val IndigoStatutoryBgDark = Color(0xFF1E1B4B)

// Unified Single-Source Palette Tokens (Mapped directly to the primary design variables)
val PalettePrimary = EnterpriseTeal700      // Primary Text & Accents
val PaletteAccent = EnterpriseTeal600       // Brand Interactive Buttons & Highlights
val PaletteSecondary = Slate600             // Supporting Labels & Dividers
val PaletteSurface = PureWhite              // Crisp Elevated Cards
val PaletteMuted = Slate100                 // Subtle Pills & Backgrounds
val PaletteBackground = Slate50             // Clean App Backdrop

// ============================================================================
// CENTRALIZED BUTTON THEME VARIABLES (Easily customizable across all screens)
// ============================================================================
val ButtonPrimaryBg = EnterpriseTeal600
val ButtonPrimaryBgDark = EnterpriseTeal500
val ButtonPrimaryText = PureWhite
val ButtonPrimaryTextDark = PureWhite

val ButtonSuccessBg = EmeraldSuccess
val ButtonSuccessBgDark = Color(0xFF10B981)
val ButtonSuccessText = PureWhite

val ButtonSecondaryBg = EnterpriseTeal50
val ButtonSecondaryBgDark = EnterpriseTeal800
val ButtonSecondaryText = EnterpriseTeal700
val ButtonSecondaryTextDark = EnterpriseTeal100

val ButtonOutlinedBorder = EnterpriseTeal600
val ButtonOutlinedBorderDark = EnterpriseTeal500
val ButtonOutlinedText = EnterpriseTeal600
val ButtonOutlinedTextDark = EnterpriseTeal300

// ============================================================================
// ZERO TAX BRAND COLOR TOKENS (Single Source of Truth)
// ============================================================================
object ZeroTaxBrandColors {
    val Primary = EnterpriseTeal600
    val PrimaryDark = EnterpriseTeal900
    val PrimaryMedium = EnterpriseTeal700
    val PrimaryInteractive = EnterpriseTeal500
    val PrimaryLight = EnterpriseTeal100
    val PrimaryContainer = EnterpriseTeal50

    val SlateDeep = Slate950
    val SlateDark = Slate900
    val SlateElevated = Slate850
    val SlateCard = Slate800
    val SlateMuted = Slate700
    val SlateTextSecondary = Slate600
    val SlateTextTertiary = Slate500
    val SlateBorder = Slate300
    val SlateDivider = Slate200
    val SlateSurface = Slate100
    val SlateBackdrop = Slate50
    val White = PureWhite

    val Success = EmeraldSuccess
    val SuccessBg = EmeraldSuccessBg
    val Warning = AmberWarning
    val WarningBg = AmberWarningBg
    val Danger = CrimsonDanger
    val DangerBg = CrimsonDangerBg
    val Statutory = IndigoStatutory
    val StatutoryBg = IndigoStatutoryBg
}

// Theme-Adaptive Color Tokens for High-Contrast Light & Dark Mode
@Composable
fun getEmeraldSuccessBg(): Color = if (isSystemInDarkTheme()) EmeraldSuccessBgDark else EmeraldSuccessBg

@Composable
fun getAmberWarningBg(): Color = if (isSystemInDarkTheme()) AmberWarningBgDark else AmberWarningBg

@Composable
fun getCrimsonDangerBg(): Color = if (isSystemInDarkTheme()) CrimsonDangerBgDark else CrimsonDangerBg

@Composable
fun getIndigoStatutoryBg(): Color = if (isSystemInDarkTheme()) IndigoStatutoryBgDark else IndigoStatutoryBg

@Composable
fun getTealHighlightBg(): Color = if (isSystemInDarkTheme()) EnterpriseTeal800 else EnterpriseTeal50

@Composable
fun getTealHighlightText(): Color = if (isSystemInDarkTheme()) EnterpriseTeal100 else EnterpriseTeal900

@Composable
fun getTealHighlightIcon(): Color = if (isSystemInDarkTheme()) EnterpriseTeal300 else EnterpriseTeal700

@Composable
fun getButtonPrimaryBg(): Color = if (isSystemInDarkTheme()) ButtonPrimaryBgDark else ButtonPrimaryBg

@Composable
fun getButtonPrimaryText(): Color = if (isSystemInDarkTheme()) ButtonPrimaryTextDark else ButtonPrimaryText

@Composable
fun getButtonSuccessBg(): Color = if (isSystemInDarkTheme()) ButtonSuccessBgDark else ButtonSuccessBg

@Composable
fun getButtonSecondaryBg(): Color = if (isSystemInDarkTheme()) ButtonSecondaryBgDark else ButtonSecondaryBg

@Composable
fun getButtonSecondaryText(): Color = if (isSystemInDarkTheme()) ButtonSecondaryTextDark else ButtonSecondaryText

@Composable
fun getButtonOutlinedBorder(): Color = if (isSystemInDarkTheme()) ButtonOutlinedBorderDark else ButtonOutlinedBorder

@Composable
fun getButtonOutlinedText(): Color = if (isSystemInDarkTheme()) ButtonOutlinedTextDark else ButtonOutlinedText

