package com.jayra.app.ui.contacts

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.model.ContactsUiState
import com.jayra.app.model.User
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraOrange
import com.jayra.app.ui.theme.JayraSeparatorDark
import com.jayra.app.ui.theme.JayraSeparatorLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraSuccessGreen
import com.jayra.app.ui.viewmodel.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onBackClick: () -> Unit,
    onContactClick: (String) -> Unit = {},
    viewModel: ContactsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }

    ContactsScreenContent(
        uiState = uiState,
        isSearching = isSearching,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onToggleSearch = viewModel::toggleSearch,
        onBackClick = onBackClick,
        onContactClick = onContactClick,
        onVoiceCall = { user -> viewModel.startVoiceCall(context, user) },
        onVideoCall = { user -> viewModel.startVideoCall(context, user) },
        onDeleteContact = { id -> viewModel.deleteContact(id, context) },
        onToggleMute = viewModel::toggleMuteContact,
        onBlockContact = { id -> viewModel.blockContact(id, context) },
        onFabClick = { showAddDialog = true }
    )

    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, phone, username ->
                showAddDialog = false
                viewModel.addContact(name, phone, username, context)
            }
        )
    }
}

@Composable
fun ContactsScreenContent(
    uiState: ContactsUiState,
    isSearching: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onContactClick: (String) -> Unit,
    onVoiceCall: (User) -> Unit,
    onVideoCall: (User) -> Unit,
    onDeleteContact: (String) -> Unit,
    onToggleMute: (String) -> Unit,
    onBlockContact: (String) -> Unit,
    onFabClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraCardDark else JayraCardLight
    var showMoreMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contacts_screen"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarBg)
            ) {
                // TopAppBar 56dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("contacts_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }

                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text(
                                    "Cari kontak...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 17.sp
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("contacts_search_input")
                        )
                        IconButton(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    onSearchQueryChange("")
                                } else {
                                    onToggleSearch(false)
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Tutup Pencarian",
                                tint = Color.White
                            )
                        }
                    } else {
                        Text(
                            text = "Kontak",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        )

                        IconButton(
                            onClick = { onToggleSearch(true) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("contacts_search_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onFabClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("contacts_add_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Tambah Kontak",
                                tint = Color.White
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opsi",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Urutkan Berdasarkan Nama") },
                                    onClick = { showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Undang Teman") },
                                    onClick = { showMoreMenu = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                containerColor = JayraBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("fab_add_contact")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Tambah Kontak",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(screenBg)
        ) {
            when (uiState) {
                is ContactsUiState.Loading -> {
                    ContactsShimmerList()
                }
                is ContactsUiState.Empty -> {
                    ContactsEmptyState(onAddClick = onFabClick)
                }
                is ContactsUiState.Error -> {
                    ContactsErrorState(message = uiState.message)
                }
                is ContactsUiState.Success -> {
                    if (uiState.contacts.isEmpty()) {
                        ContactsEmptyState(onAddClick = onFabClick)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("contacts_list")
                        ) {
                            // Online section if any
                            if (uiState.onlineUsers.isNotEmpty() && !isSearching) {
                                item {
                                    SectionHeader(title = "Online (${uiState.onlineUsers.size})", isPrimary = true)
                                }
                                items(
                                    items = uiState.onlineUsers,
                                    key = { "online_${it.id}" }
                                ) { user ->
                                    ContactItemRow(
                                        user = user,
                                        onClick = { onContactClick(user.id) },
                                        onVoiceCall = { onVoiceCall(user) },
                                        onVideoCall = { onVideoCall(user) },
                                        onDeleteContact = { onDeleteContact(user.id) },
                                        onToggleMute = { onToggleMute(user.id) },
                                        onBlockUser = { onBlockContact(user.id) }
                                    )
                                }
                            }

                            // All Contacts section
                            item {
                                SectionHeader(
                                    title = if (isSearching) "Hasil Pencarian (${uiState.contacts.size})" else "Semua Kontak (${uiState.contacts.size})",
                                    isPrimary = false
                                )
                            }

                            items(
                                items = uiState.contacts,
                                key = { it.id }
                            ) { user ->
                                ContactItemRow(
                                    user = user,
                                    onClick = { onContactClick(user.id) },
                                    onVoiceCall = { onVoiceCall(user) },
                                    onVideoCall = { onVideoCall(user) },
                                    onDeleteContact = { onDeleteContact(user.id) },
                                    onToggleMute = { onToggleMute(user.id) },
                                    onBlockUser = { onBlockContact(user.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, isPrimary: Boolean) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isPrimary) JayraBlue else JayraSubtext
    val bg = if (isDark) JayraCardDark else JayraCardLight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactItemRow(
    user: User,
    onClick: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onDeleteContact: () -> Unit,
    onToggleMute: () -> Unit,
    onBlockUser: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val itemBg = if (isDark) JayraCardDark else JayraCardLight
    val textColor = if (isDark) Color.White else Color.Black
    val dividerColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    var showMenu by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDeleteContact()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleMute()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bg = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> JayraDangerRed
                SwipeToDismissBoxValue.StartToEnd -> JayraOrange
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
                }
            ) {
                if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = Color.White
                        )
                    }
                } else if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Bisukan",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (user.isMuted) "Bunyikan" else "Bisukan",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(itemBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable { onClick() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar 48dp with 2dp green border if online
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .then(
                            if (user.isOnline) {
                                Modifier.border(2.dp, JayraSuccessGreen, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                        .padding(if (user.isOnline) 2.dp else 0.dp)
                        .clip(CircleShape)
                        .background(JayraBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user.avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = user.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = user.name.take(1).uppercase(),
                            color = JayraBlue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Detail column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val statusText = if (user.isOnline) "online" else user.lastSeenFormatted
                    val statusColor = if (user.isOnline) JayraBlue else JayraSubtext

                    Text(
                        text = statusText,
                        fontSize = 14.sp,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (user.isMuted) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = "Dibisukan",
                        tint = JayraSubtext,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp)
                    )
                }

                // Context menu button (48dp target)
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("contact_item_options_${user.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi Kontak",
                            tint = JayraSubtext
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Kirim Pesan") },
                            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, tint = JayraBlue) },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Panggil Suara") },
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = JayraBlue) },
                            onClick = {
                                showMenu = false
                                onVoiceCall()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Panggil Video") },
                            leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, tint = JayraBlue) },
                            onClick = {
                                showMenu = false
                                onVideoCall()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (user.isMuted) "Bunyikan Notifikasi" else "Bisukan Notifikasi") },
                            leadingIcon = { Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = JayraOrange) },
                            onClick = {
                                showMenu = false
                                onToggleMute()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Blokir Pengguna") },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = JayraSubtext) },
                            onClick = {
                                showMenu = false
                                onBlockUser()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Hapus Kontak", color = JayraDangerRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = JayraDangerRed) },
                            onClick = {
                                showMenu = false
                                onDeleteContact()
                            }
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp),
                thickness = 0.5.dp,
                color = dividerColor
            )
        }
    }
}

