package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.example.model.RichTextFormatOption
import com.example.model.RichTextFormatType
import com.example.ui.theme.TelegramBlue

/**
 * Fase 21.1: Parser Markdown pesan Jayra.
 * Mendukung Bold (**teks**), Italic (__teks__), Strikethrough (~~teks~~),
 * Monospace (`teks`), dan Tautan ([label](https://...) maupun https://...).
 */
object MarkdownParser {

    const val URL_ANNOTATION_TAG: String = "URL"

    private val markdownLinkRegex = Regex("""\[([^\[\]\n]+)]\((https?://[^\s)]+)\)""")
    private val bareUrlRegex = Regex("""https?://[^\s<>"]+""")
    private val trailingUrlPunctuation = charArrayOf('.', ',', ';', ':', '!', '?', ')')

    fun parse(text: String): AnnotatedString {
        return parse(
            text = text,
            linkColor = TelegramBlue,
            codeBackground = Color(0x248E8E93)
        )
    }

    fun parse(
        text: String,
        linkColor: Color,
        codeBackground: Color = Color(0x248E8E93)
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        return buildAnnotatedString {
            appendFormattedSegment(
                source = text,
                linkColor = linkColor,
                codeBackground = codeBackground,
                depth = 0
            )
        }
    }

    fun hasMarkdownSyntax(text: String): Boolean {
        if (text.isBlank()) return false
        return text.contains("**") ||
            text.contains("__") ||
            text.contains("~~") ||
            text.contains('`') ||
            text.contains("http://") ||
            text.contains("https://")
    }

    /**
     * Menyisipkan atau membungkus seleksi teks saat ini dengan penanda format Markdown
     * serta mengatur posisi kursor / rentang seleksi secara presisi.
     */
    fun applyFormatting(
        currentValue: TextFieldValue,
        option: RichTextFormatOption
    ): TextFieldValue {
        val rawText = currentValue.text
        val start = minOf(currentValue.selection.start, currentValue.selection.end)
            .coerceIn(0, rawText.length)
        val end = maxOf(currentValue.selection.start, currentValue.selection.end)
            .coerceIn(0, rawText.length)

        val before = rawText.substring(0, start)
        val selected = rawText.substring(start, end)
        val after = rawText.substring(end)

        return if (option.type == RichTextFormatType.LINK) {
            val defaultLabel = option.placeholder.ifBlank { "teks" }
            if (selected.isNotEmpty()) {
                if (selected.startsWith("http://") || selected.startsWith("https://")) {
                    val inserted = "[$defaultLabel]($selected)"
                    val newText = before + inserted + after
                    TextFieldValue(
                        text = newText,
                        selection = TextRange(start + 1, start + 1 + defaultLabel.length)
                    )
                } else {
                    val inserted = "[$selected](https://)"
                    val newText = before + inserted + after
                    val urlCursor = start + 1 + selected.length + 2 + "https://".length
                    TextFieldValue(
                        text = newText,
                        selection = TextRange(urlCursor)
                    )
                }
            } else {
                val inserted = "[$defaultLabel](https://)"
                val newText = before + inserted + after
                TextFieldValue(
                    text = newText,
                    selection = TextRange(start + 1, start + 1 + defaultLabel.length)
                )
            }
        } else {
            val prefix = option.prefix
            val suffix = option.suffix
            if (selected.isNotEmpty()) {
                val newText = before + prefix + selected + suffix + after
                TextFieldValue(
                    text = newText,
                    selection = TextRange(
                        start + prefix.length,
                        start + prefix.length + selected.length
                    )
                )
            } else {
                val newText = before + prefix + suffix + after
                val cursor = start + prefix.length
                TextFieldValue(
                    text = newText,
                    selection = TextRange(cursor)
                )
            }
        }
    }

