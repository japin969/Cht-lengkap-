package com.jayra.app.ui.communities

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.data.ChatRepository
import com.jayra.app.data.CommunityRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.Chat
import com.jayra.app.model.CommunitiesStrings
import com.jayra.app.model.Community
import com.jayra.app.model.CommunityUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSeparatorDark
import com.jayra.app.ui.theme.JayraSeparatorLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CommunitiesViewModel(
    private val communityRepository: CommunityRepository = AppModule.communityRepository,
    private val chatRepository: ChatRepository = AppModule.chatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommunityUiState>(CommunityUiState.Loading)
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private val _strings = MutableStateFlow(CommunitiesStrings())
    val strings: StateFlow<CommunitiesStrings> = _strings.asStateFlow()

    private val _availableGroups = MutableStateFlow<List<Chat>>(emptyList())
    val availableGroups: StateFlow<List<Chat>> = _availableGroups.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var communitiesJob: Job? = null

    init {
        observeGroups()
        loadCommunities()
    }

    private fun observeGroups() {
        viewModelScope.launch {
            chatRepository.getChats()
                .catch { emit(emptyList()) }
                .collect { chats ->
                    _availableGroups.value = chats.filter { it.isGroup }
                }
        }
    }

    fun loadCommunities() {
        communitiesJob?.cancel()
        _uiState.value = CommunityUiState.Loading
        communitiesJob = viewModelScope.launch {
            communityRepository.seedInitialCommunitiesIfEmpty()
            communityRepository.observeCommunities()
                .map<List<Community>, CommunityUiState> { list ->
                    if (list.isEmpty()) {
                        CommunityUiState.Empty
                    } else {
                        CommunityUiState.Success(list)
                    }
                }
                .catch { error ->
                    emit(
                        CommunityUiState.Error(
                            error.message ?: "Gagal memuat daftar komunitas."
                        )
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onCreateCommunity(
        name: String,
        description: String,
        groupIds: List<String>,
        onCompleted: () -> Unit = {}
    ) {
        if (name.isBlank() || _isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            val result = communityRepository.createCommunity(
                name = name,
                description = description,
                groupIds = groupIds
            )
            _isSaving.value = false
            if (result.isSuccess) {
                onCompleted()
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Gagal menyimpan komunitas."
                _uiState.value = CommunityUiState.Error(msg)
            }
        }
    }
}

@Composable
fun CommunitiesScreen(
    onBackClick: () -> Unit,
    onGroupClick: (String) -> Unit = {},
    viewModel: CommunitiesViewModel = viewModel()
) {
    BackHandler { onBackClick() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val availableGroups by viewModel.availableGroups.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    CommunitiesContent(
        uiState = uiState,
        strings = strings,
        availableGroups = availableGroups,
        isSaving = isSaving,
        onBackClick = onBackClick,
        onGroupClick = onGroupClick,
        onCreateCommunity = viewModel::onCreateCommunity,
        onRetry = viewModel::loadCommunities
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesContent(
    uiState: CommunityUiState,
    strings: CommunitiesStrings = CommunitiesStrings(),
    availableGroups: List<Chat> = emptyList(),
    isSaving: Boolean = false,
    onBackClick: () -> Unit,
    onGroupClick: (String) -> Unit = {},
    onCreateCommunity: (String, String, List<String>, () -> Unit) -> Unit = { _, _, _, done -> done() },
    onRetry: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight
    val dividerColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedCommunityForGroups by remember { mutableStateOf<Community?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(topBarBg)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("communities_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backContentDescription,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = strings.screenTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("communities_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.addContentDescription,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        containerColor = screenBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding)
        ) {
            when (uiState) {
                is CommunityUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = JayraBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                is CommunityUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(JayraBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = JayraBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.emptyTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.emptySubtitle,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = JayraSubtext,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JayraBlue,
                                contentColor = Color(0xFFFFFFFF)
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("create_first_community_button")
                        ) {
                            Text(
                                text = strings.createButtonLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                is CommunityUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = JayraDangerRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.message,
                            fontSize = 15.sp,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JayraBlue,
                                contentColor = Color(0xFFFFFFFF)
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("communities_retry_button")
                        ) {
                            Text(
                                text = strings.retryButtonLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                is CommunityUiState.Success -> {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(durationMillis = 220))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(cardBg)
                                .testTag("communities_list"),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(
                                items = uiState.communities,
                                key = { it.id }
                            ) { community ->
                                CommunityItemRow(
                                    community = community,
                                    groupCountSuffix = strings.groupCountSuffix,
                                    onClick = {
                                        when {
                                            community.groupIds.size == 1 -> {
                                                onGroupClick(community.groupIds.first())
                                            }
                                            community.groupIds.size > 1 -> {
                                                selectedCommunityForGroups = community
                                            }
                                            else -> {
                                                selectedCommunityForGroups = community
                                            }
                                        }
                                    }
                                )
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = dividerColor,
                                    modifier = Modifier.padding(start = 84.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCommunityDialog(
            strings = strings,
            availableGroups = availableGroups,
            isSaving = isSaving,
            onDismiss = { showCreateDialog = false },
            onSave = { name, desc, groups ->
                onCreateCommunity(name, desc, groups) {
                    showCreateDialog = false
                }
            }
        )
    }

    val activeCommunity = selectedCommunityForGroups
    if (activeCommunity != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { selectedCommunityForGroups = null },
            sheetState = sheetState,
            containerColor = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = activeCommunity.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (activeCommunity.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeCommunity.description,
                        fontSize = 14.sp,
                        color = JayraSubtext
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = strings.selectGroupSheetTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JayraBlue
                )
                Spacer(modifier = Modifier.height(8.dp))

                activeCommunity.groupIds.forEach { groupId ->
                    val matchedGroup = availableGroups.find { it.id == groupId }
                    val displayGroupName = matchedGroup?.name ?: groupId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedCommunityForGroups = null
                                onGroupClick(groupId)
                            }
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                            .testTag("community_group_row_$groupId"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(JayraBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = JayraBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = displayGroupName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = JayraSubtext,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun CommunityItemRow(
    community: Community,
    groupCountSuffix: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("community_item_${community.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar 56dp
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(JayraBlue),
            contentAlignment = Alignment.Center
        ) {
            if (community.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(community.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = community.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = community.name.take(1).uppercase().ifBlank { "K" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Nama 17sp & Subteks "X Grup" 15sp
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = community.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${community.groupIds.size} $groupCountSuffix",
                fontSize = 15.sp,
                color = JayraSubtext,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Chevron kanan
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = JayraSubtext,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun CreateCommunityDialog(
    strings: CommunitiesStrings,
    availableGroups: List<Chat>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedGroups = remember {
        mutableStateListOf<String>().apply {
            availableGroups.firstOrNull()?.id?.let { add(it) }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = strings.dialogTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.nameLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("community_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.descriptionLabel) },
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("community_description_input")
                )
                if (availableGroups.isNotEmpty()) {
                    Text(
                        text = strings.selectGroupsLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JayraBlue
                    )
                    availableGroups.forEach { group ->
                        val isChecked = group.id in selectedGroups
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isChecked) {
                                        selectedGroups.remove(group.id)
                                    } else {
                                        selectedGroups.add(group.id)
                                    }
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (group.id !in selectedGroups) selectedGroups.add(group.id)
                                    } else {
                                        selectedGroups.remove(group.id)
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = JayraBlue)
                            )
                            Text(
                                text = group.name,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, description, selectedGroups.toList()) },
                enabled = name.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JayraBlue,
                    contentColor = Color(0xFFFFFFFF)
                ),
                modifier = Modifier.testTag("save_community_button")
            ) {
                Text(text = strings.saveLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(text = strings.cancelLabel, color = JayraSubtext)
            }
        }
    )
}

@Preview(name = "Communities Screen Light", showBackground = true)
@Composable
private fun CommunitiesScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            CommunitiesContent(
                uiState = CommunityUiState.Success(
                    communities = listOf(
                        Community(
                            id = "c1",
                            name = "Komunitas Developer Jayra",
                            description = "Diskusi Kotlin & Compose",
                            groupIds = listOf("chat_2", "chat_5")
                        ),
                        Community(
                            id = "c2",
                            name = "Desainer Produk Nusantara",
                            description = "Sistem desain Material 3",
                            groupIds = listOf("chat_5")
                        )
                    )
                ),
                onBackClick = {}
            )
        }
    }
}

@Preview(name = "Communities Screen Dark", showBackground = true)
@Composable
private fun CommunitiesScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            CommunitiesContent(
                uiState = CommunityUiState.Empty,
                onBackClick = {}
            )
        }
    }
}
