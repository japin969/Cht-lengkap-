package com.jayra.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.model.FileInfo
import com.jayra.app.model.StorageDetailUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class StorageDetailViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<StorageDetailUiState>(StorageDetailUiState.Loading)
    val uiState: StateFlow<StorageDetailUiState> = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val inMemoryFiles = mutableMapOf<String, MutableList<FileInfo>>()

    fun loadFiles(category: String) {
        viewModelScope.launch {
            _uiState.value = StorageDetailUiState.Loading
            try {
                val files = withContext(Dispatchers.IO) {
                    readCategoryFiles(category)
                }
                if (files.isEmpty()) {
                    _uiState.value = StorageDetailUiState.Empty
                } else {
                    _uiState.value = StorageDetailUiState.Success(files)
                }
            } catch (e: Exception) {
                _uiState.value = StorageDetailUiState.Error(e.localizedMessage ?: "Gagal memuat berkas")
            }
        }
    }

    fun onDeleteFile(file: FileInfo, category: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val realFile = File(file.path)
                    if (realFile.exists()) {
                        realFile.delete()
                    }
                } catch (_: Exception) {}
            }

            val list = inMemoryFiles[category] ?: mutableListOf()
            list.removeAll { it.id == file.id }
            inMemoryFiles[category] = list

            if (list.isEmpty()) {
                _uiState.value = StorageDetailUiState.Empty
            } else {
                _uiState.value = StorageDetailUiState.Success(list.toList())
            }
            _toastEvents.tryEmit("${file.name} dihapus")
        }
    }

    private fun readCategoryFiles(category: String): List<FileInfo> {
        val cacheDir = getApplication<Application>().cacheDir
        val categoryDir = File(cacheDir, category)
        if (!categoryDir.exists()) {
            categoryDir.mkdirs()
        }

        val existing = inMemoryFiles[category]
        if (!existing.isNullOrEmpty()) {
            return existing.toList()
        }

        val actualFiles = categoryDir.listFiles() ?: emptyArray()
        val result = mutableListOf<FileInfo>()

        if (actualFiles.isNotEmpty()) {
            actualFiles.forEach { f ->
                result.add(
                    FileInfo(
                        id = f.name,
                        name = f.name,
                        size = formatFileSize(f.length()),
                        date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(f.lastModified())),
                        path = f.absolutePath
                    )
                )
            }
        } else {
            // Populate realistic seed files for category
            val seedList = generateSeedFiles(category, categoryDir)
            result.addAll(seedList)
        }

        inMemoryFiles[category] = result
        return result
    }

    private fun generateSeedFiles(category: String, dir: File): List<FileInfo> {
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        return when (category.lowercase()) {
            "photo" -> listOf(
                FileInfo(UUID.randomUUID().toString(), "IMG_20261102_001.jpg", "3.2 MB", dateStr, File(dir, "IMG_20261102_001.jpg").path),
                FileInfo(UUID.randomUUID().toString(), "IMG_20261101_042.png", "2.8 MB", dateStr, File(dir, "IMG_20261101_042.png").path),
                FileInfo(UUID.randomUUID().toString(), "IMG_20261029_115.jpg", "4.1 MB", dateStr, File(dir, "IMG_20261029_115.jpg").path)
            )
            "video" -> listOf(
                FileInfo(UUID.randomUUID().toString(), "VID_20261028_102.mp4", "45.0 MB", dateStr, File(dir, "VID_20261028_102.mp4").path),
                FileInfo(UUID.randomUUID().toString(), "VID_20261025_009.mp4", "78.4 MB", dateStr, File(dir, "VID_20261025_009.mp4").path)
            )
            "document" -> listOf(
                FileInfo(UUID.randomUUID().toString(), "Jayra_Manual.pdf", "1.8 MB", dateStr, File(dir, "Jayra_Manual.pdf").path),
                FileInfo(UUID.randomUUID().toString(), "Notes_Project.docx", "540 KB", dateStr, File(dir, "Notes_Project.docx").path)
            )
            "music" -> listOf(
                FileInfo(UUID.randomUUID().toString(), "Track_Starboy_Offline.mp3", "8.5 MB", dateStr, File(dir, "Track_Starboy_Offline.mp3").path),
                FileInfo(UUID.randomUUID().toString(), "Track_BlindingLights.mp3", "7.2 MB", dateStr, File(dir, "Track_BlindingLights.mp3").path)
            )
            else -> listOf(
                FileInfo(UUID.randomUUID().toString(), "cache_temp_09.bin", "500 KB", dateStr, File(dir, "cache_temp_09.bin").path),
                FileInfo(UUID.randomUUID().toString(), "user_avatar_cache.dat", "1.2 MB", dateStr, File(dir, "user_avatar_cache.dat").path)
            )
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            else -> String.format(Locale.getDefault(), "%.0f KB", kb)
        }
    }
}
