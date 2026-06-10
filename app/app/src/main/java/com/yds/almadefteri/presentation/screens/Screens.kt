package com.yds.almadefteri.presentation.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yds.almadefteri.domain.engine.FlashCardEngine
import com.yds.almadefteri.domain.model.AnswerResult
import com.yds.almadefteri.domain.model.AnswerOption
import com.yds.almadefteri.domain.model.AppScreen
import com.yds.almadefteri.domain.model.Difficulty
import com.yds.almadefteri.domain.model.FlashCard
import com.yds.almadefteri.domain.model.Lesson
import com.yds.almadefteri.domain.model.LessonWorkspace
import com.yds.almadefteri.domain.model.QuestionSet
import com.yds.almadefteri.domain.model.StudyMode
import com.yds.almadefteri.domain.model.StudySession
import java.util.UUID

@Composable
fun EzberAppScaffold(
    screen: AppScreen,
    selectedLesson: Lesson?,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ezber Koçu",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedLesson?.let { "${it.emoji} ${it.title} Çalışma Alanı" }
                        ?: "KPSS & YDS Flash Kart",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (screen != AppScreen.HOME) {
                TextButton(onClick = onBack) {
                    Text(
                        if (screen == AppScreen.LESSON_HOME) {
                            "Dersler"
                        } else {
                            "Derse Dön"
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        content()
    }
}

@Composable
fun LessonSelectionScreen(
    cards: List<FlashCard>,
    engine: FlashCardEngine,
    lastLesson: Lesson?,
    onSelectLesson: (Lesson) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Hangi dersi çalışacağız?",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        if (lastLesson != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Son çalıştığın ders: ${lastLesson.emoji} ${lastLesson.title}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        onClick = { onSelectLesson(lastLesson) }
                    ) {
                        Text("Devam Et")
                    }
                }
            }
        }

        Lesson.values().forEach { lesson ->
            val workspace = engine.createWorkspace(
                lesson = lesson,
                allCards = cards,
                allSets = com.yds.almadefteri.data.QuestionBank.sets
            )

            LessonCard(
                workspace = workspace,
                onClick = { onSelectLesson(lesson) }
            )
        }
    }
}

@Composable
fun LessonCard(
    workspace: LessonWorkspace,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${workspace.lesson.emoji} ${workspace.lesson.title}",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Kart: ${workspace.cards.size} | Yanlış: ${workspace.wrongCards.size} | Test: ${workspace.questionSets.size}"
            )

            Button(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                onClick = onClick
            ) {
                Text("${workspace.lesson.title} Dersine Gir")
            }
        }
    }
}