    private fun AnnotatedString.Builder.appendFormattedSegment(
        source: String,
        linkColor: Color,
        codeBackground: Color,
        depth: Int
    ) {
        if (source.isEmpty()) return
        if (depth > 8) {
            append(source)
            return
        }

        var index = 0
        val length = source.length

        while (index < length) {
            // 1. Inline Monospace: `teks`
            if (source[index] == '`') {
                val closingBacktick = source.indexOf('`', startIndex = index + 1)
                if (closingBacktick > index + 1) {
                    val codeContent = source.substring(index + 1, closingBacktick)
                    if (!codeContent.contains('\n')) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBackground
                            )
                        ) {
                            append(codeContent)
                        }
                        index = closingBacktick + 1
                        continue
                    }
                }
            }

            // 2. Markdown Link: [label](https://...)
            if (source[index] == '[') {
                val mdMatch = markdownLinkRegex.find(source, startIndex = index)
                if (mdMatch != null && mdMatch.range.first == index) {
                    val label = mdMatch.groupValues[1]
                    val url = mdMatch.groupValues[2]
                    appendLinkToken(displayText = label, url = url, linkColor = linkColor)
                    index = mdMatch.range.last + 1
                    continue
                }
            }

            // 3. Bare URL: https://... atau http://...
            if (source.startsWith("https://", startIndex = index) ||
                source.startsWith("http://", startIndex = index)
            ) {
                val urlMatch = bareUrlRegex.find(source, startIndex = index)
                if (urlMatch != null && urlMatch.range.first == index) {
                    val rawUrl = urlMatch.value
                    val cleanUrl = rawUrl.trimEnd(*trailingUrlPunctuation)
                    if (cleanUrl.length > "https://".length || cleanUrl.length > "http://".length) {
                        appendLinkToken(
                            displayText = cleanUrl,
                            url = cleanUrl,
                            linkColor = linkColor
                        )
                        val trailingPart = rawUrl.substring(cleanUrl.length)
                        if (trailingPart.isNotEmpty()) {
                            append(trailingPart)
                        }
                        index = urlMatch.range.last + 1
                        continue
                    }
                }
            }

            // 4. Bold: **teks**
            if (source.startsWith("**", startIndex = index)) {
                val closingBold = findClosingPair(source, "**", startIndex = index + 2)
                if (closingBold != -1) {
                    val inner = source.substring(index + 2, closingBold)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendFormattedSegment(
                            source = inner,
                            linkColor = linkColor,
                            codeBackground = codeBackground,
                            depth = depth + 1
                        )
                    }
                    index = closingBold + 2
                    continue
                }
            }

            // 5. Italic: __teks__
            if (source.startsWith("__", startIndex = index)) {
                val closingItalic = findClosingPair(source, "__", startIndex = index + 2)
                if (closingItalic != -1) {
                    val inner = source.substring(index + 2, closingItalic)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendFormattedSegment(
                            source = inner,
                            linkColor = linkColor,
                            codeBackground = codeBackground,
                            depth = depth + 1
                        )
                    }
                    index = closingItalic + 2
                    continue
                }
            }

            // 6. Strikethrough: ~~teks~~
            if (source.startsWith("~~", startIndex = index)) {
                val closingStrike = findClosingPair(source, "~~", startIndex = index + 2)
                if (closingStrike != -1) {
                    val inner = source.substring(index + 2, closingStrike)
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        appendFormattedSegment(
                            source = inner,
                            linkColor = linkColor,
                            codeBackground = codeBackground,
                            depth = depth + 1
                        )
                    }
                    index = closingStrike + 2
                    continue
                }
            }

            // Plain character fallback
            append(source[index])
            index++
        }
    }

    private fun AnnotatedString.Builder.appendLinkToken(
        displayText: String,
        url: String,
        linkColor: Color
    ) {
        val linkSpanStyle = SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.Underline
        )
        pushStringAnnotation(tag = URL_ANNOTATION_TAG, annotation = url)
        withLink(
            LinkAnnotation.Url(
                url = url,
                styles = TextLinkStyles(style = linkSpanStyle)
            )
        ) {
            withStyle(linkSpanStyle) {
                append(displayText)
            }
        }
        pop()
    }

    private fun findClosingPair(source: String, marker: String, startIndex: Int): Int {
        if (startIndex >= source.length) return -1
        val found = source.indexOf(marker, startIndex = startIndex)
        if (found <= startIndex) return -1
        return found
    }
}
