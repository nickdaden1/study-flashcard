package com.study.flashcard

import android.app.Application
import com.study.flashcard.di.AppContainer

/** Application class. Mọi dependency lấy qua [container] (manual DI). */
class StudyApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
