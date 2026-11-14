package com.jayra.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.jayra.app.util.GlassTier

// ═══════════════════════════════════════════════════════════
// JAYRA — LiquidGlassSpecV2 Contract (v1.0, APPROVED by Manager)
// ═══════════════════════════════════════════════════════════
// Dokumen spec: docs/design/LiquidGlassSpecV2.md
//
// ATURAN:
//  1. File ini HANYA data classes + factory. NOL Composable. NOL render logic.
//  2. Mobile WAJIB pakai factory forTier() — DILARANG hardcode nilai hex/alpha.
//  3. Nilai token DIKUNCI oleh Manager. Perubahan butuh approval Manager baru.
//  4. Additive: glassSurfaceV2() diimplementasi Mobile; glassSurface() existing
//     TIDAK diganti (screen non-auth tetap pakai v1).
//  5. Dependensi: GlassTier (com.jayra.app.util.GlassEffectPrefs).
//     JANGAN hapus GlassTier saat implementasi V2.
// ═══════════════════════════════════════════════════════════

@Immutable
data class GradientStop(
    val color: Color,
    val position: Float  // 0f..1f
)

@Immutable
data class OrbSpec(
    val color: Color,
    val centerXRatio: Float,   // 0f..1f
    val centerYRatio: Float,   // 0f..1f
    val radiusRatio: Float     // 0f..1f, relative to min(width, height)
)

@Immutable
data class BackdropSpec(
    val gradientStops: List<GradientStop>,   // 2..3 stops, top→bottom
    val radialOrbs: List<OrbSpec>,           // 0..3 orbs
    val ambientDriftEnabled: Boolean
)

@Immutable
data class GlassSurfaceSpec(
    val baseAlpha: Float,
    val sheenGradient: List<GradientStop>,   // top→fade
    val borderGradient: List<GradientStop>,  // top-bright→bottom-dim
    val glareRadiusRatio: Float?,            // null = skip glare
    val shadowElevationDp: Int
)

@Immutable
data class ButtonSpec(
    val fillColor: Color,
    val fillAlpha: Float,
    val sheenGradient: List<GradientStop>?,  // null = no sheen (disabled/OFF)
    val borderGradient: List<GradientStop>?, // null = no border (disabled/OFF)
    val pressGlowEnabled: Boolean
)

