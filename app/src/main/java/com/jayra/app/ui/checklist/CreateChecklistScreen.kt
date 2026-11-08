package com.jayra.app.ui.checklist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.ChatRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.ChecklistItem
import com.jayra.app.model.ChecklistStrings
import com.jayra.app.model.CreateChecklistUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraSuccessGreen
import com.jayra.app.ui.theme.JayraTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CreateChecklistViewModel(
    private val chatRepository: ChatRepository = AppModule.chatRepository
) : ViewModel() {

    private val _strings = MutableStateFlow(ChecklistStrings())
    val strings: StateFlow<ChecklistStrings> = _strings.asStateFlow()

    private val _uiState = MutableStateFlow<CreateChecklistUiState>(CreateChecklistUiState.Idle)
    val uiState: StateFlow<CreateChecklistUiState> = _uiState.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _items = MutableStateFlow(listOf("", ""))
    val items: StateFlow<List<String>> = _items.asStateFlow()

    private val _allowOthersToToggle = MutableStateFlow(true)
    val allowOthersToToggle: StateFlow<Boolean> = _allowOthersToToggle.asStateFlow()

    fun onTitleChange(newTitle: String) {
        _title.value = newTitle.take(MAX_TITLE_LENGTH)
    }

    fun onItemChange(index: Int, newText: String) {
        _items.update { current ->
            current.toMutableList().also { list ->
                if (index in list.indices) {
                    list[index] = newText.take(MAX_ITEM_LENGTH)
                }
            }
        }
    }

    fun addItem() {
        if (_items.value.size < MAX_ITEMS) {
            _items.update { it + "" }
        }
    }

    fun removeItem(index: Int) {
        if (_items.value.size > MIN_ITEMS) {
            _items.update { current ->
                current.toMutableList().also { list ->
                    if (index in list.indices) {
                        list.removeAt(index)
                    }
                }
            }
        }
    }

    fun toggleAllowOthers(enabled: Boolean) {
        _allowOthersToToggle.value = enabled
    }

    fun sendChecklist(chatId: String, onSuccess: () -> Unit) {
        val cleanedTitle = _title.value.trim()
        val cleanedItems = _items.value
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_ITEMS)
            .map { taskText ->
                ChecklistItem(
                    id = UUID.randomUUID().toString(),
                    text = taskText,
                    isDone = false
                )
            }

        if (cleanedTitle.isEmpty() || cleanedItems.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = CreateChecklistUiState.Loading
            try {
                chatRepository.sendChecklistMessage(
                    chatId = chatId,
                    title = cleanedTitle,
                    items = cleanedItems,
                    allowOthersToToggle = _allowOthersToToggle.value
                )
                _uiState.value = CreateChecklistUiState.Success
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = CreateChecklistUiState.Error(
                    e.localizedMessage ?: "Gagal membuat daftar tugas"
                )
            }
        }
    }

    companion object {
        const val MIN_ITEMS = 1
        const val MAX_ITEMS = 20
        const val MAX_TITLE_LENGTH = 200
        const val MAX_ITEM_LENGTH = 300
    }
}

