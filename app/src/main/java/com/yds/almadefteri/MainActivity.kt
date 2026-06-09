package com.yds.almadefteri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

enum class Lesson(val title: String) {
HISTORY("Tarih"),
CITIZENSHIP("Vatandaşlık"),
GEOGRAPHY("Coğrafya"),
YDS("YDS"),
GENERAL("Genel")
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
STUDY,
CREATE_CARD,
WRONG_CARDS,
ALL_CARDS
}

enum class StudyMode {
ALL,
WRONG_ONLY
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
fun correctOption(): AnswerOption {
return options.first { it.id == correctOptionId }
}
}

data class AnswerResult(
val cardId: String,
val question: String,
val selectedOptionLabel: String,
val selectedOptionText: String,
val correctOptionLabel: String,
val correctOptionText: String,
val explanation: String,
val isCorrect: Boolean,
val movedToWrongList: Boolean,
val removedFromWrongList: Boolean
)

class FlashCardEngine {

```
fun checkAnswer(
    card: FlashCard,
    selectedOptionId: String
): AnswerResult {
    val selected = card.options.first { it.id == selectedOptionId }
    val correct = card.correctOption()
    val isCorrect = selectedOptionId == card.correctOptionId
    val wasWrong = card.progress.status == CardStatus.WRONG

    val willBeRemovedFromWrongList =
        isCorrect && wasWrong && card.progress.consecutiveCorrectCount + 1 >= 2

    return AnswerResult(
        cardId = card.id,
        question = card.question,
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

fun applyAnswerResult(
    card: FlashCard,
    result: AnswerResult
): FlashCard {
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

fun getWrongCards(cards: List<FlashCard>): List<FlashCard> {
    return cards.filter { it.progress.status == CardStatus.WRONG }
}

fun getStudyCards(cards: List<FlashCard>, mode: StudyMode): List<FlashCard> {
    return when (mode) {
        StudyMode.ALL -> cards
        StudyMode.WRONG_ONLY -> getWrongCards(cards)
    }
}
```

}

class MainActivity : ComponentActivity() {
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContent {
MaterialTheme {
Surface(modifier = Modifier.fillMaxSize()) {
EzberKocuApp()
}
}
}
}
}

@Composable
fun EzberKocuApp() {
val engine = remember { FlashCardEngine() }

```
val cards = remember {
    mutableStateListOf(
        sampleCardAmasya(),
        sampleCardAtaturkIlkeleri(),
        sampleCardKanunTeklifi(),
        sampleCardKizilirmak(),
        sampleCardAlthough()
    )
}

var screen by remember { mutableStateOf(AppScreen.HOME) }
var studyMode by remember { mutableStateOf(StudyMode.ALL) }

fun updateCard(updated: FlashCard) {
    val index = cards.indexOfFirst { it.id == updated.id }
    if (index >= 0) {
        cards[index] = updated
    }
}

fun addCard(card: FlashCard) {
    cards.add(0, card)
    screen = AppScreen.HOME
}

AppScaffold(
    screen = screen,
    onBackHome = { screen = AppScreen.HOME }
) {
    when (screen) {
        AppScreen.HOME -> HomeScreen(
            cards = cards,
            engine = engine,
            onStartStudy = {
                studyMode = StudyMode.ALL
                screen = AppScreen.STUDY
            },
            onCreateCard = {
                screen = AppScreen.CREATE_CARD
            },
            onWrongCards = {
                screen = AppScreen.WRONG_CARDS
            },
            onAllCards = {
                screen = AppScreen.ALL_CARDS
            }
        )

        AppScreen.STUDY -> StudyScreen(
            cards = cards,
            engine = engine,
            studyMode = studyMode,
            onUpdateCard = ::updateCard,
            onBackHome = {
                screen = AppScreen.HOME
            }
        )

        AppScreen.CREATE_CARD -> CreateCardScreen(
            onSave = ::addCard,
            onCancel = {
                screen = AppScreen.HOME
            }
        )

        AppScreen.WRONG_CARDS -> WrongCardsScreen(
            wrongCards = engine.getWrongCards(cards),
            onStudyWrongCards = {
                studyMode = StudyMode.WRONG_ONLY
                screen = AppScreen.STUDY
            },
            onRemoveFromWrongs = { card ->
                updateCard(
                    card.copy(
                        progress = card.progress.copy(
                            status = CardStatus.REVIEW,
                            consecutiveCorrectCount = 0
                        )
                    )
                )
            }
        )

        AppScreen.ALL_CARDS -> AllCardsScreen(cards = cards)
    }
}
```

}

