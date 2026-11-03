package com.jayra.app.ui.chatroom

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.model.Poll
import com.jayra.app.model.PollOption
import com.jayra.app.model.Vote
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun PollBubble(
    poll: Poll,
    onVote: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF242F3D) else Color.White
    val textPrimary = if (isDark) Color.White else Color.Black
    val progressTrack = if (isDark) Color(0xFF1E2833) else Color(0xFFE0E0E0)

    var userVotedIndices by remember(poll.id) {
        mutableStateOf(poll.votes.map { it.optionIndex }.toSet())
    }
    val hasVoted = userVotedIndices.isNotEmpty()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("poll_bubble_${poll.id}")
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Poll,
                    contentDescription = null,
                    tint = JayraBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = when {
                        poll.isQuiz -> "KUIS"
                        poll.isAnonymous -> "POLING ANONIM"
                        else -> "POLING"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = JayraBlue,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Question
            Text(
                text = poll.question,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                lineHeight = 20.sp,
                modifier = Modifier.testTag("poll_question_text")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Options List
            poll.options.forEachIndexed { index, option ->
                val isSelected = userVotedIndices.contains(index)
                val percentage = poll.percentageFor(index)
                val percentageInt = (percentage * 100).toInt()

                val isCorrectQuiz = poll.isQuiz && poll.correctOptionIndex == index
                val isWrongQuiz = poll.isQuiz && isSelected && poll.correctOptionIndex != null && poll.correctOptionIndex != index

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !hasVoted || poll.allowMultiple) {
                            if (poll.allowMultiple) {
                                userVotedIndices = if (isSelected) {
                                    userVotedIndices - index
                                } else {
                                    userVotedIndices + index
                                }
                                onVote(index)
                            } else if (!hasVoted) {
                                userVotedIndices = setOf(index)
                                onVote(index)
                            }
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!hasVoted) {
                            // Selection Control
                            if (poll.allowMultiple) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = JayraBlue,
                                        uncheckedColor = JayraSubtext
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = JayraBlue,
                                        unselectedColor = JayraSubtext
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            // Post-vote status indicator
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCorrectQuiz -> Color(0xFF4CAF50)
                                            isWrongQuiz -> Color(0xFFE53935)
                                            isSelected -> JayraBlue
                                            else -> progressTrack
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCorrectQuiz) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Benar",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else if (isWrongQuiz) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Salah",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Dipilih",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Option text
                        Text(
                            text = option.text,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = when {
                                hasVoted && isCorrectQuiz -> Color(0xFF4CAF50)
                                hasVoted && isWrongQuiz -> Color(0xFFE53935)
                                else -> textPrimary
                            },
                            modifier = Modifier.weight(1f)
                        )

                        if (hasVoted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$percentageInt%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) JayraBlue else JayraSubtext
                            )
                        }
                    }

                    if (hasVoted) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                isCorrectQuiz -> Color(0xFF4CAF50)
                                isWrongQuiz -> Color(0xFFE53935)
                                isSelected -> JayraBlue
                                else -> JayraBlue.copy(alpha = 0.5f)
                            },
                            trackColor = progressTrack
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${poll.totalVotes} suara",
                    fontSize = 12.sp,
                    color = JayraSubtext
                )
                Text(
                    text = if (poll.isAnonymous) "Suara bersifat rahasia" else "Suara terlihat",
                    fontSize = 12.sp,
                    color = JayraSubtext
                )
            }
        }
    }
}

@Preview(name = "Poll Bubble Light", showBackground = true)
@Composable
private fun PollBubbleLightPreview() {
    JayraTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .background(JayraLightChatBg)
                .padding(16.dp)
        ) {
            PollBubble(
                poll = Poll(
                    id = "p1",
                    question = "Kapan kita mulai rilis Fase 14?",
                    options = listOf(
                        PollOption(text = "Hari ini juga", voteCount = 5),
                        PollOption(text = "Besok pagi", voteCount = 2),
                        PollOption(text = "Minggu depan", voteCount = 1)
                    ),
                    isAnonymous = true,
                    allowMultiple = false
                )
            )
        }
    }
}

@Preview(name = "Poll Bubble Dark", showBackground = true)
@Composable
private fun PollBubbleDarkPreview() {
    JayraTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(JayraDarkChatBg)
                .padding(16.dp)
        ) {
            PollBubble(
                poll = Poll(
                    id = "p2",
                    question = "Bahasa pemrograman utama Android modern?",
                    options = listOf(
                        PollOption(text = "Kotlin", voteCount = 10),
                        PollOption(text = "Java", voteCount = 2),
                        PollOption(text = "Dart", voteCount = 1)
                    ),
                    isAnonymous = false,
                    isQuiz = true,
                    correctOptionIndex = 0,
                    votes = listOf(Vote(userId = "me", optionIndex = 0))
                )
            )
        }
    }
}