@Immutable
data class LiquidGlassSpecV2(
    val tier: GlassTier,
    val isDark: Boolean,
    val backdrop: BackdropSpec,
    val surface: GlassSurfaceSpec,
    val primaryButton: ButtonSpec
) {
    companion object {

        // ── Palette (locked) — Spec V2 A.1, A.2, A.5 ──────────────
        private val WHITE = Color(0xFFFFFFFF)

        private val MESH_LIGHT_TOP    = Color(0xFFF5F7FB)
        private val MESH_LIGHT_MID    = Color(0xFFEAF0FA)
        private val MESH_LIGHT_BOTTOM = Color(0xFFDDE5F2)
        private val MESH_LIGHT_ORB_WARM   = Color(0xFFFFD7A8)
        private val MESH_LIGHT_ORB_COOL   = Color(0xFF5AC8FA)
        private val MESH_LIGHT_ORB_ACCENT = Color(0xFF5856D6)

        private val MESH_DARK_TOP    = Color(0xFF0B0F1A)
        private val MESH_DARK_MID    = Color(0xFF080A12)
        private val MESH_DARK_BOTTOM = Color(0xFF000000)
        private val MESH_DARK_ORB_COOL   = Color(0xFF0A84FF)
        private val MESH_DARK_ORB_INDIGO = Color(0xFF5E5CE6)
        private val MESH_DARK_ORB_WARM   = Color(0xFFFF9F0A)

        private val BTN_BLUE = Color(0xFF007AFF)

        // ── Factory utama ─────────────────────────────────────────
        fun forTier(tier: GlassTier, dark: Boolean): LiquidGlassSpecV2 = when (tier) {
            GlassTier.HIGH   -> high(dark)
            GlassTier.MEDIUM -> medium(dark)
            GlassTier.LOW    -> low(dark)
            GlassTier.OFF    -> off(dark)
        }

        // ── HIGH: real blur, full 3-orb backdrop, 4-stop border, glow ──
        private fun high(dark: Boolean): LiquidGlassSpecV2 {
            val backdrop = if (dark) BackdropSpec(
                gradientStops = listOf(
                    GradientStop(MESH_DARK_TOP, 0f),
                    GradientStop(MESH_DARK_MID, 0.55f),
                    GradientStop(MESH_DARK_BOTTOM, 1f)
                ),
                radialOrbs = listOf(
                    OrbSpec(MESH_DARK_ORB_COOL,   0.85f, 0.15f, 0.55f),
                    OrbSpec(MESH_DARK_ORB_INDIGO, 0.15f, 0.85f, 0.60f),
                    OrbSpec(MESH_DARK_ORB_WARM,   0.85f, 0.90f, 0.40f)
                ),
                ambientDriftEnabled = true
            ) else BackdropSpec(
                gradientStops = listOf(
                    GradientStop(MESH_LIGHT_TOP, 0f),
                    GradientStop(MESH_LIGHT_MID, 0.55f),
                    GradientStop(MESH_LIGHT_BOTTOM, 1f)
                ),
                radialOrbs = listOf(
                    OrbSpec(MESH_LIGHT_ORB_COOL,   0.85f, 0.15f, 0.55f),
                    OrbSpec(MESH_LIGHT_ORB_ACCENT, 0.15f, 0.85f, 0.60f),
                    OrbSpec(MESH_LIGHT_ORB_WARM,   0.85f, 0.90f, 0.40f)
                ),
                ambientDriftEnabled = true
            )
            return LiquidGlassSpecV2(
                tier = GlassTier.HIGH,
                isDark = dark,
                backdrop = backdrop,
                surface = GlassSurfaceSpec(
                    baseAlpha = 0.55f,
                    sheenGradient = listOf(
                        GradientStop(WHITE.copy(alpha = 0.18f), 0f),
                        GradientStop(WHITE.copy(alpha = 0.02f), 0.55f),
                        GradientStop(WHITE.copy(alpha = 0f), 1f)
                    ),
                    borderGradient = listOf(
                        GradientStop(WHITE.copy(alpha = 0.42f), 0f),
                        GradientStop(WHITE.copy(alpha = 0.16f), 0.25f),
                        GradientStop(WHITE.copy(alpha = 0.08f), 0.75f),
                        GradientStop(WHITE.copy(alpha = 0.06f), 1f)
                    ),
                    glareRadiusRatio = 0.55f,
                    shadowElevationDp = 6
                ),
                primaryButton = ButtonSpec(
                    fillColor = BTN_BLUE,
                    fillAlpha = 0.85f,
                    sheenGradient = listOf(
                        GradientStop(WHITE.copy(alpha = 0.20f), 0f),
                        GradientStop(WHITE.copy(alpha = 0f), 1f)
                    ),
                    borderGradient = listOf(
                        GradientStop(WHITE.copy(alpha = 0.34f), 0f),
                        GradientStop(WHITE.copy(alpha = 0.10f), 1f)
                    ),
                    pressGlowEnabled = true
                )
            )
        }

        // ── MEDIUM: real blur, 2-orb backdrop (no warm), 3-stop border ──
        private fun medium(dark: Boolean): LiquidGlassSpecV2 {
            val backdrop = if (dark) BackdropSpec(
                gradientStops = listOf(
                    GradientStop(MESH_DARK_TOP, 0f),
                    GradientStop(MESH_DARK_MID, 0.55f),
                    GradientStop(MESH_DARK_BOTTOM, 1f)
                ),
                radialOrbs = listOf(
                    OrbSpec(MESH_DARK_ORB_COOL,   0.85f, 0.15f, 0.55f),
                    OrbSpec(MESH_DARK_ORB_INDIGO, 0.15f, 0.85f, 0.60f)
                ),
                ambientDriftEnabled = true
            ) else BackdropSpec(
                gradientStops = listOf(
                    GradientStop(MESH_LIGHT_TOP, 0f),
                    GradientStop(MESH_LIGHT_MID, 0.55f),
                    GradientStop(MESH_LIGHT_BOTTOM, 1f)
                ),
                radialOrbs = listOf(
                    OrbSpec(MESH_LIGHT_ORB_COOL,   0.85f, 0.15f, 0.55f),
                    OrbSpec(MESH_LIGHT_ORB_ACCENT, 0.15f, 0.85f, 0.60f)
                ),
                ambientDriftEnabled = true
            )
            return LiquidGlassSpecV2(
                tier = GlassTier.MEDIUM,
                isDark = dark,
                backdrop = backdrop,
                surface = GlassSurfaceSpec(
                    baseAlpha = 0.55f,
                    sheenGradient = listOf(
                        GradientStop(WHITE.copy(alpha = 0.18f), 0f),
                        GradientStop(WHITE.copy(alpha = 0.02f), 0.55f),
                        GradientStop(WHITE.copy(alpha = 0f), 1f)
                    ),
                    borderGradient = listOf(
                        GradientStop(WHITE.copy(alpha = 0.38f), 0f),
                        GradientStop(WHITE.copy(alpha = 0.14f), 0.4f),
                        GradientStop(WHITE.copy(alpha = 0.06f), 1f)
                    ),
                    glareRadiusRatio = 0.40f,
                    shadowElevationDp = 6
                ),
                primaryButton = ButtonSpec(
                    fillColor = BTN_BLUE,
                    fillAlpha = 0.85f,
                    sheenGradient = listOf(
                        GradientStop(WHITE.copy(alpha = 0.18f), 0f),
                        GradientStop(WHITE.copy(alpha = 0f), 1f)
                    ),
                    borderGradient = listOf(
                        GradientStop(WHITE.copy(alpha = 0.32f), 0f),
                        GradientStop(WHITE.copy(alpha = 0.10f), 0.4f),
                        GradientStop(WHITE.copy(alpha = 0.06f), 1f)
                    ),
                    pressGlowEnabled = false
                )
            )
        }

        // ── LOW: zero blur, zero shadow, zero radial (RAM 3GB safety) ──
        private fun low(dark: Boolean): LiquidGlassSpecV2 {
            val backdrop = if (dark) BackdropSpec(
                gradientStops = listOf(
                    GradientStop(MESH_DARK_TOP, 0f),
                    GradientStop(MESH_DARK_MID, 0.55f),
                    GradientStop(MESH_DARK_BOTTOM, 1f)
                ),
                radialOrbs = emptyList(),
                ambientDriftEnabled = false
            ) else BackdropSpec(
                gradientStops = listOf(
                    GradientStop(MESH_LIGHT_TOP, 0f),
                    GradientStop(MESH_LIGHT_MID, 0.55f),
                    GradientStop(MESH_LIGHT_BOTTOM, 1f)
                ),
                radialOrbs = emptyList(),
                ambientDriftEnabled = false
            )
            return LiquidGlassSpecV2(
                tier = GlassTier.LOW,
                isDark = dark,
                backdrop = backdrop,
                surface = GlassSurfaceSpec(
                    baseAlpha = 0.82f,
                    sheenGradient = listOf(
                        GradientStop(WHITE.copy(alpha = 0.14f), 0f),
                        GradientStop(WHITE.copy(alpha = 0.02f), 0.55f),
                        GradientStop(WHITE.copy(alpha = 0f), 1f)
                    ),
                    borderGradient = listOf(
                        GradientStop(WHITE.copy(alpha = 0.38f), 0f),
                        GradientStop(WHITE.copy(alpha = 0.08f), 1f)
                    ),
                    glareRadiusRatio = null,
                    shadowElevationDp = 0
                ),
                primaryButton = ButtonSpec(
                    fillColor = BTN_BLUE,
                    fillAlpha = 0.90f,
                    sheenGradient = listOf(
                        GradientStop(WHITE.copy(alpha = 0.14f), 0f),
                        GradientStop(WHITE.copy(alpha = 0f), 1f)
                    ),
                    borderGradient = listOf(
                        GradientStop(WHITE.copy(alpha = 0.28f), 0f),
                        GradientStop(WHITE.copy(alpha = 0.08f), 1f)
                    ),
                    pressGlowEnabled = false
                )
            )
        }

        // ── OFF: solid, no glass, no sheen, no border ─────────────
        private fun off(dark: Boolean): LiquidGlassSpecV2 {
            val solid = if (dark) MESH_DARK_BOTTOM else MESH_LIGHT_TOP
            return LiquidGlassSpecV2(
                tier = GlassTier.OFF,
                isDark = dark,
                backdrop = BackdropSpec(
                    gradientStops = listOf(GradientStop(solid, 0f)),
                    radialOrbs = emptyList(),
                    ambientDriftEnabled = false
                ),
                surface = GlassSurfaceSpec(
                    baseAlpha = 1.0f,
                    sheenGradient = emptyList(),
                    borderGradient = emptyList(),
                    glareRadiusRatio = null,
                    shadowElevationDp = 0
                ),
                primaryButton = ButtonSpec(
                    fillColor = BTN_BLUE,
                    fillAlpha = 1.0f,
                    sheenGradient = null,
                    borderGradient = null,
                    pressGlowEnabled = false
                )
            )
        }
    }
}