@Composable
fun LessonHomeScreen(
    workspace: LessonWorkspace,
    onStartStudy: () -> Unit,
    onCreateCard: () -> Unit,
    onWrongCards: () -> Unit,
    onAllCards: () -> Unit,
    onReadyTests: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "${workspace.lesson.emoji} ${workspace.lesson.title}",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Toplam Kart", workspace.cards.size.toString(), Modifier.weight(1f))
            StatCard("Yanlış Kart", workspace.wrongCards.size.toString(), Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Aktif Kart", workspace.activeCount().toString(), Modifier.weight(1f))
            StatCard("Hazır Test", workspace.questionSets.size.toString(), Modifier.weight(1f))
        }

        Button(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            onClick = onStartStudy
        ) {
            Text("Bu Dersi Çalış", fontSize = 17.sp)
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            onClick = onCreateCard
        ) {
            Text("${workspace.lesson.title} Kartı Ekle", fontSize = 17.sp)
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            onClick = onWrongCards
        ) {
            Text("${workspace.lesson.title} Yanlışları", fontSize = 17.sp)
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            onClick = onReadyTests
        ) {
            Text("${workspace.lesson.title} Hazır Testleri", fontSize = 17.sp)
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            onClick = onAllCards
        ) {
            Text("${workspace.lesson.title} Kartları", fontSize = 17.sp)
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ReadyTestsScreen(
    lesson: Lesson,
    questionSets: List<QuestionSet>,
    onStartSet: (QuestionSet) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("${lesson.emoji} ${lesson.title} Hazır Testleri", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        if (questionSets.isEmpty()) {
            EmptyInfoBox("Bu derse ait hazır test henüz yok.")
        } else {
            questionSets.forEach { set ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(onClick = {}, label = { Text(set.lesson.title) })
                        Text(set.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(set.description)
                        Text("Soru sayısı: ${set.questions.size}", fontWeight = FontWeight.Bold)

                        Button(
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            onClick = { onStartSet(set) }
                        ) {
                            Text("Bu Testi Çöz")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudyScreen(
    cards: List<FlashCard>,
    engine: FlashCardEngine,
    studyMode: StudyMode,
    selectedLesson: Lesson?,
    selectedQuestionSet: QuestionSet?,
    onUpdateCard: (FlashCard) -> Unit,
    onSessionFinished: (StudySession) -> Unit,
    onBackHome: () -> Unit
) {
    var currentIndex by remember(studyMode, selectedLesson, selectedQuestionSet?.id) { mutableIntStateOf(0) }
    var selectedOptionId by remember(studyMode, selectedLesson, selectedQuestionSet?.id) { mutableStateOf<String?>(null) }
    var answerResult by remember(studyMode, selectedLesson, selectedQuestionSet?.id) { mutableStateOf<AnswerResult?>(null) }
    var correctCount by remember(studyMode, selectedLesson, selectedQuestionSet?.id) { mutableIntStateOf(0) }
    var wrongCount by remember(studyMode, selectedLesson, selectedQuestionSet?.id) { mutableIntStateOf(0) }
    var answeredCount by remember(studyMode, selectedLesson, selectedQuestionSet?.id) { mutableIntStateOf(0) }

    val studyCards = engine.getStudyCards(cards, studyMode, selectedLesson, selectedQuestionSet)
    val sessionTitle = when (studyMode) {
        StudyMode.LESSON_ALL -> "${selectedLesson?.title ?: "Ders"} Çalışması"
        StudyMode.WRONG_ONLY -> "${selectedLesson?.title ?: "Ders"} Yanlışları"
        StudyMode.QUESTION_SET -> selectedQuestionSet?.title ?: "Hazır Test"
    }

    if (studyCards.isEmpty()) {
        EmptyStudyScreen(
            message = if (studyMode == StudyMode.WRONG_ONLY) {
                "Bu derste yanlış kart kalmadı 🎉"
            } else {
                "Bu derste çalışılacak kart yok."
            },
            onBackHome = onBackHome
        )
        return
    }

    if (currentIndex >= studyCards.size) currentIndex = 0

    val currentCard = studyCards[currentIndex]

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            AssistChip(onClick = {}, label = { Text(sessionTitle) })
            Spacer(modifier = Modifier.width(8.dp))
            AssistChip(onClick = {}, label = { Text("${currentIndex + 1} / ${studyCards.size}") })
        }

        Text("Doğru: $correctCount | Yanlış: $wrongCount", fontWeight = FontWeight.Bold)

        FlashCardQuestionCard(
            card = currentCard,
            selectedOptionId = selectedOptionId,
            answerResult = answerResult,
            onSelectOption = { optionId ->
                if (answerResult == null) selectedOptionId = optionId
            },
            onCheckAnswer = {
                val selected = selectedOptionId
                if (selected != null) {
                    val result = engine.checkAnswer(currentCard, selected)
                    val updated = engine.applyAnswerResult(currentCard, result)

                    onUpdateCard(updated)
                    answerResult = result
                    answeredCount += 1

                    if (result.isCorrect) correctCount += 1 else wrongCount += 1
                }
            },
            onNextCard = {
                val isLast = currentIndex + 1 >= studyCards.size

                if (isLast) {
                    onSessionFinished(
                        StudySession(
                            title = sessionTitle,
                            lesson = selectedLesson,
                            cards = studyCards,
                            correctCount = correctCount,
                            wrongCount = wrongCount,
                            answeredCount = answeredCount
                        )
                    )
                } else {
                    selectedOptionId = null
                    answerResult = null
                    currentIndex += 1
                }
            }
        )
    }
}

@Composable
fun SessionResultScreen(
    session: StudySession?,
    onBackToLesson: () -> Unit,
    onStudyWrongs: () -> Unit
) {
    if (session == null) {
        EmptyInfoBox("Sonuç bulunamadı.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Test Tamamlandı 🎉", fontSize = 26.sp, fontWeight = FontWeight.Bold)

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(session.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Toplam Soru: ${session.totalCount()}")
                Text("Cevaplanan: ${session.answeredCount}")
                Text("Doğru: ${session.correctCount}")
                Text("Yanlış: ${session.wrongCount}")
                Text("Başarı: %${session.successRate()}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = onStudyWrongs
        ) {
            Text("Bu Dersin Yanlışlarını Çöz")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = onBackToLesson
        ) {
            Text("Ders Sayfasına Dön")
        }
    }
}

@Composable
fun EmptyStudyScreen(message: String, onBackHome: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onBackHome) {
                Text("Ders Sayfasına Dön")
            }
        }
    }
}

@Composable
fun EmptyInfoBox(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(message, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FlashCardQuestionCard(
    card: FlashCard,
    selectedOptionId: String?,
    answerResult: AnswerResult?,
    onSelectOption: (String) -> Unit,
    onCheckAnswer: () -> Unit,
    onNextCard: () -> Unit
) {
    val result = answerResult

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(card.lesson.title) })
                AssistChip(onClick = {}, label = { Text(card.difficulty.title) })
            }

            Text(card.topic, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(card.question, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)

            HorizontalDivider()

            card.options.forEach { option ->
                val isSelected = selectedOptionId == option.id
                val isCorrectOption = result != null && option.id == card.correctOptionId
                val isWrongSelected = result != null && isSelected && option.id != card.correctOptionId

                val label = when {
                    isCorrectOption -> "✅ ${option.label}) ${option.text}"
                    isWrongSelected -> "❌ ${option.label}) ${option.text}"
                    isSelected -> "➤ ${option.label}) ${option.text}"
                    else -> "${option.label}) ${option.text}"
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    onClick = { onSelectOption(option.id) }
                ) {
                    Text(label)
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = selectedOptionId != null && result == null,
                onClick = onCheckAnswer
            ) {
                Text("Cevabı Kontrol Et", fontSize = 16.sp)
            }

            if (result != null) {
                ResultBox(result = result, onNextCard = onNextCard)
            }
        }
    }
}

@Composable
fun ResultBox(result: AnswerResult, onNextCard: () -> Unit) {
    val containerColor = if (result.isCorrect) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val title = if (result.isCorrect) "Doğru ✅" else "Yanlış ❌"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)

            if (!result.isCorrect) {
                Text("Senin cevabın: ${result.selectedOptionLabel}) ${result.selectedOptionText}", fontWeight = FontWeight.Bold)
            }

            Text("Doğru cevap: ${result.correctOptionLabel}) ${result.correctOptionText}", fontWeight = FontWeight.Bold)
            Text("Not: ${result.explanation}")

            if (result.movedToWrongList) {
                Text("Bu kart Yanlışlar bölümüne eklendi.", fontWeight = FontWeight.Bold)
            }

            if (result.removedFromWrongList) {
                Text("Bu kart üst üste 2 kez doğru yapıldı ve yanlışlardan çıkarıldı.", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                modifier = Modifier.fillMaxWidth().height(54.dp),
                onClick = onNextCard
            ) {
                Text("Sonraki Kart")
            }
        }
    }
}

@Composable
fun CreateCardScreen(
    lesson: Lesson,
    onSave: (FlashCard) -> Unit,
    onCancel: () -> Unit
) {
    var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var topic by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var optionA by remember { mutableStateOf("") }
    var optionB by remember { mutableStateOf("") }
    var optionC by remember { mutableStateOf("") }
    var optionD by remember { mutableStateOf("") }
    var optionE by remember { mutableStateOf("") }
    var correctOptionId by remember { mutableStateOf("B") }
    var explanation by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Yeni ${lesson.title} Kartı", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Bu kart otomatik olarak ${lesson.title} dersine eklenecek.")

        Text("Zorluk")
        DifficultySelector(selectedDifficulty = difficulty, onSelected = { difficulty = it })

        AppTextField(topic, { topic = it }, "Konu")
        AppTextField(question, { question = it }, "Soru", minLines = 3)

        AppTextField(optionA, { optionA = it }, "A şıkkı")
        AppTextField(optionB, { optionB = it }, "B şıkkı")
        AppTextField(optionC, { optionC = it }, "C şıkkı")
        AppTextField(optionD, { optionD = it }, "D şıkkı")
        AppTextField(optionE, { optionE = it }, "E şıkkı")

        Text("Doğru Şık")
        CorrectOptionSelector(selectedOptionId = correctOptionId, onSelected = { correctOptionId = it })

        AppTextField(explanation, { explanation = it }, "Açıklama / Not", minLines = 3)

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }

        Button(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            onClick = {
                val options = listOf(optionA, optionB, optionC, optionD, optionE)
                val uniqueOptions = options.map { it.trim().lowercase() }.distinct()
                val isValid = topic.isNotBlank() &&
                    question.isNotBlank() &&
                    explanation.isNotBlank() &&
                    options.all { it.isNotBlank() }

                if (!isValid) {
                    errorMessage = "Tüm alanları doldurmalısın."
                    return@Button
                }

                if (uniqueOptions.size != options.size) {
                    errorMessage = "Şıklar birbirinden farklı olmalı."
                    return@Button
                }

                val card = FlashCard(
                    id = "custom_${UUID.randomUUID()}",
                    lesson = lesson,
                    topic = topic.trim(),
                    question = question.trim(),
                    options = listOf(
                        AnswerOption("A", "A", optionA.trim()),
                        AnswerOption("B", "B", optionB.trim()),
                        AnswerOption("C", "C", optionC.trim()),
                        AnswerOption("D", "D", optionD.trim()),
                        AnswerOption("E", "E", optionE.trim())
                    ),
                    correctOptionId = correctOptionId,
                    explanation = explanation.trim(),
                    difficulty = difficulty
                )

                onSave(card)
            }
        ) {
            Text("Kartı Kaydet", fontSize = 17.sp)
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = onCancel
        ) {
            Text("Vazgeç")
        }
    }
}

