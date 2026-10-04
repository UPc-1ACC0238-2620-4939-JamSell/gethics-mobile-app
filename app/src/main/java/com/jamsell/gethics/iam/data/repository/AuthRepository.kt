package com.jamsell.gethics.iam.data.repository

import com.jamsell.gethics.iam.data.AuthService
import com.jamsell.gethics.shared.data.local.SessionStorage

class AuthRepository(
    private val service: AuthService,
    private val sessionStorage: SessionStorage
) {
    // TODO: signIn / signUp / forgotPassword / resetPassword devolviendo Resource<T>

    fun signOut() = sessionStorage.clear()
}
