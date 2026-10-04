package com.jamsell.gethics.iam.presentation.forgot_password

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.jamsell.gethics.iam.data.repository.AuthRepository
import com.jamsell.gethics.shared.common.UIState

class ForgotPasswordViewModel(private val repository: AuthRepository) : ViewModel() {

    // TODO: cambiar Unit por el tipo real y agregar las funciones de la pantalla
    private val _state = mutableStateOf(UIState<Unit>())
    val state: State<UIState<Unit>> get() = _state
}
