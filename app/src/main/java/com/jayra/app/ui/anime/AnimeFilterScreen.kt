package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.AnimeKompiFilterListData
import com.jayra.app.data.AnimeKompiFilterOption
import com.jayra.app.ui.theme.JayraTheme

// ============================================================================
// COLOR CONSTANTS
// ============================================================================

private val NetflixBlack = Color(0xFF000000)
private val NetflixDarkGray = Color(0xFF181818)
private val NetflixCardGray = Color(0xFF262626)
private val NetflixWhite = Color(0xFFFFFFFF)
private val NetflixTextPrimary = Color(0xFFFFFFFF)
private val NetflixTextSecondary = Color(0xFFB3B3B3)
private val NetflixTextMetadata = Color(0xFF8C8C8C)
private val NetflixRed = Color(0xFFE50914)

// ============================================================================
// FILTER BOTTOM SHEET
// ============================================================================

@Composable
fun AnimeFilterBottomSheet(
    viewModel: AnimeViewModel = viewModel(),
    onApplyClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val state by viewModel.filterState.collectAsState()
    val expandedSections = remember {
        mutableStateMapOf<String, Boolean>(
            "genres" to true,
            "seasons" to false,
            "studios" to false,
            "status" to true,
            "types" to false,
            "orders" to true,
        )
    }

    // Load filter list on first open
    LaunchedEffect(Unit) {
        viewModel.loadFilterList()
    }

    // Use a simple BottomSheet instead of ModalBottomSheetLayout
    val isSheetOpen = remember { mutableStateOf(true) }

    if (!isSheetOpen.value) return

    // Simple bottom sheet implementation
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { isSheetOpen.value = false; onDismiss() }
    ) {
        FilterSheetContent(
            state = state,
            expandedSections = expandedSections,
            onApplyClick = onApplyClick,
            onDismiss = { isSheetOpen.value = false; onDismiss() },
            viewModel = viewModel
        )
    }
}

@Composable
fun FilterSheetContent(
    state: AnimeFilterUiState,
    expandedSections: MutableStateMap<String, Boolean>,
    onApplyClick: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: AnimeViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .background(NetflixDarkGray, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(bottom = 16.dp) // Handle bottom padding
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filter Anime", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    viewModel.resetFilter()
                }) {
                    Text("Reset", fontSize = 14.sp, color = NetflixTextSecondary)
                }
                IconButton(onClick = { /* handled by parent */ }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NetflixWhite, modifier = Modifier.size(24.dp))
                }
            }
        }

        androidx.compose.material3.Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))

        // Scrollable filter content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Genres
            FilterSection(
                title = "Genre",
                options = state.filterList?.genres ?: emptyList(),
                selected = state.selectedGenres,
                isExpanded = expandedSections["genres"] ?: true,
                onToggleExpand = { expandedSections["genres"] = !(expandedSections["genres"] ?: true) },
                onSelectionChange = { selected ->
                    viewModel.applyFilter(genres = selected)
                },
                multiSelect = true
            )

            // Seasons
            FilterSection(
                title = "Season",
                options = state.filterList?.seasons ?: emptyList(),
                selected = state.selectedSeasons,
                isExpanded = expandedSections["seasons"] ?: false,
                onToggleExpand = { expandedSections["seasons"] = !(expandedSections["seasons"] ?: false) },
                onSelectionChange = { selected ->
                    viewModel.applyFilter(seasons = selected)
                },
                multiSelect = true
            )

            // Studios
            FilterSection(
                title = "Studio",
                options = state.filterList?.studios ?: emptyList(),
                selected = state.selectedStudios,
                isExpanded = expandedSections["studios"] ?: false,
                onToggleExpand = { expandedSections["studios"] = !(expandedSections["studios"] ?: false) },
                onSelectionChange = { selected ->
                    viewModel.applyFilter(studios = selected)
                },
                multiSelect = true
            )

            // Status
            FilterSection(
                title = "Status",
                options = state.filterList?.status ?: emptyList(),
                selected = state.selectedStatus,
                isExpanded = expandedSections["status"] ?: true,
                onToggleExpand = { expandedSections["status"] = !(expandedSections["status"] ?: true) },
                onSelectionChange = { selected ->
                    viewModel.applyFilter(status = selected)
                },
                multiSelect = true
            )

            // Types
            FilterSection(
                title = "Type",
                options = state.filterList?.types ?: emptyList(),
                selected = state.selectedTypes,
                isExpanded = expandedSections["types"] ?: false,
                onToggleExpand = { expandedSections["types"] = !(expandedSections["types"] ?: false) },
                onSelectionChange = { selected ->
                    viewModel.applyFilter(types = selected)
                },
                multiSelect = true
            )

            // Orders (Single select)
            FilterSection(
                title = "Urutkan",
                options = state.filterList?.orders ?: emptyList(),
                selected = state.selectedOrder?.let { listOf(it) } ?: emptyList(),
                isExpanded = expandedSections["orders"] ?: true,
                onToggleExpand = { expandedSections["orders"] = !(expandedSections["orders"] ?: true) },
                onSelectionChange = { selected ->
                    viewModel.applyFilter(order = selected.firstOrNull())
                },
                multiSelect = false
            )
        }

        // Apply button
        Button(
            onClick = onApplyClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NetflixRed, contentColor = NetflixWhite),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Terapkan Filter", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================================================
