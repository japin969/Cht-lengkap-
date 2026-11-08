package com.jayra.app.ui.contactinfo

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FilePresent
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.model.Contact
import com.jayra.app.model.ContactInfoUiState
import com.jayra.app.model.ContactNote
import com.jayra.app.model.ContactNoteStrings
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraOrange
import com.jayra.app.ui.theme.JayraSeparatorDark
import com.jayra.app.ui.theme.JayraSeparatorLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraSuccessGreen
import com.jayra.app.ui.theme.JayraTheme
import com.jayra.app.ui.theme.JayraYellow
import com.jayra.app.ui.viewmodel.ContactInfoViewModel

@Composable
fun ContactInfoScreen(
    contactId: String,
    onBack: () -> Unit,
    viewModel: ContactInfoViewModel = viewModel()
) {
    BackHandler { onBack() }

    LaunchedEffect(contactId) {
        viewModel.loadContact(contactId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contactNote by viewModel.contactNote.collectAsStateWithLifecycle()
    val noteStrings by viewModel.noteStrings.collectAsStateWithLifecycle()
    val isSavingNote by viewModel.isSavingNote.collectAsStateWithLifecycle()
    val showSavedFeedback by viewModel.showSavedFeedback.collectAsStateWithLifecycle()

    ContactInfoContent(
        uiState = uiState,
        contactNote = contactNote,
        noteStrings = noteStrings,
        isSavingNote = isSavingNote,
        showSavedFeedback = showSavedFeedback,
        onBack = onBack,
        onMuteToggle = viewModel::onMuteToggle,
        onSaveToCameraToggle = viewModel::onSaveToCameraToggle,
        onDisappearingToggle = viewModel::onDisappearingMessagesToggle,
        onSavePrivateNote = viewModel::onSavePrivateNote,
        onSaveBirthday = viewModel::onSaveBirthday,
        onRetry = viewModel::onRetry
    )
}

@Composable
fun ContactInfoContent(
    uiState: ContactInfoUiState,
    contactNote: ContactNote = ContactNote(),
    noteStrings: ContactNoteStrings = ContactNoteStrings(),
    isSavingNote: Boolean = false,
    showSavedFeedback: Boolean = false,
    onBack: () -> Unit,
    onMuteToggle: (Boolean) -> Unit,
    onSaveToCameraToggle: (Boolean) -> Unit,
    onDisappearingToggle: () -> Unit,
    onSavePrivateNote: (String) -> Unit = {},
    onSaveBirthday: (Long?) -> Unit = {},
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val headerBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight
    val separatorColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    Scaffold(
        topBar = {
            // Header with Back button, "Info Kontak" and "Edit"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(headerBg)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("contact_info_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Info Kontak",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                TextButton(onClick = { /* Edit contact */ }) {
                    Text(
                        text = "Edit",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding)
        ) {
            when (uiState) {
                is ContactInfoUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = JayraBlue)
                    }
                }
                is ContactInfoUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = uiState.message, color = JayraSubtext, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = onRetry) {
                            Text("Retry", color = JayraBlue)
                        }
                    }
                }
                is ContactInfoUiState.Success -> {
                    val contact = uiState.contact
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Top Profile Area
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(headerBg)
                                .padding(bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 120dp Avatar Circle
                            Box(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(JayraBlue)
                            ) {
                                if (contact.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(contact.avatarUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = contact.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = contact.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = contact.phone,
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Action Grid (3 buttons: Panggilan, Video, Cari)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ActionGridItem(
                                    icon = Icons.Rounded.Call,
                                    label = "panggilan",
                                    modifier = Modifier.weight(1f)
                                )
                                ActionGridItem(
                                    icon = Icons.Rounded.Videocam,
                                    label = "video",
                                    modifier = Modifier.weight(1f)
                                )
                                ActionGridItem(
                                    icon = Icons.Rounded.Search,
                                    label = "cari",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Card Media & Bintang
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                            ) {
                                SettingItemRow(
                                    icon = Icons.Rounded.FilePresent,
                                    iconTint = JayraBlue,
                                    title = "Media, Tautan, dan Dok",
                                    valueText = "${contact.mediaCount}"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .padding(start = 56.dp)
                                        .background(separatorColor)
                                )
                                SettingItemRow(
                                    icon = Icons.Rounded.Star,
                                    iconTint = JayraYellow,
                                    title = "Pesan Berbintang",
                                    valueText = "${contact.starredCount}"
                                )
                            }

                            // Fase 24: Card "Catatan Pribadi" (tersimpan ke Firestore users/{uid}/notes)
                            PersonalNoteCard(
                                contactNote = contactNote,
                                noteStrings = noteStrings,
                                isSaving = isSavingNote,
                                showSavedFeedback = showSavedFeedback,
                                cardBg = cardBg,
                                isDark = isDark,
                                onSaveNote = onSavePrivateNote
                            )

                            // Fase 24: Card "Ulang Tahun" (DatePicker tersimpan ke Firestore users/{uid}/notes)
                            BirthdayCard(
                                contactNote = contactNote,
                                noteStrings = noteStrings,
                                cardBg = cardBg,
                                isDark = isDark,
                                onSaveBirthday = onSaveBirthday
                            )

                            // Card Settings 1 (Bisukan, Wallpaper & Bunyi, Simpan ke Rol Kamera)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                            ) {
                                SettingSwitchRow(
                                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                                    iconTint = JayraSuccessGreen,
                                    title = "Bisukan",
                                    checked = contact.isMuted,
                                    onCheckedChange = onMuteToggle
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .padding(start = 56.dp)
                                        .background(separatorColor)
                                )
                                SettingItemRow(
                                    icon = Icons.Rounded.Palette,
                                    iconTint = JayraOrange,
                                    title = "Wallpaper & Bunyi",
                                    valueText = "Default"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .padding(start = 56.dp)
                                        .background(separatorColor)
                                )
                                SettingSwitchRow(
                                    icon = Icons.Rounded.Download,
                                    iconTint = JayraYellow,
                                    title = "Simpan ke Rol Kamera",
                                    checked = contact.saveToCamera,
                                    onCheckedChange = onSaveToCameraToggle
                                )
                            }

                            // Card Settings 2 (Pesan Sementara)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                            ) {
                                SettingSwitchRow(
                                    icon = Icons.Rounded.Timer,
                                    iconTint = JayraBlue,
                                    title = "Pesan Sementara",
                                    checked = contact.disappearingEnabled,
                                    onCheckedChange = { onDisappearingToggle() }
                                )
                            }

                            // Card 5.15 Privacy Dashboard & Security Indicators
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = "Security",
                                        tint = JayraSuccessGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Privacy & Encryption Shield",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• AES-256-GCM Hardware Keystore: ACTIVE\n• Screen Capture Blocker (FLAG_SECURE): ACTIVE\n• Clipboard Auto-Purge (30s): ACTIVE\n• Integrity Guard: VERIFIED",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = JayraSubtext
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionGridItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { /* action */ }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = JayraBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = JayraBlue
        )
    }
}

