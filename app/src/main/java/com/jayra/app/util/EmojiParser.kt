package com.jayra.app.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.jayra.app.ui.theme.AppleEmojiFont

object EmojiParser {

    // Range emoji umum (subset font 1390 glyph)
    private fun isEmojiCp(cp: Int): Boolean {
        return cp in 0x1F300..0x1FAFF ||
               cp in 0x2600..0x27BF ||
               cp in 0x1F1E6..0x1F1FF ||
               cp in 0x1F000..0x1F0FF ||
               cp in 0x2190..0x21FF ||
               cp in 0x2B00..0x2BFF ||
               cp == 0x2764 || cp == 0x200D || cp == 0xFE0F ||
               cp == 0x20E3 || cp == 0x2122 || cp == 0x2139 ||
               cp in 0x2934..0x2935 || cp in 0x3030..0x303D ||
               cp in 0x3297..0x3299
    }

    // Cek apakah text punya emoji (skip parse kalau gak ada)
    fun hasEmoji(text: CharSequence): Boolean {
        var i = 0
        while (i < text.length) {
            val cp = Character.codePointAt(text, i)
            if (isEmojiCp(cp)) return true
            i += Character.charCount(cp)
        }
        return false
    }

    /**
     * Parse text → AnnotatedString dengan emoji di-wrap SpanStyle(fontFamily=AppleEmojiFont).
     * Teks biasa tetap pakai font family dari TextStyle caller (SF Pro).
     * Tidak pakai library eksternal — 0 overhead kalau text kosong / gak ada emoji.
     */
    fun parse(text: String): AnnotatedString {
        if (text.isEmpty() || !hasEmoji(text)) return AnnotatedString(text)

        return buildAnnotatedString {
            append(text)
            var i = 0
            var runStart = -1
            while (i < text.length) {
                val cp = Character.codePointAt(text, i)
                val cc = Character.charCount(cp)
                val isEmoji = isEmojiCp(cp)
                if (isEmoji && runStart == -1) {
                    runStart = i
                } else if (!isEmoji && runStart != -1) {
                    addStyle(SpanStyle(fontFamily = AppleEmojiFont), runStart, i)
                    runStart = -1
                }
                i += cc
            }
            if (runStart != -1) {
                addStyle(SpanStyle(fontFamily = AppleEmojiFont), runStart, text.length)
            }
        }
    }
}
