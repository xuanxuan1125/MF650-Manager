package com.mf650.manager

import android.app.Application
import com.mf650.manager.data.repository.Mf650Repository
import com.mf650.manager.data.security.SecureCredentialStorage

class MF650Application : Application() {

    lateinit var credentialStorage: SecureCredentialStorage
        private set

    lateinit var repository: Mf650Repository
        private set

    override fun onCreate() {
        super.onCreate()
        credentialStorage = SecureCredentialStorage(this)
        repository = Mf650Repository(credentialStorage)
    }
}