@Composable
private fun SettingItemRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    valueText: String
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Item Click */ }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = if (isDark) Color.White else Color.Black,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueText,
            fontSize = 15.sp,
            color = JayraSubtext
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = JayraSubtext,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = if (isDark) Color.White else Color.Black,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = JayraBlue
            )
        )
    }
}

@Composable
private fun PersonalNoteCard(
    contactNote: ContactNote,
    noteStrings: ContactNoteStrings,
    isSaving: Boolean,
    showSavedFeedback: Boolean,
    cardBg: Color,
    isDark: Boolean,
    onSaveNote: (String) -> Unit
) {
    var draftNote by remember(contactNote.contactId, contactNote.note) {
        mutableStateOf(contactNote.note)
    }
    val primaryText = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1C242B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .padding(16.dp)
            .testTag("contact_personal_note_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.EditNote,
                    contentDescription = noteStrings.personalNoteCardTitle,
                    tint = JayraBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = noteStrings.personalNoteCardTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showSavedFeedback,
                enter = fadeIn(animationSpec = tween(durationMillis = 180)),
                exit = fadeOut(animationSpec = tween(durationMillis = 180))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = noteStrings.savedBadgeLabel,
                        tint = JayraSuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = noteStrings.savedBadgeLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = JayraSuccessGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = noteStrings.personalNoteSubtitle,
            fontSize = 13.sp,
            color = JayraSubtext,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = draftNote,
            onValueChange = { draftNote = it.take(2000) },
            placeholder = {
                Text(
                    text = noteStrings.personalNotePlaceholder,
                    fontSize = 14.sp,
                    color = JayraSubtext
                )
            },
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contact_personal_note_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { onSaveNote(draftNote) },
                enabled = !isSaving,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("contact_save_note_button")
            ) {
                Text(
                    text = if (isSaving) noteStrings.savingLabel else noteStrings.saveNoteButtonLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayCard(
    contactNote: ContactNote,
    noteStrings: ContactNoteStrings,
    cardBg: Color,
    isDark: Boolean,
    onSaveBirthday: (Long?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val primaryText = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1C242B)
    val hasBirthday = contactNote.birthdayFormatted.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .padding(16.dp)
            .testTag("contact_birthday_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Cake,
                contentDescription = noteStrings.birthdayCardTitle,
                tint = JayraOrange,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = noteStrings.birthdayCardTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (hasBirthday) contactNote.birthdayFormatted else noteStrings.birthdayEmptyLabel,
            fontSize = 16.sp,
            fontWeight = if (hasBirthday) FontWeight.Medium else FontWeight.Normal,
            color = if (hasBirthday) primaryText else JayraSubtext,
            modifier = Modifier.testTag("contact_birthday_value_text")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("contact_select_birthday_button")
            ) {
                Text(
                    text = if (hasBirthday) {
                        noteStrings.changeBirthdayButtonLabel
                    } else {
                        noteStrings.selectBirthdayButtonLabel
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JayraBlue
                )
            }

            if (hasBirthday) {
                TextButton(
                    onClick = { onSaveBirthday(null) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("contact_clear_birthday_button")
                ) {
                    Text(
                        text = noteStrings.clearBirthdayButtonLabel,
                        fontSize = 14.sp,
                        color = JayraSubtext
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = contactNote.birthdayMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveBirthday(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("contact_birthday_confirm_button")
                ) {
                    Text(
                        text = noteStrings.confirmButtonLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JayraBlue
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(
                        text = noteStrings.cancelButtonLabel,
                        fontSize = 14.sp,
                        color = JayraSubtext
                    )
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = noteStrings.datePickerTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 12.dp)
                    )
                }
            )
        }
    }
}

@Preview(name = "Jayra Contact Info Light", showBackground = true)
@Composable
private fun ContactInfoLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            ContactInfoContent(
                uiState = ContactInfoUiState.Success(
                    Contact(
                        id = "pavel_durov",
                        name = "Pavel Durov",
                        phone = "+1 (555) 739-0192",
                        mediaCount = 184,
                        starredCount = 19
                    )
                ),
                contactNote = ContactNote(
                    contactId = "pavel_durov",
                    ownerId = "preview_uid",
                    note = "Arsitek utama protokol enkripsi & kolaborator proyek Jayra.",
                    birthdayFormatted = "10 Oktober 1984",
                    birthdayMillis = 466214400000L
                ),
                showSavedFeedback = true,
                onBack = {},
                onMuteToggle = {},
                onSaveToCameraToggle = {},
                onDisappearingToggle = {},
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Jayra Contact Info Dark", showBackground = true)
@Composable
private fun ContactInfoDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            ContactInfoContent(
                uiState = ContactInfoUiState.Success(
                    Contact(
                        id = "elena_p",
                        name = "Elena Petrova",
                        phone = "+44 7911 123456",
                        mediaCount = 64,
                        starredCount = 5
                    )
                ),
                contactNote = ContactNote(
                    contactId = "elena_p",
                    ownerId = "preview_uid",
                    note = "Peneliti keamanan siber — diskusi audit kunci publik.",
                    birthdayFormatted = ""
                ),
                onBack = {},
                onMuteToggle = {},
                onSaveToCameraToggle = {},
                onDisappearingToggle = {},
                onRetry = {}
            )
        }
    }
}
