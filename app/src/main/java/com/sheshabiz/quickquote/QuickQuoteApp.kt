package com.sheshabiz.quickquote

import android.app.Application
import android.content.Context
import com.sheshabiz.quickquote.di.AppContainer

class QuickQuoteApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (container.preferences.overdueRemindersEnabled.value) {
            container.reminderScheduler.start()
        }
    }
}

fun Context.appContainer(): AppContainer =
    (applicationContext as QuickQuoteApp).container
