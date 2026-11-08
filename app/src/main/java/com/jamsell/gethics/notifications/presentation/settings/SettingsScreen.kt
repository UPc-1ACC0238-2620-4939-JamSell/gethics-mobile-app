package com.jamsell.gethics.notifications.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.R
import com.jamsell.gethics.sanitary.presentation.register_event.FormHeader
import com.jamsell.gethics.sanitary.presentation.register_event.FormHorizontalPadding
import com.jamsell.gethics.sanitary.presentation.register_event.FormMessage
import com.jamsell.gethics.shared.data.local.LanguageManager
import com.jamsell.gethics.shared.ui.components.GethicsCard

/** Ajustes: notificaciones push (US22) e idioma de la app (ES / EN). */
@Composable
fun SettingsScreen(viewModel: NotificationSettingsViewModel, onBack: () -> Unit) {
    val state = viewModel.state.value
    val pushEnabled = state.data
    val context = LocalContext.current
    val currentLanguage = LanguageManager.saved(context)

    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize()) {
        FormHeader(title = stringResource(R.string.settings_title), onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FormHorizontalPadding, vertical = 24.dp)
        ) {
            // ---- Notificaciones push ----
            Text(stringResource(R.string.notif_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (pushEnabled != null) {
                GethicsCard {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(stringResource(R.string.notif_push), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.notif_push_desc), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = pushEnabled, onCheckedChange = viewModel::setPushEnabled)
                    }
                }
            } else if (state.isLoading) {
                Text(stringResource(R.string.settings_loading), style = MaterialTheme.typography.bodySmall)
            }
            if (state.message.isNotEmpty()) FormMessage(state.message)

            // ---- Idioma ----
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            GethicsCard {
                LanguageOption(
                    label = stringResource(R.string.lang_system),
                    selected = currentLanguage == null,
                    onSelect = { LanguageManager.change(context, null) }
                )
                LanguageOption(
                    label = stringResource(R.string.lang_es),
                    selected = currentLanguage == LanguageManager.SPANISH,
                    onSelect = { LanguageManager.change(context, LanguageManager.SPANISH) }
                )
                LanguageOption(
                    label = stringResource(R.string.lang_en),
                    selected = currentLanguage == LanguageManager.ENGLISH,
                    onSelect = { LanguageManager.change(context, LanguageManager.ENGLISH) }
                )
            }
        }
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp))
    }
}
