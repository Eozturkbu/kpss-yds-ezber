package com.yds.almadefteri.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.yds.almadefteri.data.JsonFlashCardRepository
import com.yds.almadefteri.domain.engine.FlashCardEngine
import com.yds.almadefteri.domain.model.AppScreen
import com.yds.almadefteri.presentation.screens.EzberAppScaffold
import com.yds.almadefteri.presentation.screens.LessonSelectionScreen
import com.yds.almadefteri.presentation.screens.LessonHomeScreen
import com.yds.almadefteri.presentation.screens.ReadyTestsScreen
import com.yds.almadefteri.presentation.screens.StudyScreen
import com.yds.almadefteri.presentation.screens.SessionResultScreen
import com.yds.almadefteri.presentation.screens.CreateCardScreen
import com.yds.almadefteri.presentation.screens.WrongCardsScreen
import com.yds.almadefteri.presentation.screens.AllCardsScreen
import com.yds.almadefteri.data.QuestionBank

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    val viewModel = remember {
                        EzberViewModel(
                            repository = JsonFlashCardRepository(context),
                            engine = FlashCardEngine()
                        )
                    }

                    EzberKocuApp(viewModel = viewModel)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun EzberKocuApp(viewModel: EzberViewModel) {
    val uiState = viewModel.uiState

    BackHandler(enabled = uiState.screen != AppScreen.HOME) {
        viewModel.goBackSmart()
    }

    EzberAppScaffold(
        screen = uiState.screen,
        selectedLesson = uiState.selectedLesson,
        onBack = { viewModel.goBackSmart() }
    ) {
        when (uiState.screen) {
            AppScreen.HOME -> LessonSelectionScreen(
                cards = uiState.cards,
                engine = viewModel.engine,
                lastLesson = uiState.selectedLesson,
                onSelectLesson = { viewModel.selectLesson(it) }
            )

            AppScreen.LESSON_HOME -> {
                val lesson = uiState.selectedLesson
                if (lesson == null) {
                    viewModel.goHome()
                } else {
                    val workspace = viewModel.engine.createWorkspace(
                        lesson = lesson,
                        allCards = uiState.cards,
                        allSets = QuestionBank.sets
                    )

                    LessonHomeScreen(
                        workspace = workspace,
                        onStartStudy = { viewModel.startLessonStudy() },
                        onCreateCard = { viewModel.openCreateCard() },
                        onWrongCards = { viewModel.openWrongCards() },
                        onAllCards = { viewModel.openAllCards() },
                        onReadyTests = { viewModel.openReadyTests() }
                    )
                }
            }

            AppScreen.READY_TESTS -> {
                val lesson = uiState.selectedLesson
                if (lesson == null) {
                    viewModel.goHome()
                } else {
                    ReadyTestsScreen(
                        lesson = lesson,
                        questionSets = viewModel.questionSetsForSelectedLesson(),
                        onStartSet = { viewModel.startQuestionSet(it) }
                    )
                }
            }

            AppScreen.STUDY -> StudyScreen(
                cards = uiState.cards,
                engine = viewModel.engine,
                studyMode = uiState.studyMode,
                selectedLesson = uiState.selectedLesson,
                selectedQuestionSet = uiState.selectedQuestionSet,
                onUpdateCard = { viewModel.updateCard(it) },
                onSessionFinished = { viewModel.finishSession(it) },
                onBackHome = { viewModel.backToLesson() }
            )

            AppScreen.SESSION_RESULT -> SessionResultScreen(
                session = uiState.lastSession,
                onBackToLesson = { viewModel.backToLesson() },
                onStudyWrongs = { viewModel.startWrongStudy() }
            )

            AppScreen.CREATE_CARD -> {
                val lesson = uiState.selectedLesson
                if (lesson == null) {
                    viewModel.goHome()
                } else {
                    CreateCardScreen(
                        lesson = lesson,
                        onSave = { viewModel.addCard(it) },
                        onCancel = { viewModel.backToLesson() }
                    )
                }
            }

            AppScreen.WRONG_CARDS -> {
                val lesson = uiState.selectedLesson
                if (lesson == null) {
                    viewModel.goHome()
                } else {
                    val wrongCards = uiState.cards.filter {
                        it.lesson == lesson &&
                            it.progress.status == com.yds.almadefteri.domain.model.CardStatus.WRONG
                    }

                    WrongCardsScreen(
                        lesson = lesson,
                        wrongCards = wrongCards,
                        onStudyWrongCards = { viewModel.startWrongStudy() },
                        onRemoveFromWrongs = { viewModel.removeFromWrongs(it) }
                    )
                }
            }

            AppScreen.ALL_CARDS -> {
                val lesson = uiState.selectedLesson
                if (lesson == null) {
                    viewModel.goHome()
                } else {
                    AllCardsScreen(
                        lesson = lesson,
                        cards = uiState.cards.filter { it.lesson == lesson }
                    )
                }
            }
        }
    }
}
