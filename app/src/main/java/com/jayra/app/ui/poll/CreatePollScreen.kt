package com.jayra.app.ui.poll

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.CreatePollUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePollScreen(
    chatId: String,
    onBack: () -> Unit,
    viewModel: CreatePollViewModel = viewModel()
) {
    BackHandler { onBack() }

    val question by viewModel.question.collectAsStateWithLifecycle()
    val options by viewModel.options.collectAsStateWithLifecycle()
    val isAnonymous by viewModel.isAnonymous.collectAsStateWithLifecycle()
    val allowMultiple by viewModel.allowMultiple.collectAsStateWithLifecycle()
    val isQuiz by viewModel.isQuiz.collectAsStateWithLifecycle()
    val correctIndex by viewModel.correctOptionIndex.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) Color(0xFF242F3D) else Color.White
    val textPrimary = if (isDark) Color.White else Color.Black

    val canSend = viewModel.canSend

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Poling Baru",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.testTag("create_poll_title")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("poll_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (uiState is CreatePollUiState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 12.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick = {
                                viewModel.sendPoll(chatId = chatId, onSuccess = onBack)
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("send_poll_button")
                        ) {
                            Text(
                                text = "BUAT",
                                color = if (canSend) Color.White else Color.White.copy(alpha = 0.45f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarBg)
            )
        },
        containerColor = screenBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error banner if any
            if (uiState is CreatePollUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE53935).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (uiState as CreatePollUiState.Error).message,
                        color = Color(0xFFE53935),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Section 1: Pertanyaan Poling
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pertanyaan Poling",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JayraBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = question,
                        onValueChange = { viewModel.onQuestionChange(it) },
                        placeholder = { Text("Ajukan pertanyaan...", color = JayraSubtext) },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("poll_question_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JayraBlue,
                            unfocusedBorderColor = JayraSubtext.copy(alpha = 0.3f),
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Section 2: Pilihan Jawaban
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pilihan Jawaban",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = JayraBlue
                        )
                        Text(
                            text = "${options.size}/10",
                            fontSize = 12.sp,
                            color = JayraSubtext
                        )
                    }

                    if (isQuiz) {
                        Text(
                            text = "Ketuk ikon bulat di sebelah kiri untuk memilih jawaban yang benar.",
                            fontSize = 12.sp,
                            color = Color(0xFFFFA000),
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    options.forEachIndexed { index, optionText ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isQuiz) {
                                val isSelected = correctIndex == index
                                IconButton(
                                    onClick = { viewModel.selectCorrectOption(index) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("quiz_option_select_$index")
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Outlined.Circle,
                                        contentDescription = "Pilih jawaban benar",
                                        tint = if (isSelected) Color(0xFF4CAF50) else JayraSubtext
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = optionText,
                                onValueChange = { viewModel.onOptionChange(index, it) },
                                placeholder = { Text("Pilihan ${index + 1}", color = JayraSubtext) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("poll_option_input_$index"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = JayraBlue,
                                    unfocusedBorderColor = JayraSubtext.copy(alpha = 0.3f),
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            if (options.size > 2) {
                                IconButton(
                                    onClick = { viewModel.removeOption(index) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("remove_option_button_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Hapus pilihan",
                                        tint = JayraSubtext
                                    )
                                }
                            }
                        }
                    }

                    if (options.size < 10) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.addOption() },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("add_option_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = JayraBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tambah Pilihan",
                                color = JayraBlue,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Section 3: Pengaturan Poling
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pengaturan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JayraBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Switch Voting Anonim
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voting Anonim",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                            Text(
                                text = "Tidak ada yang dapat melihat siapa yang memilih apa",
                                fontSize = 12.sp,
                                color = JayraSubtext
                            )
                        }
                        Switch(
                            checked = isAnonymous,
                            onCheckedChange = { viewModel.toggleAnonymous(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = JayraBlue
                            ),
                            modifier = Modifier.testTag("anonymous_voting_switch")
                        )
                    }

                    HorizontalDivider(
                        color = JayraSubtext.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Switch Jawaban Ganda
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Jawaban Ganda",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                            Text(
                                text = "Pengguna dapat memilih lebih dari satu opsi",
                                fontSize = 12.sp,
                                color = JayraSubtext
                            )
                        }
                        Switch(
                            checked = allowMultiple,
                            onCheckedChange = { viewModel.toggleMultipleAnswers(it) },
                            enabled = !isQuiz,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = JayraBlue
                            ),
                            modifier = Modifier.testTag("multiple_answers_switch")
                        )
                    }

                    HorizontalDivider(
                        color = JayraSubtext.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Switch Mode Kuis
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mode Kuis",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                            Text(
                                text = "Poling hanya memiliki satu jawaban yang benar",
                                fontSize = 12.sp,
                                color = JayraSubtext
                            )
                        }
                        Switch(
                            checked = isQuiz,
                            onCheckedChange = { viewModel.toggleQuizMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = JayraBlue
                            ),
                            modifier = Modifier.testTag("quiz_mode_switch")
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Create Poll Light Mode", showBackground = true)
@Composable
private fun CreatePollScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        CreatePollScreen(chatId = "preview_chat", onBack = {})
    }
}

@Preview(name = "Create Poll Dark Mode", showBackground = true)
@Composable
private fun CreatePollScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        CreatePollScreen(chatId = "preview_chat", onBack = {})
    }
}
