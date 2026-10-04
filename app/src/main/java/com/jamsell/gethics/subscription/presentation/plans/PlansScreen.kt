package com.jamsell.gethics.subscription.presentation.plans

import androidx.compose.runtime.Composable
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun PlansScreen(viewModel: PlansViewModel, onBack: () -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Planes",
        userStory = "US24",
        description = "Planes de suscripcion. GET plans, POST subscriptions."
    ) {
        GethicsButton(text = "Volver", onClick = { onBack() }, outlined = true)
    }
}
