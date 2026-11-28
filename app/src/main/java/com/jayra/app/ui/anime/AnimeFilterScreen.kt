package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
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
private val NetflixDivider = Color(0xFF262626)

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
    val expandedSections = remember { mutableMapOf("genres" to true, "seasons" to false, "studios" to false, "status" to true, "types" to false, "orders" to true) }

    LaunchedEffect(Unit) { viewModel.loadFilterList() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { onDismiss() }) {
        FilterSheetContent(state, expandedSections, onApplyClick, onDismiss, viewModel)
    }
}

@Composable
fun FilterSheetContent(
    state: AnimeFilterUiState,
    expandedSections: MutableMap<String, Boolean>,
    onApplyClick: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: AnimeViewModel
) {
    Column(modifier = Modifier.fillMaxWidth().height(500.dp).background(NetflixDarkGray, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).padding(bottom = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Filter Anime", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { viewModel.resetFilter() }) { Text("Reset", fontSize = 14.sp, color = NetflixTextSecondary) }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", NetflixWhite, Modifier.size(24.dp)) }
            }
        }
        Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).height(400.dp).verticalScroll(rememberScrollState())) {
            FilterSection("Genre", state.filterList?.genres ?: emptyList(), state.selectedGenres, expandedSections["genres"] ?: true, { expandedSections["genres"] = !(expandedSections["genres"] ?: true) }, { viewModel.applyFilter(genres = it) }, true)
            FilterSection("Season", state.filterList?.seasons ?: emptyList(), state.selectedSeasons, expandedSections["seasons"] ?: false, { expandedSections["seasons"] = !(expandedSections["seasons"] ?: false) }, { viewModel.applyFilter(seasons = it) }, true)
            FilterSection("Studio", state.filterList?.studios ?: emptyList(), state.selectedStudios, expandedSections["studios"] ?: false, { expandedSections["studios"] = !(expandedSections["studios"] ?: false) }, { viewModel.applyFilter(studios = it) }, true)
            FilterSection("Status", state.filterList?.status ?: emptyList(), state.selectedStatus, expandedSections["status"] ?: true, { expandedSections["status"] = !(expandedSections["status"] ?: true) }, { viewModel.applyFilter(status = it) }, true)
            FilterSection("Type", state.filterList?.types ?: emptyList(), state.selectedTypes, expandedSections["types"] ?: false, { expandedSections["types"] = !(expandedSections["types"] ?: false) }, { viewModel.applyFilter(types = it) }, true)
            FilterSection("Urutkan", state.filterList?.orders ?: emptyList(), state.selectedOrder?.let { listOf(it) } ?: emptyList(), expandedSections["orders"] ?: true, { expandedSections["orders"] = !(expandedSections["orders"] ?: true) }, { viewModel.applyFilter(order = it.firstOrNull()) }, false)
        }
        Button(onClick = onApplyClick, modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp), colors = ButtonDefaults.buttonColors(NetflixRed, NetflixWhite), shape = RoundedCornerShape(8.dp)) { Text("Terapkan Filter", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
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
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { onToggleExpand() }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selected.isNotEmpty() && multiSelect) Text("${selected.size} dipilih", fontSize = 12.sp, color = NetflixTextSecondary)
                Icon(Icons.Default.ExpandMore, null, NetflixWhite, Modifier.size(20.dp).graphicsLayer { rotationZ = if (showContent.value) 180f else 0f })
            }
        }
        AnimatedVisibility(visible = showContent.value) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val isSelected = option.slug?.let { selected.contains(it) } == true
                    val displayName = option.name ?: option.slug ?: ""
                    val count = option.count?.let { " ($it)" } ?: ""
                    if (multiSelect) {
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp).background(if (isSelected) NetflixRed.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(8.dp)).clickable { onSelectionChange(if (isSelected) selected.filter { it != option.slug } else selected + (option.slug ?: "")) }) {
                            Box(modifier = Modifier.size(20.dp).border(2.dp, if (isSelected) NetflixRed else NetflixTextMetadata, RoundedCornerShape(4.dp)).background(if (isSelected) NetflixRed else Color.Transparent)) { if (isSelected) Icon(Icons.Default.Check, null, NetflixWhite, Modifier.size(14.dp).align(Alignment.Center)) }
                            Spacer(Modifier.width(12.dp))
                            Text(displayName, fontSize = 14.sp, color = NetflixWhite, modifier = Modifier.weight(1f))
                            Text(count, fontSize = 12.sp, color = NetflixTextMetadata)
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp).background(if (isSelected) NetflixRed.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(8.dp)).clickable { option.slug?.let { onSelectionChange(listOf(it)) } }) {
                            Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).border(2.dp, if (isSelected) NetflixRed else NetflixTextMetadata).background(if (isSelected) NetflixRed else Color.Transparent))
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
    Surface(color = NetflixBlack, modifier = Modifier.fillMaxWidth().height(40.dp).padding(vertical = 4.dp, horizontal = 16.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chips) { chip ->
                val isSelected = selectedChip == chip.id
                FilterChip(selected = isSelected, onClick = { onChipClick(chip.id) }, label = { Text(chip.label) }, modifier = Modifier.height(32.dp), colors = FilterChipDefaults.colors(containerColor = if (isSelected) NetflixWhite else NetflixCardGray, labelColor = if (isSelected) NetflixBlack else NetflixWhite, selectedContainerColor = NetflixWhite, selectedLabelColor = NetflixBlack), shape = RoundedCornerShape(16.dp))
            }
            item { IconButton(onClick = onFilterClick) { Icon(Icons.Default.FilterList, "Filter", NetflixWhite, Modifier.size(20.dp).padding(6.dp)) }.background(NetflixCardGray, RoundedCornerShape(16.dp)) }
        }
    }
}

data class FilterChipData(val label: String, val id: String)

fun defaultFilterChips(): List<FilterChipData> = listOf(
    FilterChipData("Semua", "all"), FilterChipData("Ongoing", "ongoing"), FilterChipData("Completed", "completed"),
    FilterChipData("Popular", "popular"), FilterChipData("New", "new"), FilterChipData("Top Rated", "top_rated"),
    FilterChipData("Batch", "batch"), FilterChipData("Movie", "movie"), FilterChipData("Live-Action", "live_action"),
    FilterChipData("Donghua", "donghua"), FilterChipData("Tokusatsu", "tokusatsu"), FilterChipData("Upcoming", "upcoming"),
    FilterChipData("Schedule", "schedule"), FilterChipData("Action", "action"), FilterChipData("Fantasy", "fantasy"),
    FilterChipData("Romance", "romance"), FilterChipData("Comedy", "comedy"), FilterChipData("Adventure", "adventure")
)