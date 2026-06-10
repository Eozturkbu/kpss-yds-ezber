package com.yds.almadefteri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
    fun correctOption(): AnswerOption {
        return options.first { it.id == correctOptionId }
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
        val selected = card.options.first { it.id == selectedOptionId }
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
                else allCards.filter { it.lesson == selectedLesson }
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

object QuestionBank {
    val sets: List<QuestionSet> = listOf(
        QuestionSet(
            id = "kpss_tarih_temel",
            title = "KPSS Tarih Temel Test",
            lesson = Lesson.HISTORY,
            description = "Kurtuluş Savaşı ve inkılap tarihi için başlangıç testi.",
            questions = listOf(
                sampleCardAmasya(),
                sampleCardAtaturkIlkeleri(),
                FlashCard(
                    id = "tarih_kongre_1",
                    lesson = Lesson.HISTORY,
                    topic = "Erzurum Kongresi",
                    question = "Erzurum Kongresi'nin en önemli ulusal özelliği aşağıdakilerden hangisidir?",
                    options = listOf(
                        AnswerOption("A", "A", "Toplanış bakımından bölgesel, kararları bakımından ulusaldır."),
                        AnswerOption("B", "B", "İlk kez manda ve himaye kabul edilmiştir."),
                        AnswerOption("C", "C", "Saltanat kaldırılmıştır."),
                        AnswerOption("D", "D", "Misakımillî ilan edilmiştir."),
                        AnswerOption("E", "E", "TBMM açılmıştır.")
                    ),
                    correctOptionId = "A",
                    explanation = "Erzurum Kongresi toplanış amacı bakımından bölgesel, aldığı kararlar bakımından ulusaldır.",
                    difficulty = Difficulty.MEDIUM
                )
            )
        ),
        QuestionSet(
            id = "vatandaslik_yasama",
            title = "Vatandaşlık Yasama Testi",
            lesson = Lesson.CITIZENSHIP,
            description = "Anayasa ve yasama konuları için kısa test.",
            questions = listOf(
                sampleCardKanunTeklifi(),
                FlashCard(
                    id = "vat_yasama_2",
                    lesson = Lesson.CITIZENSHIP,
                    topic = "TBMM",
                    question = "TBMM seçimleri kural olarak kaç yılda bir yapılır?",
                    options = listOf(
                        AnswerOption("A", "A", "3"),
                        AnswerOption("B", "B", "4"),
                        AnswerOption("C", "C", "5"),
                        AnswerOption("D", "D", "6"),
                        AnswerOption("E", "E", "7")
                    ),
                    correctOptionId = "C",
                    explanation = "TBMM seçimleri kural olarak 5 yılda bir yapılır.",
                    difficulty = Difficulty.EASY
                )
            )
        ),
        QuestionSet(
            id = "cografya_temel",
            title = "Coğrafya Temel Test",
            lesson = Lesson.GEOGRAPHY,
            description = "Türkiye coğrafyası için temel tekrar.",
            questions = listOf(
                sampleCardKizilirmak()
            )
        ),
        QuestionSet(
            id = "yds_baglaclar",
            title = "YDS Bağlaçlar",
            lesson = Lesson.YDS,
            description = "YDS için temel bağlaç anlamları.",
            questions = listOf(
                sampleCardAlthough(),
                FlashCard(
                    id = "yds_however",
                    lesson = Lesson.YDS,
                    topic = "Bağlaçlar",
                    question = "'However' kelimesinin en yakın anlamı hangisidir?",
                    options = listOf(
                        AnswerOption("A", "A", "Bu nedenle"),
                        AnswerOption("B", "B", "Fakat / ancak"),
                        AnswerOption("C", "C", "Çünkü"),
                        AnswerOption("D", "D", "Ek olarak"),
                        AnswerOption("E", "E", "Sonuç olarak")
                    ),
                    correctOptionId = "B",
                    explanation = "However = fakat, ancak, bununla birlikte.",
                    difficulty = Difficulty.EASY
                )
            )
        )
    )

    fun allCards(): List<FlashCard> {
        return sets.flatMap { it.questions }.distinctBy { it.id } + listOf(
            FlashCard(
                id = "general_sample_1",
                lesson = Lesson.GENERAL,
                topic = "Çalışma Stratejisi",
                question = "Yanlış yapılan kartların tekrar çalışılması hangi öğrenme tekniğine daha yakındır?",
                options = listOf(
                    AnswerOption("A", "A", "Pasif okuma"),
                    AnswerOption("B", "B", "Aktif hatırlama"),
                    AnswerOption("C", "C", "Sadece özet çıkarma"),
                    AnswerOption("D", "D", "Rastgele tekrar"),
                    AnswerOption("E", "E", "Ezbere bakmadan geçme")
                ),
                correctOptionId = "B",
                explanation = "Yanlışlardan tekrar çözmek aktif hatırlama ve pekiştirme mantığına yakındır.",
                difficulty = Difficulty.EASY
            )
        )
    }
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

    val cards = remember {
        mutableStateListOf<FlashCard>().apply {
            addAll(QuestionBank.allCards())
        }
    }

    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    var studyMode by remember { mutableStateOf(StudyMode.LESSON_ALL) }
    var selectedQuestionSet by remember { mutableStateOf<QuestionSet?>(null) }
    var lastSession by remember { mutableStateOf<StudySession?>(null) }

    BackHandler(enabled = screen != AppScreen.HOME) {
        screen = if (screen == AppScreen.LESSON_HOME || selectedLesson == null) {
            AppScreen.HOME
        } else {
            AppScreen.LESSON_HOME
        }
    }

    fun updateCard(updated: FlashCard) {
        val index = cards.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            cards[index] = updated
        } else {
            cards.add(updated)
        }
    }

    fun addCard(card: FlashCard) {
        cards.add(0, card)
        screen = AppScreen.LESSON_HOME
    }

    AppScaffold(
        screen = screen,
        selectedLesson = selectedLesson,
        onBackHome = {
            screen = if (screen == AppScreen.LESSON_HOME || selectedLesson == null) {
                AppScreen.HOME
            } else {
                AppScreen.LESSON_HOME
            }
        }
    ) {
        when (screen) {
            AppScreen.HOME -> LessonSelectionScreen(
                cards = cards,
                engine = engine,
                onSelectLesson = { lesson ->
                    selectedLesson = lesson
                    selectedQuestionSet = null
                    screen = AppScreen.LESSON_HOME
                }
            )

            AppScreen.LESSON_HOME -> {
                val lesson = selectedLesson
                if (lesson == null) {
                    screen = AppScreen.HOME
                } else {
                    val workspace = engine.createWorkspace(
                        lesson = lesson,
                        allCards = cards,
                        allSets = QuestionBank.sets
                    )

                    LessonHomeScreen(
                        workspace = workspace,
                        onStartStudy = {
                            studyMode = StudyMode.LESSON_ALL
                            selectedQuestionSet = null
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
                        },
                        onReadyTests = {
                            screen = AppScreen.READY_TESTS
                        }
                    )
                }
            }

            AppScreen.READY_TESTS -> {
                val lesson = selectedLesson
                if (lesson == null) {
                    screen = AppScreen.HOME
                } else {
                    ReadyTestsScreen(
                        lesson = lesson,
                        questionSets = QuestionBank.sets.filter { it.lesson == lesson },
                        onStartSet = { set ->
                            selectedQuestionSet = set
                            studyMode = StudyMode.QUESTION_SET
                            screen = AppScreen.STUDY
                        }
                    )
                }
            }

            AppScreen.STUDY -> StudyScreen(
                cards = cards,
                engine = engine,
                studyMode = studyMode,
                selectedLesson = selectedLesson,
                selectedQuestionSet = selectedQuestionSet,
                onUpdateCard = { updateCard(it) },
                onSessionFinished = { session ->
                    lastSession = session
                    screen = AppScreen.SESSION_RESULT
                },
                onBackHome = {
                    screen = AppScreen.LESSON_HOME
                }
            )

            AppScreen.SESSION_RESULT -> SessionResultScreen(
                session = lastSession,
                onBackToLesson = {
                    screen = AppScreen.LESSON_HOME
                },
                onStudyWrongs = {
                    studyMode = StudyMode.WRONG_ONLY
                    selectedQuestionSet = null
                    screen = AppScreen.STUDY
                }
            )

            AppScreen.CREATE_CARD -> {
                val lesson = selectedLesson
                if (lesson == null) {
                    screen = AppScreen.HOME
                } else {
                    CreateCardScreen(
                        lesson = lesson,
                        onSave = { addCard(it) },
                        onCancel = {
                            screen = AppScreen.LESSON_HOME
                        }
                    )
                }
            }

            AppScreen.WRONG_CARDS -> {
                val lesson = selectedLesson
                if (lesson == null) {
                    screen = AppScreen.HOME
                } else {
                    val wrongCards = cards.filter {
                        it.lesson == lesson && it.progress.status == CardStatus.WRONG
                    }

                    WrongCardsScreen(
                        lesson = lesson,
                        wrongCards = wrongCards,
                        onStudyWrongCards = {
                            studyMode = StudyMode.WRONG_ONLY
                            selectedQuestionSet = null
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
                }
            }

            AppScreen.ALL_CARDS -> {
                val lesson = selectedLesson
                if (lesson == null) {
                    screen = AppScreen.HOME
                } else {
                    AllCardsScreen(
                        lesson = lesson,
                        cards = cards.filter { it.lesson == lesson }
                    )
                }
            }
        }
    }
}

@Composable
fun AppScaffold(
    screen: AppScreen,
    selectedLesson: Lesson?,
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
                    text = selectedLesson?.let { "${it.emoji} ${it.title} Çalışma Alanı" }
                        ?: "KPSS & YDS Flash Kart",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (screen != AppScreen.HOME) {
                TextButton(onClick = onBackHome) {
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
    onSelectLesson: (Lesson) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Hangi dersi çalışacağız?",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Lesson.values().forEach { lesson ->
            val workspace = engine.createWorkspace(
                lesson = lesson,
                allCards = cards,
                allSets = QuestionBank.sets
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
