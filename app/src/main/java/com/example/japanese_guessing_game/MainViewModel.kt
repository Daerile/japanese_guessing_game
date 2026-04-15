package com.example.japanese_guessing_game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanese_guessing_game.data.JapaneseWord
import com.example.japanese_guessing_game.data.JapaneseWordInsert
import com.example.japanese_guessing_game.data.ReturnValue
import com.example.japanese_guessing_game.data.WordRepository
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException

enum class GameType { TRANSLATION, PICKER }
enum class GameDirection { JAPANESE_TO_ENGLISH, ENGLISH_TO_JAPANESE }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WordRepository(application)

    private val _words = MutableStateFlow<List<JapaneseWord>>(emptyList())
    val words: StateFlow<List<JapaneseWord>> = _words.asStateFlow()

    private val _currentWord = MutableStateFlow<JapaneseWord?>(null)
    val currentWord: StateFlow<JapaneseWord?> = _currentWord.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _feedbackMessage = MutableStateFlow("")
    val feedbackMessage: StateFlow<String> = _feedbackMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Game State
    private var _gameType = GameType.TRANSLATION
    private var _gameDirection = GameDirection.JAPANESE_TO_ENGLISH
    
    // Picker Choices
    private val _choices = MutableStateFlow<List<String>>(emptyList())
    val choices: StateFlow<List<String>> = _choices.asStateFlow()

    init {
        // Try to load cached words on startup
        viewModelScope.launch {
            val cached = repository.loadFromCache()
            if (cached.isNotEmpty()) {
                _words.value = cached
            }
        }
    }

    fun startGame(type: GameType, direction: GameDirection) {
        _gameType = type
        _gameDirection = direction
        _score.value = 0
        _feedbackMessage.value = ""
        nextWord()
    }

    fun loadWords() {
        if (_isLoading.value) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _feedbackMessage.value = ""
            
            try {
                val fetchedWords = repository.getAllWords()
                if (fetchedWords.isNotEmpty()) {
                    _words.value = fetchedWords
                } else {
                    _error.value = "No words found in database."
                }
            } catch (e: TimeoutCancellationException) {
                _error.value = "Loading timed out. Please check your connection."
            } catch (e: Exception) {
                _error.value = "Failed to load words: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun nextWord() {
        val wordList = _words.value
        if (wordList.isNotEmpty()) {
            val next = wordList.random()
            _currentWord.value = next
            _feedbackMessage.value = ""
            
            if (_gameType == GameType.PICKER) {
                generateChoices(next, wordList)
            }
        }
    }

    private fun generateChoices(correct: JapaneseWord, allWords: List<JapaneseWord>) {
        val correctAns = if (_gameDirection == GameDirection.JAPANESE_TO_ENGLISH) correct.meaning_1 else correct.hiragana
        
        // Pick 3 random distractors
        val distractors = allWords.filter { it.id != correct.id }
            .shuffled()
            .take(3)
            .map { if (_gameDirection == GameDirection.JAPANESE_TO_ENGLISH) it.meaning_1 else it.hiragana }
        
        _choices.value = (distractors + correctAns).shuffled()
    }

    fun checkAnswer(answer: String) {
        val current = _currentWord.value ?: return
        var isCorrect = false
        var correctString = ""

        if (_gameDirection == GameDirection.JAPANESE_TO_ENGLISH) {
            // Question: Hiragana -> Answer: Meaning (English)
            correctString = "${current.meaning_1} or ${current.meaning_2 ?: ""}"
            if (answer.equals(current.meaning_1, ignoreCase = true) || 
                (current.meaning_2 != null && answer.equals(current.meaning_2, ignoreCase = true))) {
                isCorrect = true
            }
        } else {
            // Question: Meaning (English) -> Answer: Hiragana
            correctString = current.hiragana
            if (answer.equals(current.hiragana, ignoreCase = true)) {
                isCorrect = true
            }
        }

        if (isCorrect) {
            _score.value += 1
            _feedbackMessage.value = "Correct!"
            // Small delay or immediate next? For picker, immediate often feels better or small delay.
            // For translation, usually immediate next logic.
            nextWord()
        } else {
            _feedbackMessage.value = "Incorrect. Try again!"
        }
    }
    
    fun skipWord() {
        val current = _currentWord.value ?: return
        val ans = if (_gameDirection == GameDirection.JAPANESE_TO_ENGLISH) current.meaning_1 else current.hiragana
        _feedbackMessage.value = "Skipped. Answer was: $ans"
        
        // Delay moving to next word so user can see answer
        viewModelScope.launch {
            delay(3000)
            nextWord()
        }
    }

    suspend fun addWord(hiragana: String, meaning1: String, meaning2: String?): ReturnValue {
        var result: ReturnValue = ReturnValue(0, null)
        _isLoading.value = true
        try {
            result = repository.addWord(JapaneseWordInsert(meaning_1 = meaning1, meaning_2 = meaning2 ?: "none", hiragana = hiragana))
        } catch (e: Exception) {
            _error.value = "Failed to add word: ${e.message}"
        } finally {
            _isLoading.value = false
        }
        return result
    }
    
    fun clearError() {
        _error.value = null
    }

    fun getQuestionText(): String {
        val current = _currentWord.value ?: return ""
        return if (_gameDirection == GameDirection.JAPANESE_TO_ENGLISH) {
            current.hiragana
        } else {
            current.meaning_1
        }
    }
    
    fun getCurrentGameMode(): GameType = _gameType
    fun getCurrentDirection(): GameDirection = _gameDirection
}
