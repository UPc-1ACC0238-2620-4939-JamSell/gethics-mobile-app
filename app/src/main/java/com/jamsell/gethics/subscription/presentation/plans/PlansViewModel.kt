package com.jamsell.gethics.subscription.presentation.plans

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.jamsell.gethics.subscription.data.repository.SubscriptionRepository
import com.jamsell.gethics.shared.common.UIState

class PlansViewModel(private val repository: SubscriptionRepository) : ViewModel() {

    // TODO: cambiar Unit por el tipo real y agregar las funciones de la pantalla
    private val _state = mutableStateOf(UIState<Unit>())
    val state: State<UIState<Unit>> get() = _state
}
