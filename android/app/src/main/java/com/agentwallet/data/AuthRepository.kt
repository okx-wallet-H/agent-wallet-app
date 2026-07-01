package com.agentwallet.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class AuthRepository(private val context: Context) {

    private val tokenKey = stringPreferencesKey("jwt_token")
    private val emailKey = stringPreferencesKey("user_email")

    /** Stored JWT token (null if not logged in). */
    val token = context.dataStore.data.map { it[tokenKey] }

    /** Get current token synchronously (for ApiClient setup). */
    suspend fun getToken(): String? = context.dataStore.data.first()[tokenKey]

    suspend fun getEmail(): String? = context.dataStore.data.first()[emailKey]

    suspend fun saveAuth(token: String, email: String) {
        context.dataStore.edit {
            it[tokenKey] = token
            it[emailKey] = email
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { it.clear() }
    }
}