@Composable
fun AppScaffold(
screen: AppScreen,
onBackHome: () -> Unit,
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
text = "KPSS & YDS Flash Kart",
fontSize = 15.sp,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}

```
        if (screen != AppScreen.HOME) {
            TextButton(onClick = onBackHome) {
                Text("Ana Sayfa")
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))
    content()
}
```

}

@Composable
fun HomeScreen(
cards: List<FlashCard>,
engine: FlashCardEngine,
onStartStudy: () -> Unit,
onCreateCard: () -> Unit,
onWrongCards: () -> Unit,
onAllCards: () -> Unit
) {
val wrongCards = engine.getWrongCards(cards)
val masteredCount = cards.count { it.progress.status == CardStatus.MASTERED }
val learningCount = cards.count {
it.progress.status == CardStatus.LEARNING || it.progress.status == CardStatus.REVIEW
}

```
Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text(
        text = "Bugün neyi hatırlayacağız?",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(
            title = "Toplam Kart",
            value = cards.size.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Yanlış Kart",
            value = wrongCards.size.toString(),
            modifier = Modifier.weight(1f)
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(
            title = "Çalışılıyor",
            value = learningCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Öğrenildi",
            value = masteredCount.toString(),
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        onClick = onStartStudy
    ) {
        Text("Çalışmaya Başla", fontSize = 17.sp)
    }

    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        onClick = onCreateCard
    ) {
        Text("+ Flash Kart Oluştur", fontSize = 17.sp)
    }

    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        onClick = onWrongCards
    ) {
        Text("Yanlışlarım", fontSize = 17.sp)
    }

    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        onClick = onAllCards
    ) {
        Text("Kartlarım", fontSize = 17.sp)
    }
}
```

}

