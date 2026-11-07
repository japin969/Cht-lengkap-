package com.jayra.app.ui.chatroom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.model.RichTextEditorConfig
import com.jayra.app.model.RichTextFormatOption
import com.jayra.app.model.RichTextFormatType
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraInputDark
import com.jayra.app.ui.theme.JayraInputLight
import com.jayra.app.ui.theme.JayraSearchDark
import com.jayra.app.ui.theme.JayraSearchLight
import com.jayra.app.ui.theme.JayraSeparatorDark
import com.jayra.app.ui.theme.JayraSeparatorLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme
import com.jayra.app.util.MarkdownParser

/**
 * Fase 21.2: Bilah alat pemformatan teks horizontal yang dapat digulir
 * (Bold, Italic, Strikethrough, Code, Link).
 */
@Composable
fun RichTextFormattingToolbar(
    formatOptions: List<RichTextFormatOption>,
    onFormatClick: (RichTextFormatOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("rich_text_toolbar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        formatOptions.forEach { option ->
            val iconVector = iconForFormatType(option.type)
            val tag = "format_btn_${option.type.name.lowercase()}"

            IconButton(
                onClick = { onFormatClick(option) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag(tag)
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = option.label,
                    tint = JayraSubtext,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Fase 21.2: Rich Text Editor lengkap dengan bilah alat format Markdown,
 * pratinjau langsung terformat, dan TextField multi-baris berbasis TextFieldValue.
 */
@Composable
fun RichTextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onFormatSelected: (RichTextFormatOption) -> Unit = { option ->
        onValueChange(MarkdownParser.applyFormatting(value, option))
    },
    onSend: () -> Unit,
    onAttachClick: () -> Unit = {},
    config: RichTextEditorConfig = RichTextEditorConfig(),
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val containerBg = if (isDark) JayraInputDark else JayraInputLight
    val fieldBg = if (isDark) JayraSearchDark else JayraSearchLight
    val dividerColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    val showFormattedPreview = remember(value.text) {
        MarkdownParser.hasMarkdownSyntax(value.text)
    }
    val parsedPreview = remember(value.text) {
        if (showFormattedPreview) MarkdownParser.parse(value.text) else null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerBg)
            .testTag("rich_text_editor_root")
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = dividerColor
        )

        // Bilah alat pemformatan (Bold, Italic, Strikethrough, Code, Link)
        RichTextFormattingToolbar(
            formatOptions = config.formatOptions,
            onFormatClick = onFormatSelected
        )

        // Pratinjau format langsung saat penanda Markdown terdeteksi
        AnimatedVisibility(
            visible = showFormattedPreview && parsedPreview != null,
            enter = expandVertically(animationSpec = tween(durationMillis = 180)) +
                fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 150)) +
                fadeOut(animationSpec = tween(durationMillis = 150))
        ) {
            if (parsedPreview != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(fieldBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${config.previewHeaderLabel}:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JayraBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = parsedPreview,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Baris Input Multi-baris + Lampiran + Kirim
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = onAttachClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("attach_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = config.attachContentDescription,
                    tint = JayraSubtext,
                    modifier = Modifier.size(24.dp)
                )
            }

            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .testTag("chat_input_textfield"),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = textColor
                ),
                placeholder = {
                    Text(
                        text = config.placeholderText,
                        fontSize = 16.sp,
                        color = JayraSubtext
                    )
                },
                singleLine = false,
                minLines = 1,
                maxLines = 5,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    disabledContainerColor = fieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = JayraBlue,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            AnimatedVisibility(
                visible = value.text.isNotBlank(),
                enter = scaleIn(animationSpec = tween(durationMillis = 150)) +
                    fadeIn(animationSpec = tween(durationMillis = 150)),
                exit = scaleOut(animationSpec = tween(durationMillis = 150)) +
                    fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = config.sendContentDescription,
                        tint = JayraBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun iconForFormatType(type: RichTextFormatType): ImageVector {
    return when (type) {
        RichTextFormatType.BOLD -> Icons.Default.FormatBold
        RichTextFormatType.ITALIC -> Icons.Default.FormatItalic
        RichTextFormatType.STRIKETHROUGH -> Icons.Default.FormatStrikethrough
        RichTextFormatType.CODE -> Icons.Default.Code
        RichTextFormatType.LINK -> Icons.Default.Link
    }
}

@Preview(name = "Rich Text Toolbar Light", showBackground = true)
@Composable
private fun RichTextToolbarLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface(color = JayraInputLight) {
            RichTextFormattingToolbar(
                formatOptions = RichTextEditorConfig.DEFAULT_FORMAT_OPTIONS,
                onFormatClick = {}
            )
        }
    }
}

@Preview(name = "Rich Text Toolbar Dark", showBackground = true)
@Composable
private fun RichTextToolbarDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface(color = JayraInputDark) {
            RichTextFormattingToolbar(
                formatOptions = RichTextEditorConfig.DEFAULT_FORMAT_OPTIONS,
                onFormatClick = {}
            )
        }
    }
}

@Preview(name = "Rich Text Editor Light", showBackground = true)
@Composable
private fun RichTextEditorLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            RichTextEditor(
                value = TextFieldValue(
                    text = "Halo **Jayra** __cepat__ & `aman` di https://jayra.app",
                    selection = TextRange(54)
                ),
                onValueChange = {},
                onSend = {}
            )
        }
    }
}

@Preview(name = "Rich Text Editor Dark", showBackground = true)
@Composable
private fun RichTextEditorDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            RichTextEditor(
                value = TextFieldValue(
                    text = "",
                    selection = TextRange.Zero
                ),
                onValueChange = {},
                onSend = {}
            )
        }
    }
}
