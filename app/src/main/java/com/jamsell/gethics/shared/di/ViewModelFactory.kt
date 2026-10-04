package com.jamsell.gethics.shared.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Crea un ViewModel con dependencias manuales y lo ata a la pantalla actual (sobrevive rotaciones).
 * Uso:  val vm = gethicsViewModel { SignInViewModel(container.authRepository) }
 */
@Composable
inline fun <reified VM : ViewModel> gethicsViewModel(crossinline create: () -> VM): VM =
    viewModel(factory = viewModelFactory { initializer { create() } })
