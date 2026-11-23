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

// Soft Warm Canvas & Elevated Card System (Matching Reference Design)
val CanvasWarm = Color(0xFFF4F6F5) // Soft warm off-white canvas
val Slate950 = Color(0xFF0F1513) // Deepest charcoal / dark canvas
val Slate900 = Color(0xFF151C19) // Rich deep charcoal / Primary text
val Slate850 = Color(0xFF1E2623) // Elevated dark card surface
val Slate800 = Color(0xFF28322E) // Dark surface container
val Slate700 = Color(0xFF434E49) // Dark border / muted text on dark
val Slate600 = Color(0xFF6B7772) // Secondary text (#6B7772)
val Slate500 = Color(0xFF8E9592) // Muted subtle labels (Greeting / Subtitles)
val Slate400 = Color(0xFFA5AEA9) // Placeholder text / subtle borders
val Slate300 = Color(0xFFD4DAD7) // Light card border
val Slate200 = Color(0xFFE5EAE7) // Subtle dividers
val Slate100 = Color(0xFFECEFE8) // Light surface variant / active nav pill
val Slate50  = Color(0xFFF4F6F5) // Primary light backdrop (soft warm canvas)
val PureWhite = Color(0xFFFFFFFF) // Clean elevated card surface

// Deep Forest Green & Modern Sage Palette
val ForestGreenDark = Color(0xFF133E35) // Deep forest green
val ForestGreenPrimary = Color(0xFF184E42) // Primary brand forest green
val ForestGreenAccent = Color(0xFF1E6152) // Interactive button accent
val SageMintLight = Color(0xFFC0D8CF) // Soft sage / mint for chart bars
val SageMintSubtle = Color(0xFFDCEAE5) // Very light sage tint
val SageMintPill = Color(0xFFE4F0EB) // Soft green pill background
val SageMintBorder = Color(0xFFC0D8CF) // Soft sage / mint border stroke
val RosePillBg = Color(0xFFFCEBEB) // Soft rose pill background
val RosePillText = Color(0xFFD94444) // Rose badge text
val AmberPillBg = Color(0xFFFEF5E7) // Soft amber/gold pill background
val AmberPillText = Color(0xFFB46E13) // Amber badge text
val NavPillActive = Color(0xFFDEE5F5) // Soft active nav selector pill (light lavender/periwinkle tint)

// Legacy alias mappings for backward compatibility
val EnterpriseTeal900 = ForestGreenDark
val EnterpriseTeal800 = ForestGreenPrimary
val EnterpriseTeal700 = ForestGreenPrimary
val EnterpriseTeal600 = ForestGreenAccent
val EnterpriseTeal500 = Color(0xFF2E7D6B)
val EnterpriseTeal400 = Color(0xFF52A390)
val EnterpriseTeal300 = SageMintLight
val EnterpriseTeal200 = SageMintSubtle
val EnterpriseTeal100 = SageMintPill
val EnterpriseTeal50  = Color(0xFFF2F8F5)

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
val PalettePrimary = ForestGreenPrimary     // Primary Text & Accents
val PaletteAccent = ForestGreenPrimary      // Brand Interactive Buttons & Highlights
val PaletteSecondary = Slate600             // Supporting Labels & Dividers
val PaletteSurface = PureWhite              // Crisp Elevated Cards
val PaletteMuted = SageMintPill             // Subtle Pills & Backgrounds
val PaletteBackground = CanvasWarm          // Clean App Backdrop

// ============================================================================
// CENTRALIZED BUTTON THEME VARIABLES (Easily customizable across all screens)
// ============================================================================
val ButtonPrimaryBg = ForestGreenPrimary
val ButtonPrimaryBgDark = ForestGreenPrimary
val ButtonPrimaryText = PureWhite
val ButtonPrimaryTextDark = PureWhite

val ButtonSuccessBg = EmeraldSuccess
val ButtonSuccessBgDark = Color(0xFF10B981)
val ButtonSuccessText = PureWhite

val ButtonSecondaryBg = SageMintPill
val ButtonSecondaryBgDark = Slate800
val ButtonSecondaryText = ForestGreenPrimary
val ButtonSecondaryTextDark = SageMintLight

val ButtonOutlinedBorder = ForestGreenPrimary
val ButtonOutlinedBorderDark = Slate700
val ButtonOutlinedText = ForestGreenPrimary
val ButtonOutlinedTextDark = PureWhite

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

