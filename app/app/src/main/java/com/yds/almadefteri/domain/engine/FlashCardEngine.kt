package com.yds.almadefteri.domain.engine

import com.yds.almadefteri.domain.model.AnswerResult
import com.yds.almadefteri.domain.model.CardStatus
import com.yds.almadefteri.domain.model.FlashCard
import com.yds.almadefteri.domain.model.Lesson
import com.yds.almadefteri.domain.model.LessonWorkspace
import com.yds.almadefteri.domain.model.QuestionSet
import com.yds.almadefteri.domain.model.StudyMode

interface ReviewPolicy {
    fun apply(card: FlashCard, result: AnswerResult): FlashCard
}

class TwoCorrectReviewPolicy : ReviewPolicy {
    override fun apply(card: FlashCard, result: AnswerResult): FlashCard {
        val old = card.progress
        val now = System.currentTimeMillis()

        val newProgress = if (result.isCorrect) {
            val newConsecutive = old.consecutiveCorrectCount + 1

            val newStatus = when {
                old.status == CardStatus.WRONG && newConsecutive >= 2 -> CardStatus.REVIEW
                old.correctCount + 1 >= 4 && old.wrongCount == 0 -> CardStatus.MASTERED
                old.status == CardStatus.WRONG -> CardStatus.WRONG
                else -> CardStatus.LEARNING
            }

            old.copy(
                correctCount = old.correctCount + 1,
                consecutiveCorrectCount = newConsecutive,
                lastAnsweredAt = now,
                status = newStatus
            )
        } else {
            old.copy(
                wrongCount = old.wrongCount + 1,
                consecutiveCorrectCount = 0,
                lastAnsweredAt = now,
                status = CardStatus.WRONG
            )
        }

        return card.copy(progress = newProgress)
    }
}

class FlashCardEngine(
    private val reviewPolicy: ReviewPolicy = TwoCorrectReviewPolicy()
) {

    fun createWorkspace(
        lesson: Lesson,
        allCards: List<FlashCard>,
        allSets: List<QuestionSet>
    ): LessonWorkspace {
        val lessonCards = allCards.filter { it.lesson == lesson }
        val wrongCards = lessonCards.filter { it.progress.status == CardStatus.WRONG }
        val lessonSets = allSets.filter { it.lesson == lesson }

        return LessonWorkspace(
            lesson = lesson,
            cards = lessonCards,
            wrongCards = wrongCards,
            questionSets = lessonSets
        )
    }

    fun checkAnswer(
        card: FlashCard,
        selectedOptionId: String
    ): AnswerResult {
        val selected = card.options.firstOrNull { it.id == selectedOptionId }
            ?: card.options.firstOrNull()
            ?: throw IllegalArgumentException("Kartta seçenek bulunamadı.")

        val correct = card.correctOption()
        val isCorrect = selectedOptionId == card.correctOptionId
        val wasWrong = card.progress.status == CardStatus.WRONG

        val willBeRemovedFromWrongList =
            isCorrect && wasWrong && card.progress.consecutiveCorrectCount + 1 >= 2

        return AnswerResult(
            cardId = card.id,
            selectedOptionLabel = selected.label,
            selectedOptionText = selected.text,
            correctOptionLabel = correct.label,
            correctOptionText = correct.text,
            explanation = card.explanation,
            isCorrect = isCorrect,
            movedToWrongList = !isCorrect,
            removedFromWrongList = willBeRemovedFromWrongList
        )
    }

    fun applyAnswerResult(card: FlashCard, result: AnswerResult): FlashCard {
        return reviewPolicy.apply(card, result)
    }

    fun getStudyCards(
        allCards: List<FlashCard>,
        studyMode: StudyMode,
        selectedLesson: Lesson?,
        selectedSet: QuestionSet?
    ): List<FlashCard> {
        return when (studyMode) {
            StudyMode.LESSON_ALL -> {
                if (selectedLesson == null) emptyList()
                else allCards
                    .filter { it.lesson == selectedLesson }
                    .sortedWith(
                        compareByDescending<FlashCard> { it.progress.status == CardStatus.WRONG }
                            .thenBy { it.progress.lastAnsweredAt ?: 0L }
                            .thenByDescending { it.difficulty.ordinal }
                    )
            }

            StudyMode.WRONG_ONLY -> {
                if (selectedLesson == null) emptyList()
                else allCards.filter {
                    it.lesson == selectedLesson && it.progress.status == CardStatus.WRONG
                }
            }

            StudyMode.QUESTION_SET -> {
                val setQuestions = selectedSet?.questions ?: emptyList()
                setQuestions.map { setCard ->
                    allCards.firstOrNull { it.id == setCard.id } ?: setCard
                }
            }
        }
    }
}
