package com.yds.almadefteri.data

import android.content.Context
import com.yds.almadefteri.domain.model.AnswerOption
import com.yds.almadefteri.domain.model.CardProgress
import com.yds.almadefteri.domain.model.CardStatus
import com.yds.almadefteri.domain.model.Difficulty
import com.yds.almadefteri.domain.model.FlashCard
import com.yds.almadefteri.domain.model.Lesson
import org.json.JSONArray
import org.json.JSONObject

interface FlashCardRepository {
    fun loadCards(): List<FlashCard>
    fun saveCards(cards: List<FlashCard>)
    fun saveLastLesson(lesson: Lesson?)
    fun loadLastLesson(): Lesson?
}

class JsonFlashCardRepository(context: Context) : FlashCardRepository {
    private val prefs = context.getSharedPreferences("ezber_kocu_storage_v1", Context.MODE_PRIVATE)

    override fun loadCards(): List<FlashCard> {
        val baseCards = QuestionBank.allCards()
        val customCards = loadCustomCards()
        val progressMap = loadProgressMap()

        return (baseCards + customCards)
            .distinctBy { it.id }
            .map { card ->
                val savedProgress = progressMap[card.id]
                if (savedProgress == null) card else card.copy(progress = savedProgress)
            }
    }

    override fun saveCards(cards: List<FlashCard>) {
        saveCustomCards(cards.filter { it.isCustomCard() })
        saveProgressMap(cards.associate { it.id to it.progress })
    }

    override fun saveLastLesson(lesson: Lesson?) {
        prefs.edit()
            .putString("last_lesson", lesson?.name)
            .apply()
    }

    override fun loadLastLesson(): Lesson? {
        val name = prefs.getString("last_lesson", null) ?: return null
        return runCatching { Lesson.valueOf(name) }.getOrNull()
    }

    private fun loadCustomCards(): List<FlashCard> {
        val raw = prefs.getString("custom_cards_json", "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val result = mutableListOf<FlashCard>()

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i)
            if (obj != null) {
                parseFlashCard(obj)?.let { result.add(it) }
            }
        }

        return result
    }

    private fun saveCustomCards(cards: List<FlashCard>) {
        val arr = JSONArray()
        cards.forEach { arr.put(cardToJson(it)) }

        prefs.edit()
            .putString("custom_cards_json", arr.toString())
            .apply()
    }

    private fun loadProgressMap(): Map<String, CardProgress> {
        val raw = prefs.getString("progress_json", "{}") ?: "{}"
        val obj = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
        val result = mutableMapOf<String, CardProgress>()

        obj.keys().forEach { cardId ->
            val progressObject = obj.optJSONObject(cardId)
            if (progressObject != null) {
                result[cardId] = parseProgress(progressObject)
            }
        }

        return result
    }

    private fun saveProgressMap(progressMap: Map<String, CardProgress>) {
        val obj = JSONObject()
        progressMap.forEach { entry ->
            obj.put(entry.key, progressToJson(entry.value))
        }

        prefs.edit()
            .putString("progress_json", obj.toString())
            .apply()
    }

    private fun cardToJson(card: FlashCard): JSONObject {
        val optionsArray = JSONArray()
        card.options.forEach { option ->
            optionsArray.put(
                JSONObject()
                    .put("id", option.id)
                    .put("label", option.label)
                    .put("text", option.text)
            )
        }

        return JSONObject()
            .put("id", card.id)
            .put("lesson", card.lesson.name)
            .put("topic", card.topic)
            .put("question", card.question)
            .put("options", optionsArray)
            .put("correctOptionId", card.correctOptionId)
            .put("explanation", card.explanation)
            .put("difficulty", card.difficulty.name)
            .put("progress", progressToJson(card.progress))
    }

    private fun parseFlashCard(obj: JSONObject): FlashCard? {
        return runCatching {
            val optionsArray = obj.getJSONArray("options")
            val options = mutableListOf<AnswerOption>()

            for (i in 0 until optionsArray.length()) {
                val optionObject = optionsArray.getJSONObject(i)
                options.add(
                    AnswerOption(
                        id = optionObject.getString("id"),
                        label = optionObject.getString("label"),
                        text = optionObject.getString("text")
                    )
                )
            }

            FlashCard(
                id = obj.getString("id"),
                lesson = Lesson.valueOf(obj.getString("lesson")),
                topic = obj.getString("topic"),
                question = obj.getString("question"),
                options = options,
                correctOptionId = obj.getString("correctOptionId"),
                explanation = obj.getString("explanation"),
                difficulty = Difficulty.valueOf(obj.getString("difficulty")),
                progress = if (obj.has("progress")) {
                    parseProgress(obj.getJSONObject("progress"))
                } else {
                    CardProgress()
                }
            )
        }.getOrNull()
    }

    private fun progressToJson(progress: CardProgress): JSONObject {
        val obj = JSONObject()
            .put("correctCount", progress.correctCount)
            .put("wrongCount", progress.wrongCount)
            .put("consecutiveCorrectCount", progress.consecutiveCorrectCount)
            .put("status", progress.status.name)

        if (progress.lastAnsweredAt == null) {
            obj.put("lastAnsweredAt", JSONObject.NULL)
        } else {
            obj.put("lastAnsweredAt", progress.lastAnsweredAt)
        }

        return obj
    }

    private fun parseProgress(obj: JSONObject): CardProgress {
        val lastAnsweredValue = obj.opt("lastAnsweredAt")
        val lastAnsweredAt = if (lastAnsweredValue == null || lastAnsweredValue == JSONObject.NULL) {
            null
        } else {
            obj.optLong("lastAnsweredAt")
        }

        val statusName = obj.optString("status", CardStatus.NEW.name)
        val status = runCatching { CardStatus.valueOf(statusName) }.getOrDefault(CardStatus.NEW)

        return CardProgress(
            correctCount = obj.optInt("correctCount", 0),
            wrongCount = obj.optInt("wrongCount", 0),
            consecutiveCorrectCount = obj.optInt("consecutiveCorrectCount", 0),
            lastAnsweredAt = lastAnsweredAt,
            status = status
        )
    }
}