@Composable
fun StatCard(
title: String,
value: String,
modifier: Modifier = Modifier
) {
ElevatedCard(
modifier = modifier,
shape = RoundedCornerShape(18.dp)
) {
Column(modifier = Modifier.padding(16.dp)) {
Text(
text = title,
fontSize = 14.sp,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
Spacer(modifier = Modifier.height(6.dp))
Text(
text = value,
fontSize = 28.sp,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.primary
)
}
}
}

@Composable
fun StudyScreen(
cards: List<FlashCard>,
engine: FlashCardEngine,
studyMode: StudyMode,
onUpdateCard: (FlashCard) -> Unit,
onBackHome: () -> Unit
) {
var currentIndex by remember(studyMode) { mutableIntStateOf(0) }
var selectedOptionId by remember(studyMode) { mutableStateOf<String?>(null) }
var answerResult by remember(studyMode) { mutableStateOf<AnswerResult?>(null) }

```
val studyCards = engine.getStudyCards(cards, studyMode)

if (studyCards.isEmpty()) {
    EmptyStudyScreen(
        studyMode = studyMode,
        onBackHome = onBackHome
    )
    return
}

if (currentIndex >= studyCards.size) {
    currentIndex = 0
}

val currentCard = studyCards[currentIndex]

Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(modifier = Modifier.fillMaxWidth()) {
        AssistChip(
            onClick = {},
            label = {
                Text(
                    if (studyMode == StudyMode.WRONG_ONLY) {
                        "Yanlışları Çöz"
                    } else {
                        "Genel Çalışma"
                    }
                )
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        AssistChip(
            onClick = {},
            label = {
                Text("${currentIndex + 1} / ${studyCards.size}")
            }
        )
    }

    FlashCardQuestionCard(
        card = currentCard,
        selectedOptionId = selectedOptionId,
        answerResult = answerResult,
        onSelectOption = { optionId ->
            if (answerResult == null) {
                selectedOptionId = optionId
            }
        },
        onCheckAnswer = {
            val selected = selectedOptionId
            if (selected != null) {
                val result = engine.checkAnswer(currentCard, selected)
                val updated = engine.applyAnswerResult(currentCard, result)
                onUpdateCard(updated)
                answerResult = result
            }
        },
        onNextCard = {
            selectedOptionId = null
            answerResult = null
            currentIndex = if (currentIndex + 1 >= studyCards.size) {
                0
            } else {
                currentIndex + 1
            }
        }
    )
}
```

}

@Composable
fun EmptyStudyScreen(
studyMode: StudyMode,
onBackHome: () -> Unit
) {
Card(
modifier = Modifier.fillMaxWidth(),
colors = CardDefaults.cardColors(
containerColor = MaterialTheme.colorScheme.secondaryContainer
)
) {
Column(
modifier = Modifier.padding(20.dp),
verticalArrangement = Arrangement.spacedBy(12.dp)
) {
Text(
text = if (studyMode == StudyMode.WRONG_ONLY) {
"Yanlış kart kalmadı 🎉"
} else {
"Çalışılacak kart yok."
},
fontSize = 22.sp,
fontWeight = FontWeight.Bold
)

```
        Text(
            text = if (studyMode == StudyMode.WRONG_ONLY) {
                "Harika gidiyorsun. Yanlışlar bölümünü temizledin."
            } else {
                "Yeni flash kart oluşturarak başlayabilirsin."
            }
        )

        Button(onClick = onBackHome) {
            Text("Ana Sayfaya Dön")
        }
    }
}
```

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

```
ElevatedCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp)
) {
    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = {},
                label = { Text(card.lesson.title) }
            )
            AssistChip(
                onClick = {},
                label = { Text(card.difficulty.title) }
            )
        }

        Text(
            text = card.topic,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = card.question,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp
        )

        HorizontalDivider()

        card.options.forEach { option ->
            val isSelected = selectedOptionId == option.id
            val isCorrectOption = result != null && option.id == card.correctOptionId
            val isWrongSelected =
                result != null && isSelected && option.id != card.correctOptionId

            val label = when {
                isCorrectOption -> "✅ ${option.label}) ${option.text}"
                isWrongSelected -> "❌ ${option.label}) ${option.text}"
                isSelected -> "➤ ${option.label}) ${option.text}"
                else -> "${option.label}) ${option.text}"
            }

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = { onSelectOption(option.id) }
            ) {
                Text(text = label)
            }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedOptionId != null && result == null,
            onClick = onCheckAnswer
        ) {
            Text("Cevabı Kontrol Et", fontSize = 16.sp)
        }

        if (result != null) {
            ResultBox(
                result = result,
                onNextCard = onNextCard
            )
        }
    }
}
```

}

@Composable
fun ResultBox(
result: AnswerResult,
onNextCard: () -> Unit
) {
val containerColor = if (result.isCorrect) {
MaterialTheme.colorScheme.primaryContainer
} else {
MaterialTheme.colorScheme.errorContainer
}

```
val title = if (result.isCorrect) {
    "Doğru ✅"
} else {
    "Yanlış ❌"
}

Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    shape = RoundedCornerShape(18.dp)
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        if (!result.isCorrect) {
            Text(
                text = "Senin cevabın: ${result.selectedOptionLabel}) ${result.selectedOptionText}",
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Doğru cevap: ${result.correctOptionLabel}) ${result.correctOptionText}",
            fontWeight = FontWeight.Bold
        )

        Text(text = "Not: ${result.explanation}")

        if (result.movedToWrongList) {
            Text(
                text = "Bu kart Yanlışlar bölümüne eklendi.",
                fontWeight = FontWeight.Bold
            )
        }

        if (result.removedFromWrongList) {
            Text(
                text = "Bu kart üst üste 2 kez doğru yapıldı ve yanlışlardan çıkarıldı.",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            onClick = onNextCard
        ) {
            Text("Sonraki Kart")
        }
    }
}
```

}

@Composable
fun CreateCardScreen(
onSave: (FlashCard) -> Unit,
onCancel: () -> Unit
) {
var lesson by remember { mutableStateOf(Lesson.HISTORY) }
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

```
Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
        text = "Yeni Flash Kart",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )

    Text(text = "Ders")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Lesson.entries.forEach { item ->
            FilterChip(
                selected = lesson == item,
                onClick = { lesson = item },
                label = { Text(item.title) }
            )
        }
    }

    Text(text = "Zorluk")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Difficulty.entries.forEach { item ->
            FilterChip(
                selected = difficulty == item,
                onClick = { difficulty = item },
                label = { Text(item.title) }
            )
        }
    }

    AppTextField(
        value = topic,
        onValueChange = { topic = it },
        label = "Konu"
    )

    AppTextField(
        value = question,
        onValueChange = { question = it },
        label = "Soru",
        minLines = 3
    )

    AppTextField(value = optionA, onValueChange = { optionA = it }, label = "A şıkkı")
    AppTextField(value = optionB, onValueChange = { optionB = it }, label = "B şıkkı")
    AppTextField(value = optionC, onValueChange = { optionC = it }, label = "C şıkkı")
    AppTextField(value = optionD, onValueChange = { optionD = it }, label = "D şıkkı")
    AppTextField(value = optionE, onValueChange = { optionE = it }, label = "E şıkkı")

    Text(text = "Doğru Şık")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("A", "B", "C", "D", "E").forEach { label ->
            FilterChip(
                selected = correctOptionId == label,
                onClick = { correctOptionId = label },
                label = { Text(label) }
            )
        }
    }

    AppTextField(
        value = explanation,
        onValueChange = { explanation = it },
        label = "Açıklama / Not",
        minLines = 3
    )

    errorMessage?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        onClick = {
            val options = listOf(optionA, optionB, optionC, optionD, optionE)
            val isValid = topic.isNotBlank() &&
                question.isNotBlank() &&
                explanation.isNotBlank() &&
                options.all { it.isNotBlank() }

            if (!isValid) {
                errorMessage = "Tüm alanları doldurmalısın."
                return@Button
            }

            val card = FlashCard(
                id = UUID.randomUUID().toString(),
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
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        onClick = onCancel
    ) {
        Text("Vazgeç")
    }
}
```

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
wrongCards: List<FlashCard>,
onStudyWrongCards: () -> Unit,
onRemoveFromWrongs: (FlashCard) -> Unit
) {
Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
Text(
text = "Yanlışlarım",
fontSize = 24.sp,
fontWeight = FontWeight.Bold
)

```
    if (wrongCards.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Yanlış kart yok 🎉",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Şimdilik temizsin. Harika gidiyorsun.")
            }
        }
    } else {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            onClick = onStudyWrongCards
        ) {
            Text("Yanlışları Tekrar Çöz")
        }

        wrongCards.forEach { card ->
            WrongCardItem(
                card = card,
                onRemoveFromWrongs = { onRemoveFromWrongs(card) }
            )
        }
    }
}
```

}

@Composable
fun WrongCardItem(
card: FlashCard,
onRemoveFromWrongs: () -> Unit
) {
ElevatedCard(
modifier = Modifier.fillMaxWidth(),
shape = RoundedCornerShape(18.dp)
) {
Column(
modifier = Modifier.padding(16.dp),
verticalArrangement = Arrangement.spacedBy(8.dp)
) {
Text(
text = "${card.lesson.title} / ${card.topic}",
color = MaterialTheme.colorScheme.primary,
fontWeight = FontWeight.Bold
)

```
        Text(
            text = card.question,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Doğru cevap: ${card.correctOption().label}) ${card.correctOption().text}",
            fontWeight = FontWeight.Bold
        )

        Text(text = "Not: ${card.explanation}")

        Text(
            text = "Yanlış sayısı: ${card.progress.wrongCount} | Üst üste doğru: ${card.progress.consecutiveCorrectCount}"
        )

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRemoveFromWrongs
        ) {
            Text("Yanlıştan Çıkar")
        }
    }
}
```

}

@Composable
fun AllCardsScreen(cards: List<FlashCard>) {
Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
Text(
text = "Kartlarım",
fontSize = 24.sp,
fontWeight = FontWeight.Bold
)

```
    cards.forEach { card ->
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(card.lesson.title) })
                    AssistChip(onClick = {}, label = { Text(card.progress.status.title) })
                }

                Text(
                    text = card.topic,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = card.question,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Doğru: ${card.progress.correctCount} | Yanlış: ${card.progress.wrongCount}"
                )
            }
        }
    }
}
```

}

fun sampleCardAmasya(): FlashCard {
return FlashCard(
id = "sample-amasya",
lesson = Lesson.HISTORY,
topic = "Amasya Genelgesi",
question = "Amasya Genelgesi'nin KPSS açısından en önemli özelliği nedir?",
options = listOf(
AnswerOption("A", "A", "Misakımillî ilk kez kabul edilmiştir."),
AnswerOption("B", "B", "Milli Mücadele'nin gerekçesi, amacı ve yöntemi açıklanmıştır."),
AnswerOption("C", "C", "Saltanat kaldırılmıştır."),
AnswerOption("D", "D", "İlk TBMM açılmıştır."),
AnswerOption("E", "E", "Lozan Antlaşması imzalanmıştır.")
),
correctOptionId = "B",
explanation = "Amasya Genelgesi = gerekçe + amaç + yöntem.",
difficulty = Difficulty.MEDIUM
)
}

fun sampleCardAtaturkIlkeleri(): FlashCard {
return FlashCard(
id = "sample-ataturk-ilkeleri",
lesson = Lesson.HISTORY,
topic = "Atatürk İlkeleri",
question = "Atatürk ilkeleri hangi yıl anayasaya girmiştir?",
options = listOf(
AnswerOption("A", "A", "1921"),
AnswerOption("B", "B", "1924"),
AnswerOption("C", "C", "1928"),
AnswerOption("D", "D", "1934"),
AnswerOption("E", "E", "1937")
),
correctOptionId = "E",
explanation = "Atatürk ilkeleri 1937 yılında anayasaya girmiştir.",
difficulty = Difficulty.EASY
)
}

fun sampleCardKanunTeklifi(): FlashCard {
return FlashCard(
id = "sample-kanun-teklifi",
lesson = Lesson.CITIZENSHIP,
topic = "Yasama",
question = "Kanun teklif etmeye kim yetkilidir?",
options = listOf(
AnswerOption("A", "A", "Cumhurbaşkanı"),
AnswerOption("B", "B", "Bakanlar"),
AnswerOption("C", "C", "Milletvekilleri"),
AnswerOption("D", "D", "Anayasa Mahkemesi"),
AnswerOption("E", "E", "Danıştay")
),
correctOptionId = "C",
explanation = "Kanun teklif etmeye milletvekilleri yetkilidir.",
difficulty = Difficulty.MEDIUM
)
}

fun sampleCardKizilirmak(): FlashCard {
return FlashCard(
id = "sample-kizilirmak",
lesson = Lesson.GEOGRAPHY,
topic = "Akarsular",
question = "Türkiye sınırları içinde akan en uzun akarsu hangisidir?",
options = listOf(
AnswerOption("A", "A", "Fırat"),
AnswerOption("B", "B", "Dicle"),
AnswerOption("C", "C", "Sakarya"),
AnswerOption("D", "D", "Kızılırmak"),
AnswerOption("E", "E", "Yeşilırmak")
),
correctOptionId = "D",
explanation = "Kızılırmak tamamen Türkiye sınırları içinde akan en uzun akarsudur.",
difficulty = Difficulty.EASY
)
}

fun sampleCardAlthough(): FlashCard {
return FlashCard(
id = "sample-although",
lesson = Lesson.YDS,
topic = "Bağlaçlar",
question = "'Although' kelimesinin anlamı nedir?",
options = listOf(
AnswerOption("A", "A", "Çünkü"),
AnswerOption("B", "B", "Bu yüzden"),
AnswerOption("C", "C", "-mesine rağmen"),
AnswerOption("D", "D", "Ayrıca"),
AnswerOption("E", "E", "Bununla birlikte")
),
correctOptionId = "C",
explanation = "Although = -mesine rağmen. Even though ile yakın anlamdadır.",
difficulty = Difficulty.EASY
)
}