@Composable
fun ContactsShimmerList() {
    val infiniteTransition = rememberInfiniteTransition(label = "contacts_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "contacts_shimmer_alpha"
    )

    val isDark = isSystemInDarkTheme()
    val shimmerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)

    Column(modifier = Modifier.fillMaxSize()) {
        repeat(8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(shimmerColor.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
fun ContactsEmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(JayraBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = JayraBlue,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Belum Ada Kontak",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSystemInDarkTheme()) Color.White else Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tambahkan kontak teman Anda untuk mengobrol dan melakukan panggilan aman.",
            fontSize = 14.sp,
            color = JayraSubtext,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(
            onClick = onAddClick,
            modifier = Modifier.testTag("btn_add_first_contact")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = JayraBlue)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tambah Kontak Sekarang", color = JayraBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ContactsErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Gagal memuat daftar kontak",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = JayraDangerRed
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = JayraSubtext,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tambah Kontak Baru",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) hasError = false
                    },
                    label = { Text("Nama Lengkap *") },
                    isError = hasError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_contact_name_input")
                )
                if (hasError) {
                    Text(
                        text = "Nama tidak boleh kosong",
                        color = JayraDangerRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor Telepon") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_contact_phone_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username Jayra (@)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_contact_username_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        hasError = true
                    } else {
                        onAdd(name, phone, username)
                    }
                },
                modifier = Modifier.testTag("btn_save_new_contact")
            ) {
                Text("Simpan", color = JayraBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = JayraSubtext)
            }
        }
    )
}

// ==========================================
// PREVIEWS (Light & Dark Mode)
// ==========================================

@Preview(name = "Contacts Screen Light", showBackground = true)
@Composable
fun ContactsScreenLightPreview() {
    val sampleContacts = listOf(
        User(
            id = "p1",
            name = "Pavel Durov",
            phoneNumber = "+1 555 739 0192",
            username = "durov",
            avatarUrl = "",
            isOnline = true,
            lastSeenFormatted = "online"
        ),
        User(
            id = "p2",
            name = "Elena Petrova",
            phoneNumber = "+44 7911 123456",
            username = "elenap",
            avatarUrl = "",
            isOnline = false,
            lastSeenFormatted = "terlihat 15 menit lalu"
        )
    )

    ContactsScreenContent(
        uiState = ContactsUiState.Success(
            contacts = sampleContacts,
            onlineUsers = sampleContacts.filter { it.isOnline }
        ),
        isSearching = false,
        searchQuery = "",
        onSearchQueryChange = {},
        onToggleSearch = {},
        onBackClick = {},
        onContactClick = {},
        onVoiceCall = {},
        onVideoCall = {},
        onDeleteContact = {},
        onToggleMute = {},
        onBlockContact = {},
        onFabClick = {}
    )
}

@Preview(name = "Contacts Screen Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ContactsScreenDarkPreview() {
    val sampleContacts = listOf(
        User(
            id = "p1",
            name = "Pavel Durov",
            phoneNumber = "+1 555 739 0192",
            username = "durov",
            avatarUrl = "",
            isOnline = true,
            lastSeenFormatted = "online"
        ),
        User(
            id = "p2",
            name = "Elena Petrova",
            phoneNumber = "+44 7911 123456",
            username = "elenap",
            avatarUrl = "",
            isOnline = false,
            lastSeenFormatted = "terlihat 15 menit lalu"
        )
    )

    ContactsScreenContent(
        uiState = ContactsUiState.Success(
            contacts = sampleContacts,
            onlineUsers = sampleContacts.filter { it.isOnline }
        ),
        isSearching = false,
        searchQuery = "",
        onSearchQueryChange = {},
        onToggleSearch = {},
        onBackClick = {},
        onContactClick = {},
        onVoiceCall = {},
        onVideoCall = {},
        onDeleteContact = {},
        onToggleMute = {},
        onBlockContact = {},
        onFabClick = {}
    )
}
