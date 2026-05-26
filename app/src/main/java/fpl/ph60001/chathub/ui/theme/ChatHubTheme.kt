package fpl.ph60001.chathub.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// ═══════════════════════════════════════════════════════
//  ChatHub Premium Design System — Purple Aurora Theme
// ═══════════════════════════════════════════════════════

// --- Nền tối chủ đạo ---
val DarkBg          = Color(0xFF0D0D1A)   // Đen tím rất sâu
val DarkSurface     = Color(0xFF13131F)   // Bề mặt card
val DarkCard        = Color(0xFF1A1A2E)   // Card nền

// --- Màu chính Purple-Violet ---
val PrimaryViolet   = Color(0xFF7C3AED)   // Violet 600
val PrimaryPurple   = Color(0xFF9333EA)   // Purple 600
val AccentPink      = Color(0xFFEC4899)   // Pink 500
val AccentIndigo    = Color(0xFF6366F1)   // Indigo 500

// --- Màu phụ trợ ---
val GlowPurple      = Color(0x407C3AED)   // Violet glow (alpha 25%)
val GlowPink        = Color(0x30EC4899)   // Pink glow (alpha 19%)
val BorderColor     = Color(0x267C3AED)   // Violet border nhẹ
val BorderBright    = Color(0x557C3AED)   // Violet border sáng hơn

// --- Tin nhắn bubble ---
val BubbleMe        = Color(0xFF6D28D9)   // Violet 700 — tin nhắn của mình
val BubbleOther     = Color(0xFF1E1B4B)   // Indigo 950 — tin nhắn đối phương

// --- Trạng thái ---
val OnlineGreen     = Color(0xFF10B981)   // Emerald 500
val ErrorRed        = Color(0xFFEF4444)   // Red 500
val WarningAmber    = Color(0xFFF59E0B)   // Amber 500
val AdminGold       = Color(0xFFEAB308)   // Yellow 500

// --- Text ---
val TextPrimary     = Color(0xFFF8FAFC)   // Slate 50
val TextSecondary   = Color(0xFF94A3B8)   // Slate 400
val TextMuted       = Color(0xFF475569)   // Slate 600

// ═══════════════════════════════════════════════════════
//  Gradient Presets
// ═══════════════════════════════════════════════════════

val GradientMain = Brush.verticalGradient(
    colors = listOf(DarkBg, DarkCard, DarkBg)
)

val GradientPrimary = Brush.linearGradient(
    colors = listOf(PrimaryViolet, PrimaryPurple, AccentPink)
)

val GradientButton = Brush.horizontalGradient(
    colors = listOf(PrimaryViolet, AccentIndigo)
)

val GradientBubbleMe = Brush.linearGradient(
    colors = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))
)

val GradientCard = Brush.linearGradient(
    colors = listOf(
        Color(0xFF1A1A2E),
        Color(0xFF16213E)
    )
)

val GradientHeader = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0D0D1A),
        Color(0x000D0D1A)
    )
)

// ═══════════════════════════════════════════════════════
//  Aliases tương thích ngược (legacy names → new colors)
// ═══════════════════════════════════════════════════════
val NeonBlue     = PrimaryViolet          // #7C3AED  (was #64D2FF)
val NeonCyan     = AccentPink             // #EC4899  (was #00F2FE)
val GreenOnline  = OnlineGreen            // #10B981  (unchanged concept)
val GlassCard    = DarkCard               // #1A1A2E  (was #331E293B)
val GlassBorder  = BorderColor            // #267C3AED (was #3364D2FF)

