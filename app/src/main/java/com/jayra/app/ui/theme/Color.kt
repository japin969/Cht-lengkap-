package com.jayra.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════
// iOS HIG — Apple System Colors (Light)
// ═══════════════════════════════════════════════════════════
val IOSBlue    = Color(0xFF007AFF)
val IOSGreen   = Color(0xFF34C759)
val IOSIndigo  = Color(0xFF5856D6)
val IOSOrange  = Color(0xFFFF9500)
val IOSPink    = Color(0xFFFF2D55)
val IOSPurple  = Color(0xFFAF52DE)
val IOSRed     = Color(0xFFFF3B30)
val IOSTeal    = Color(0xFF5AC8FA)
val IOSYellow  = Color(0xFFFFCC00)

val IOSGray    = Color(0xFF8E8E93)
val IOSGray2   = Color(0xFFAEAEB2)
val IOSGray3   = Color(0xFFC7C7CC)
val IOSGray4   = Color(0xFFD1D1D6)
val IOSGray5   = Color(0xFFE5E5EA)
val IOSGray6   = Color(0xFFF2F2F7)

val IOSLabel           = Color(0xFF000000)
val IOSSecondaryLabel  = Color(0x993C3C43)
val IOSTertiaryLabel   = Color(0x4D3C3C43)

val IOSSeparator       = Color(0x4A3C3C43)
val IOSSeparatorOpaque = Color(0xFFC6C6C8)

val IOSSystemBackground          = Color(0xFFFFFFFF)
val IOSSecondarySystemBackground = Color(0xFFF2F2F7)
val IOSSystemGroupedBackground   = Color(0xFFF2F2F7)
val IOSSecondarySystemGroupedBackground = Color(0xFFFFFFFF)

val IOSFill          = Color(0x33787880)
val IOSSecondaryFill = Color(0x29787880)
val IOSTertiaryFill  = Color(0x1F767680)

// iOS HIG — Dark mode
val IOSBlueDark   = Color(0xFF0A84FF)
val IOSGreenDark  = Color(0xFF30D158)
val IOSRedDark    = Color(0xFFFF453A)
val IOSGrayDark   = Color(0xFF8E8E93)
val IOSGray2Dark  = Color(0xFF636366)
val IOSGray3Dark  = Color(0xFF48484A)
val IOSGray4Dark  = Color(0xFF3A3A3C)
val IOSGray5Dark  = Color(0xFF2C2C2E)
val IOSGray6Dark  = Color(0xFF1C1C1E)

val IOSLabelDark          = Color(0xFFFFFFFF)
val IOSSecondaryLabelDark = Color(0x99EBEBF5)
val IOSSeparatorDark      = Color(0x99545458)
val IOSOpaqueSeparatorDark = Color(0xFF38383A)

val IOSSystemBackgroundDark          = Color(0xFF000000)
val IOSSecondarySystemBackgroundDark = Color(0xFF1C1C1E)
val IOSSystemGroupedBackgroundDark   = Color(0xFF000000)

// ═══════════════════════════════════════════════════════════
// iOS NAV BAR (khusus untuk screen baru/refactor iOS)
// — DIPISAH dari Jayra*TopBar agar screen lama tidak berubah
// ═══════════════════════════════════════════════════════════
val IOSNavBarLight          = Color(0xFFF2F2F7)  // grouped bg
val IOSNavBarLightOpaque    = Color(0xFFFFFFFF)
val IOSNavBarDark           = Color(0xFF1C1C1E)
val IOSNavBarTitleLight     = Color(0xFF000000)
val IOSNavBarTitleDark      = Color(0xFFFFFFFF)
val IOSNavBarTintLight      = IOSBlue
val IOSNavBarTintDark       = IOSBlueDark

// ═══════════════════════════════════════════════════════════
// BACKWARD COMPAT — nama Jayra lama TETAP & AMAN
// ═══════════════════════════════════════════════════════════
val JayraBlue          = IOSBlue
val JayraLightTopBar   = Color(0xFF517DA2)   // ← TIDAK diubah (Telegram-style lama)
val JayraDarkTopBar    = Color(0xFF212D3B)   // ← TIDAK diubah

val JayraLightChatBg   = Color(0xFFE6EBEE)
val JayraDarkChatBg    = Color(0xFF0E1621)

val JayraBubbleOutLight = Color(0xFFEFFDDE)
val JayraBubbleOutDark  = Color(0xFF2B5278)
val JayraBubbleInLight  = Color(0xFFFFFFFF)
val JayraBubbleInDark   = Color(0xFF182533)

val JayraReplyLight    = Color(0xFFD3E2ED)
val JayraReplyDark     = Color(0xFF17354B)
val JayraSubtext       = IOSGray
val JayraStatusOnline  = Color(0xFFA0C4E2)

val JayraInputLight    = Color(0xFFFFFFFF)
val JayraInputDark     = Color(0xFF17212B)
val JayraSearchLight   = Color(0xFFF0F0F0)
val JayraSearchDark    = Color(0xFF2C2C2E)
val JayraCardLight     = Color(0xFFFFFFFF)
val JayraCardDark      = Color(0xFF1C1C1E)
val JayraSeparatorLight = Color(0xFFE0E0E0)
val JayraSeparatorDark  = Color(0xFF38383A)

val JayraDangerRed     = IOSRed
val JayraSuccessGreen  = IOSGreen
val JayraOrange        = IOSOrange
val JayraYellow        = IOSYellow
