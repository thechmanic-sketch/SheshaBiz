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
        // Always started, not gated on isLoggedIn — SyncManager.sync() itself is a no-op for
        // an offline-only user, so this is harmless for them and just means a just-logged-in
        // user doesn't need an app restart before background sync kicks in.
        container.syncScheduler.start()
    }
}

fun Context.appContainer(): AppContainer =
    (applicationContext as QuickQuoteApp).container