@Composable
fun DifficultySelector(selectedDifficulty: Difficulty, onSelected: (Difficulty) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedDifficulty == Difficulty.EASY, onClick = { onSelected(Difficulty.EASY) }, label = { Text(Difficulty.EASY.title) })
            FilterChip(selected = selectedDifficulty == Difficulty.MEDIUM, onClick = { onSelected(Difficulty.MEDIUM) }, label = { Text(Difficulty.MEDIUM.title) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedDifficulty == Difficulty.HARD, onClick = { onSelected(Difficulty.HARD) }, label = { Text(Difficulty.HARD.title) })
            FilterChip(selected = selectedDifficulty == Difficulty.VERY_HARD, onClick = { onSelected(Difficulty.VERY_HARD) }, label = { Text(Difficulty.VERY_HARD.title) })
        }
    }
}

@Composable
fun CorrectOptionSelector(selectedOptionId: String, onSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("A", "B", "C", "D", "E").forEach { label ->
            FilterChip(selected = selectedOptionId == label, onClick = { onSelected(label) }, label = { Text(label) })
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    minLines: Int = 1
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = minLines
    )
}

@Composable
fun WrongCardsScreen(
    lesson: Lesson,
    wrongCards: List<FlashCard>,
    onStudyWrongCards: () -> Unit,
    onRemoveFromWrongs: (FlashCard) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("${lesson.emoji} ${lesson.title} Yanlışları", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        if (wrongCards.isEmpty()) {
            EmptyInfoBox("Bu derste yanlış kart yok 🎉")
        } else {
            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                onClick = onStudyWrongCards
            ) {
                Text("Bu Dersin Yanlışlarını Tekrar Çöz")
            }

            wrongCards.forEach { card ->
                WrongCardItem(card = card, onRemoveFromWrongs = { onRemoveFromWrongs(card) })
            }
        }
    }
}

@Composable
fun WrongCardItem(card: FlashCard, onRemoveFromWrongs: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${card.lesson.title} / ${card.topic}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(card.question, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Doğru cevap: ${card.correctOption().label}) ${card.correctOption().text}", fontWeight = FontWeight.Bold)
            Text("Not: ${card.explanation}")
            Text("Yanlış sayısı: ${card.progress.wrongCount} | Üst üste doğru: ${card.progress.consecutiveCorrectCount}")

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRemoveFromWrongs
            ) {
                Text("Yanlıştan Çıkar")
            }
        }
    }
}

@Composable
fun AllCardsScreen(lesson: Lesson, cards: List<FlashCard>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${lesson.emoji} ${lesson.title} Kartları", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        if (cards.isEmpty()) {
            EmptyInfoBox("Bu derse ait kart yok.")
        } else {
            cards.forEach { card ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = {}, label = { Text(card.lesson.title) })
                            AssistChip(onClick = {}, label = { Text(card.progress.status.title) })
                        }

                        Text(card.topic, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(card.question, fontWeight = FontWeight.Bold)
                        Text("Doğru: ${card.progress.correctCount} | Yanlış: ${card.progress.wrongCount}")
                    }
                }
            }
        }
    }
}
