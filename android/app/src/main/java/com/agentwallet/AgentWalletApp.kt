package com.agentwallet

import android.app.Application

class AgentWalletApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AgentWalletApp
            private set
    }
}