// FILTER SECTION
// ============================================================================

@Composable
fun FilterSection(
    title: String,
    options: List<AnimeKompiFilterOption>,
    selected: List<String>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectionChange: (List<String>) -> Unit,
    multiSelect: Boolean
) {
    val showContent = remember { mutableStateOf(isExpanded) }
    LaunchedEffect(isExpanded) { showContent.value = isExpanded }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable { onToggleExpand() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selected.isNotEmpty() && multiSelect) {
                    Text("${selected.size} dipilih", fontSize = 12.sp, color = NetflixTextSecondary)
                }
                // Use a simple icon instead of ExpandMore
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = NetflixWhite,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (showContent.value) 180f else 0f)
                )
            }
        }

        // Options - using crossfade for animation instead of AnimatedVisibility
        androidx.compose.animation.AnimatedVisibility(visible = showContent.value) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    val isSelected = option.slug?.let { selected.contains(it) } == true
                    val displayName = option.name ?: option.slug ?: ""
                    val count = option.count?.let { " ($it)" } ?: ""

                    if (multiSelect) {
                        // Checkbox style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 8.dp)
                                .background(if (isSelected) NetflixRed.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { onSelectionChange(if (isSelected) selected.filter { it != option.slug } else selected + (option.slug ?: "")) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(2.dp, if (isSelected) NetflixRed else NetflixTextMetadata, RoundedCornerShape(4.dp))
                                    .background(if (isSelected) NetflixRed else Color.Transparent)
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NetflixWhite, modifier = Modifier.size(14.dp).align(Alignment.Center))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(displayName, fontSize = 14.sp, color = NetflixWhite, modifier = Modifier.weight(1f))
                            Text(count, fontSize = 12.sp, color = NetflixTextMetadata)
                        }
                    } else {
                        // Radio style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 8.dp)
                                .background(if (isSelected) NetflixRed.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { option.slug?.let { onSelectionChange(listOf(it)) } }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(2.dp, if (isSelected) NetflixRed else NetflixTextMetadata)
                                    .background(if (isSelected) NetflixRed else Color.Transparent)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(displayName, fontSize = 14.sp, color = NetflixWhite, modifier = Modifier.weight(1f))
                            Text(count, fontSize = 12.sp, color = NetflixTextMetadata)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// FILTER CHIP ROW (for Home Screen)
// ============================================================================

@Composable
fun AnimeFilterChipRow(
    selectedChip: String,
    onChipClick: (String) -> Unit,
    onFilterClick: () -> Unit,
    chips: List<FilterChipData> = defaultFilterChips()
) {
    Surface(
        color = NetflixBlack,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(vertical = 4.dp, horizontal = 16.dp)
    ) {
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chips) { chip ->
                val isSelected = selectedChip == chip.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onChipClick(chip.id) },
                    label = { Text(chip.label) },
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.colors(
                        containerColor = if (isSelected) NetflixWhite else NetflixCardGray,
                        labelColor = if (isSelected) NetflixBlack else NetflixWhite,
                        selectedContainerColor = NetflixWhite,
                        selectedLabelColor = NetflixBlack
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
            // Filter button at end
            item {
                IconButton(onClick = onFilterClick) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = NetflixWhite, modifier = Modifier.size(20.dp).padding(6.dp))
                }
                .background(NetflixCardGray, RoundedCornerShape(16.dp))
            }
        }
    }
}

data class FilterChipData(
    val label: String,
    val id: String
)

fun defaultFilterChips(): List<FilterChipData> {
    return listOf(
        FilterChipData("Semua", "all"),
        FilterChipData("Ongoing", "ongoing"),
        FilterChipData("Completed", "completed"),
        FilterChipData("Popular", "popular"),
        FilterChipData("New", "new"),
        FilterChipData("Top Rated", "top_rated"),
        FilterChipData("Batch", "batch"),
        FilterChipData("Movie", "movie"),
        FilterChipData("Live-Action", "live_action"),
        FilterChipData("Donghua", "donghua"),
        FilterChipData("Tokusatsu", "tokusatsu"),
        FilterChipData("Upcoming", "upcoming"),
        FilterChipData("Schedule", "schedule"),
        FilterChipData("Action", "action"),
        FilterChipData("Fantasy", "fantasy"),
        FilterChipData("Romance", "romance"),
        FilterChipData("Comedy", "comedy"),
        FilterChipData("Adventure", "adventure"),
    )
}