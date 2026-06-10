package com.yds.almadefteri.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.yds.almadefteri.data.FlashCardRepository
import com.yds.almadefteri.data.QuestionBank
import com.yds.almadefteri.domain.engine.FlashCardEngine
import com.yds.almadefteri.domain.model.AppScreen
import com.yds.almadefteri.domain.model.CardStatus
import com.yds.almadefteri.domain.model.EzberUiState
import com.yds.almadefteri.domain.model.FlashCard
import com.yds.almadefteri.domain.model.Lesson
import com.yds.almadefteri.domain.model.QuestionSet
import com.yds.almadefteri.domain.model.StudyMode
import com.yds.almadefteri.domain.model.StudySession

class EzberViewModel(
    private val repository: FlashCardRepository,
    val engine: FlashCardEngine
) : ViewModel() {

    var uiState by mutableStateOf(EzberUiState())
        private set

    init {
        val cards = repository.loadCards()
        val lastLesson = repository.loadLastLesson()

        uiState = EzberUiState(
            cards = cards,
            selectedLesson = lastLesson,
            screen = if (lastLesson == null) AppScreen.HOME else AppScreen.LESSON_HOME
        )
    }

    fun persistAll() {
        repository.saveCards(uiState.cards)
        repository.saveLastLesson(uiState.selectedLesson)
    }

    fun selectLesson(lesson: Lesson) {
        uiState = uiState.copy(
            selectedLesson = lesson,
            selectedQuestionSet = null,
            screen = AppScreen.LESSON_HOME
        )
        repository.saveLastLesson(lesson)
    }

    fun goHome() {
        uiState = uiState.copy(screen = AppScreen.HOME)
        persistAll()
    }

    fun goBackSmart() {
        val nextScreen = if (uiState.screen == AppScreen.LESSON_HOME || uiState.selectedLesson == null) {
            AppScreen.HOME
        } else {
            AppScreen.LESSON_HOME
        }
        uiState = uiState.copy(screen = nextScreen)
        persistAll()
    }

    fun openCreateCard() {
        uiState = uiState.copy(screen = AppScreen.CREATE_CARD)
    }

    fun openAllCards() {
        uiState = uiState.copy(screen = AppScreen.ALL_CARDS)
    }

    fun openWrongCards() {
        uiState = uiState.copy(screen = AppScreen.WRONG_CARDS)
    }

    fun openReadyTests() {
        uiState = uiState.copy(screen = AppScreen.READY_TESTS)
    }

    fun startLessonStudy() {
        uiState = uiState.copy(
            studyMode = StudyMode.LESSON_ALL,
            selectedQuestionSet = null,
            screen = AppScreen.STUDY
        )
    }

    fun startWrongStudy() {
        uiState = uiState.copy(
            studyMode = StudyMode.WRONG_ONLY,
            selectedQuestionSet = null,
            screen = AppScreen.STUDY
        )
    }

    fun startQuestionSet(set: QuestionSet) {
        uiState = uiState.copy(
            studyMode = StudyMode.QUESTION_SET,
            selectedQuestionSet = set,
            screen = AppScreen.STUDY
        )
    }

    fun updateCard(updated: FlashCard) {
        val currentCards = uiState.cards.toMutableList()
        val index = currentCards.indexOfFirst { it.id == updated.id }

        if (index >= 0) {
            currentCards[index] = updated
        } else {
            currentCards.add(updated)
        }

        uiState = uiState.copy(cards = currentCards)
        persistAll()
    }

    fun addCard(card: FlashCard) {
        uiState = uiState.copy(
            cards = listOf(card) + uiState.cards,
            screen = AppScreen.LESSON_HOME
        )
        persistAll()
    }

    fun finishSession(session: StudySession) {
        uiState = uiState.copy(
            lastSession = session,
            screen = AppScreen.SESSION_RESULT
        )
        persistAll()
    }

    fun removeFromWrongs(card: FlashCard) {
        updateCard(
            card.copy(
                progress = card.progress.copy(
                    status = CardStatus.REVIEW,
                    consecutiveCorrectCount = 0
                )
            )
        )
    }

    fun backToLesson() {
        uiState = uiState.copy(screen = AppScreen.LESSON_HOME)
        persistAll()
    }

    fun questionSetsForSelectedLesson(): List<QuestionSet> {
        val lesson = uiState.selectedLesson ?: return emptyList()
        return QuestionBank.sets.filter { it.lesson == lesson }
    }
}
