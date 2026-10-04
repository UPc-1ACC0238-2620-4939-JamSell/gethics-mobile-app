package com.jamsell.gethics.iam.presentation.profile

import androidx.compose.runtime.Composable
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onViewPlans: () -> Unit, onSignOut: () -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Mi perfil",
        userStory = "US04",
        description = "Ver y editar perfil. GET/PUT users/me."
    ) {
        GethicsButton(text = "Ver planes", onClick = { onViewPlans() }, outlined = true)
        GethicsButton(text = "Cerrar sesion", onClick = { onSignOut() }, outlined = true)
    }
}
