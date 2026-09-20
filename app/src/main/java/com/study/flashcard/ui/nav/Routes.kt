package com.study.flashcard.ui.nav

object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val STUDY = "study/{deckId}"
    const val EXAM = "exam/{deckId}"

    fun study(deckId: Long) = "study/$deckId"
    fun exam(deckId: Long) = "exam/$deckId"
}
