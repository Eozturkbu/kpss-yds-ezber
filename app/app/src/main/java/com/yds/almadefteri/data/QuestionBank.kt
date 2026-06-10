package com.yds.almadefteri.data

import com.yds.almadefteri.domain.model.AnswerOption
import com.yds.almadefteri.domain.model.Difficulty
import com.yds.almadefteri.domain.model.FlashCard
import com.yds.almadefteri.domain.model.Lesson
import com.yds.almadefteri.domain.model.QuestionSet

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