@Composable
fun CreateChecklistScreen(
    chatId: String,
    onBack: () -> Unit,
    viewModel: CreateChecklistViewModel = viewModel()
) {
    BackHandler { onBack() }

    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val allowOthersToToggle by viewModel.allowOthersToToggle.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CreateChecklistContent(
        strings = strings,
        title = title,
        items = items,
        allowOthersToToggle = allowOthersToToggle,
        uiState = uiState,
        onBack = onBack,
        onTitleChange = viewModel::onTitleChange,
        onItemChange = viewModel::onItemChange,
        onAddItem = viewModel::addItem,
        onRemoveItem = viewModel::removeItem,
        onToggleAllowOthers = viewModel::toggleAllowOthers,
        onSendChecklist = {
            viewModel.sendChecklist(chatId = chatId, onSuccess = onBack)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChecklistContent(
    strings: ChecklistStrings,
    title: String,
    items: List<String>,
    allowOthersToToggle: Boolean,
    uiState: CreateChecklistUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onItemChange: (Int, String) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onToggleAllowOthers: (Boolean) -> Unit,
    onSendChecklist: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    val canSend = title.trim().isNotEmpty() && items.any { it.trim().isNotEmpty() }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Checklist,
                            contentDescription = null,
                            tint = Color(0xFFFFFFFF),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = strings.screenTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFFFFFFFF),
                            modifier = Modifier.testTag("create_checklist_title")
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("checklist_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.backContentDescription,
                            tint = Color(0xFFFFFFFF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    if (uiState is CreateChecklistUiState.Loading) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFFFFF),
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 12.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick = onSendChecklist,
                            enabled = canSend,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("send_checklist_button")
                        ) {
                            Text(
                                text = strings.createActionLabel,
                                color = if (canSend) {
                                    Color(0xFFFFFFFF)
                                } else {
                                    Color(0xFFFFFFFF).copy(alpha = 0.45f)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarBg)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState is CreateChecklistUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = JayraDangerRed.copy(alpha = 0.14f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.message,
                        color = JayraDangerRed,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Section 1: Checklist Title
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.titleSectionHeader,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JayraBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        placeholder = {
                            Text(
                                text = strings.titlePlaceholder,
                                fontSize = 14.sp,
                                color = JayraSubtext
                            )
                        },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checklist_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JayraBlue,
                            unfocusedBorderColor = JayraSubtext.copy(alpha = 0.30f),
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Section 2: Checklist Task Items
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
                            text = strings.itemsSectionHeader,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = JayraBlue
                        )
                        Text(
                            text = "${items.size}/${CreateChecklistViewModel.MAX_ITEMS}",
                            fontSize = 12.sp,
                            color = JayraSubtext
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    items.forEachIndexed { index, itemText ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = itemText,
                                onValueChange = { updated -> onItemChange(index, updated) },
                                placeholder = {
                                    Text(
                                        text = "${strings.itemPlaceholderPrefix} ${index + 1}",
                                        fontSize = 14.sp,
                                        color = JayraSubtext
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("checklist_item_input_$index"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = JayraBlue,
                                    unfocusedBorderColor = JayraSubtext.copy(alpha = 0.30f),
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            if (items.size > CreateChecklistViewModel.MIN_ITEMS) {
                                IconButton(
                                    onClick = { onRemoveItem(index) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("remove_checklist_item_button_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = strings.removeItemContentDescription,
                                        tint = JayraSubtext,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (items.size < CreateChecklistViewModel.MAX_ITEMS) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = onAddItem,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("add_checklist_item_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = JayraBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.addItemButtonLabel,
                                color = JayraBlue,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Section 3: Collaborative Toggle Setting
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.settingsSectionHeader,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JayraBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = strings.allowOthersTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.allowOthersSubtitle,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = JayraSubtext
                            )
                        }
                        Switch(
                            checked = allowOthersToToggle,
                            onCheckedChange = onToggleAllowOthers,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFFFFF),
                                checkedTrackColor = JayraSuccessGreen
                            ),
                            modifier = Modifier.testTag("allow_others_toggle_switch")
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Create Checklist Light Mode", showBackground = true)
@Composable
private fun CreateChecklistScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            CreateChecklistContent(
                strings = ChecklistStrings(),
                title = "Persiapan Rilis Fase 25",
                items = listOf(
                    "Desain antarmuka CreateChecklistScreen",
                    "Integrasi sinkronisasi Firestore",
                    ""
                ),
                allowOthersToToggle = true,
                uiState = CreateChecklistUiState.Idle,
                onBack = {},
                onTitleChange = {},
                onItemChange = { _, _ -> },
                onAddItem = {},
                onRemoveItem = {},
                onToggleAllowOthers = {},
                onSendChecklist = {}
            )
        }
    }
}

@Preview(name = "Create Checklist Dark Mode", showBackground = true)
@Composable
private fun CreateChecklistScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            CreateChecklistContent(
                strings = ChecklistStrings(),
                title = "Target Mingguan Tim",
                items = listOf(
                    "Review kode modul ChecklistBubble",
                    "Verifikasi aturan keamanan Firestore"
                ),
                allowOthersToToggle = true,
                uiState = CreateChecklistUiState.Idle,
                onBack = {},
                onTitleChange = {},
                onItemChange = { _, _ -> },
                onAddItem = {},
                onRemoveItem = {},
                onToggleAllowOthers = {},
                onSendChecklist = {}
            )
        }
    }
}
