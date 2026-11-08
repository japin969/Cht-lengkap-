package com.jayra.app.ui.chatroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatInputBar(
    editorValue: TextFieldValue,
    onEditorValueChange: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9F9))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        IconButton(onClick = onAttachmentClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Add, "Attachment", tint = Color(0xFF0A84FF))
        }
        OutlinedTextField(
            value = editorValue,
            onValueChange = onEditorValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Pesan", color = Color.Gray, fontSize = 17.sp) },
            shape = RoundedCornerShape(20.dp),
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = Color(0xFF0A84FF)
            ),
            textStyle = TextStyle(fontSize = 17.sp, color = Color.Black)
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onSendClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Send, "Send", tint = Color(0xFF0A84FF))
        }
    }
}
