package com.jayra.app.ui.poll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.di.AppModule
import com.jayra.app.model.CreatePollUiState
import com.jayra.app.model.Poll
import com.jayra.app.model.PollOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CreatePollViewModel(
    private val chatRepository: com.jayra.app.data.ChatRepository = AppModule.chatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreatePollUiState>(CreatePollUiState.Idle)
    val uiState: StateFlow<CreatePollUiState> = _uiState.asStateFlow()

    private val _question = MutableStateFlow("")
    val question: StateFlow<String> = _question.asStateFlow()

    private val _options = MutableStateFlow(listOf("", ""))
    val options: StateFlow<List<String>> = _options.asStateFlow()

    private val _isAnonymous = MutableStateFlow(true)
    val isAnonymous: StateFlow<Boolean> = _isAnonymous.asStateFlow()

    private val _allowMultiple = MutableStateFlow(false)
    val allowMultiple: StateFlow<Boolean> = _allowMultiple.asStateFlow()

    private val _isQuiz = MutableStateFlow(false)
    val isQuiz: StateFlow<Boolean> = _isQuiz.asStateFlow()

    private val _correctOptionIndex = MutableStateFlow<Int?>(null)
    val correctOptionIndex: StateFlow<Int?> = _correctOptionIndex.asStateFlow()

    val canSend: Boolean
        get() {
            val validQuestion = _question.value.trim().isNotEmpty()
            val validOptions = _options.value.filter { it.trim().isNotEmpty() }.size >= 2
            val quizValid = if (_isQuiz.value) _correctOptionIndex.value != null else true
            return validQuestion && validOptions && quizValid
        }

    fun onQuestionChange(newQuestion: String) {
        _question.value = newQuestion
    }

    fun onOptionChange(index: Int, newText: String) {
        _options.update { list ->
            list.toMutableList().also {
                if (index in it.indices) {
                    it[index] = newText
                }
            }
        }
    }

    fun addOption() {
        if (_options.value.size < 10) {
            _options.update { it + "" }
        }
    }

    fun removeOption(index: Int) {
        if (_options.value.size > 2) {
            _options.update { list ->
                list.toMutableList().also {
                    if (index in it.indices) {
                        it.removeAt(index)
                    }
                }
            }
            if (_correctOptionIndex.value == index) {
                _correctOptionIndex.value = null
            } else if ((_correctOptionIndex.value ?: -1) > index) {
                _correctOptionIndex.value = (_correctOptionIndex.value ?: 1) - 1
            }
        }
    }

    fun toggleAnonymous(enabled: Boolean) {
        _isAnonymous.value = enabled
    }

    fun toggleMultipleAnswers(enabled: Boolean) {
        if (enabled && _isQuiz.value) {
            _isQuiz.value = false
        }
        _allowMultiple.value = enabled
    }

    fun toggleQuizMode(enabled: Boolean) {
        if (enabled && _allowMultiple.value) {
            _allowMultiple.value = false
        }
        _isQuiz.value = enabled
        if (!enabled) {
            _correctOptionIndex.value = null
        }
    }

    fun selectCorrectOption(index: Int) {
        if (_isQuiz.value) {
            _correctOptionIndex.value = index
        }
    }

    fun sendPoll(chatId: String, onSuccess: () -> Unit) {
        if (!canSend) return

        viewModelScope.launch {
            _uiState.value = CreatePollUiState.Loading
            try {
                val cleanedOptions = _options.value
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { PollOption(text = it, voteCount = 0) }

                val poll = Poll(
                    id = UUID.randomUUID().toString(),
                    question = _question.value.trim(),
                    options = cleanedOptions,
                    isAnonymous = _isAnonymous.value,
                    allowMultiple = _allowMultiple.value,
                    isQuiz = _isQuiz.value,
                    correctOptionIndex = _correctOptionIndex.value
                )

                // Send message to chat
                val pollBadge = if (poll.isQuiz) "🧠 Kuis" else "📊 Poling"
                val optionSummary = cleanedOptions.mapIndexed { idx, opt -> "${idx + 1}. ${opt.text}" }.joinToString("\n")
                val messageText = "$pollBadge: ${poll.question}\n$optionSummary"
                
                chatRepository.sendMessage(
                    chatId = chatId,
                    text = messageText
                )

                _uiState.value = CreatePollUiState.Success
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = CreatePollUiState.Error(e.message ?: "Gagal membuat poling")
            }
        }
    }
}
