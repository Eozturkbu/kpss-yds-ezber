package com.yds.almadefteri.domain.model

import java.util.UUID

enum class Lesson(val title: String, val emoji: String) {
    HISTORY("Tarih", "📜"),
    CITIZENSHIP("Vatandaşlık", "⚖️"),
    GEOGRAPHY("Coğrafya", "🌍"),
    YDS("YDS", "🇬🇧"),
    GENERAL("Genel", "📚")
}

enum class Difficulty(val title: String) {
    EASY("Kolay"),
    MEDIUM("Orta"),
    HARD("Zor"),
    VERY_HARD("Çok Zor")
}

enum class CardStatus(val title: String) {
    NEW("Yeni"),
    LEARNING("Çalışılıyor"),
    WRONG("Yanlışta"),
    REVIEW("Tekrar"),
    MASTERED("Öğrenildi")
}

enum class AppScreen {
    HOME,
    LESSON_HOME,
    STUDY,
    CREATE_CARD,
    WRONG_CARDS,
    ALL_CARDS,
    READY_TESTS,
    SESSION_RESULT
}

enum class StudyMode {
    LESSON_ALL,
    WRONG_ONLY,
    QUESTION_SET
}

data class AnswerOption(
    val id: String,
    val label: String,
    val text: String
)

data class CardProgress(
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val consecutiveCorrectCount: Int = 0,
    val lastAnsweredAt: Long? = null,
    val status: CardStatus = CardStatus.NEW
)

data class FlashCard(
    val id: String,
    val lesson: Lesson,
    val topic: String,
    val question: String,
    val options: List<AnswerOption>,
    val correctOptionId: String,
    val explanation: String,
    val difficulty: Difficulty,
    val progress: CardProgress = CardProgress()
) {
    fun correctOptionOrNull(): AnswerOption? {
        return options.firstOrNull { it.id == correctOptionId }
    }

    fun correctOption(): AnswerOption {
        return correctOptionOrNull() ?: AnswerOption(
            id = correctOptionId,
            label = correctOptionId,
            text = "Doğru seçenek bulunamadı."
        )
    }

    fun isCustomCard(): Boolean {
        return id.startsWith("custom_")
    }
}

data class QuestionSet(
    val id: String,
    val title: String,
    val lesson: Lesson,
    val description: String,
    val questions: List<FlashCard>
)

data class LessonWorkspace(
    val lesson: Lesson,
    val cards: List<FlashCard>,
    val wrongCards: List<FlashCard>,
    val questionSets: List<QuestionSet>
) {
    fun learnedCount(): Int {
        return cards.count { it.progress.status == CardStatus.MASTERED }
    }

    fun activeCount(): Int {
        return cards.count {
            it.progress.status == CardStatus.NEW ||
                it.progress.status == CardStatus.LEARNING ||
                it.progress.status == CardStatus.REVIEW
        }
    }
}

data class StudySession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val lesson: Lesson?,
    val cards: List<FlashCard>,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val answeredCount: Int = 0
) {
    fun totalCount(): Int = cards.size

    fun successRate(): Int {
        if (answeredCount == 0) return 0
        return ((correctCount.toDouble() / answeredCount.toDouble()) * 100).toInt()
    }
}

data class AnswerResult(
    val cardId: String,
    val selectedOptionLabel: String,
    val selectedOptionText: String,
    val correctOptionLabel: String,
    val correctOptionText: String,
    val explanation: String,
    val isCorrect: Boolean,
    val movedToWrongList: Boolean,
    val removedFromWrongList: Boolean
)

data class EzberUiState(
    val cards: List<FlashCard> = emptyList(),
    val screen: AppScreen = AppScreen.HOME,
    val selectedLesson: Lesson? = null,
    val studyMode: StudyMode = StudyMode.LESSON_ALL,
    val selectedQuestionSet: QuestionSet? = null,
    val lastSession: StudySession? = null
)
