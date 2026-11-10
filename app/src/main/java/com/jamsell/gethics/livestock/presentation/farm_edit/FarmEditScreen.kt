package com.jamsell.gethics.livestock.presentation.farm_edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamsell.gethics.livestock.presentation.localizedMessage
import com.jamsell.gethics.R
import com.jamsell.gethics.livestock.domain.model.Farm
import com.jamsell.gethics.shared.common.UIState
import com.jamsell.gethics.shared.ui.components.AlertBanner
import com.jamsell.gethics.shared.ui.components.ErrorMessage
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.GethicsTextField
import com.jamsell.gethics.shared.ui.components.LoadingBox
import java.util.Locale

private val TextPrimary = Color(0xFF141D16)
private val TitleStyle = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, color = TextPrimary)

/** Mensajes de validacion en el idioma de la app; cualquier otro texto (por ejemplo el del backend) se muestra tal cual. */
@Composable
private fun localizedFarmError(error: String?): String? = when (error) {
    null -> null
    FARM_NAME_REQUIRED_ERROR -> stringResource(R.string.farm_error_name_required)
    FARM_NAME_TOO_LONG_ERROR -> stringResource(R.string.farm_error_name_too_long)
    FARM_LOCATION_REQUIRED_ERROR -> stringResource(R.string.farm_error_location_required)
    FARM_LOCATION_TOO_LONG_ERROR -> stringResource(R.string.farm_error_location_too_long)
    FARM_SIZE_INVALID_ERROR -> stringResource(R.string.farm_error_size_invalid)
    else -> error
}

/** US-32: editar nombre, ubicacion y tamano de la granja del animal. */
@Composable
fun FarmEditScreen(viewModel: FarmEditViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val farmState = viewModel.farm.value
    val saveState = viewModel.saveState.value
    val farm = farmState.data

    LaunchedEffect(saveState.data) {
        if (saveState.data == true) onSaved()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.animal_back), modifier = Modifier.size(20.dp), tint = TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.farm_edit_title), style = TitleStyle)
        }
        when {
            farmState.isLoading -> LoadingBox()
            farm == null -> ErrorMessage(message = localizedMessage(farmState.message), onRetry = { viewModel.load() })
            else -> FarmForm(farm, viewModel.errors.value, saveState, viewModel::save, onBack)
        }
    }
}

@Composable
private fun FarmForm(
    farm: Farm,
    errors: FarmEditErrors,
    saveState: UIState<Boolean>,
    onSave: (name: String, location: String, size: String) -> Unit,
    onCancel: () -> Unit
) {
    // Se precarga una sola vez por granja.
    var name by rememberSaveable(farm.id) { mutableStateOf(farm.name) }
    var location by rememberSaveable(farm.id) { mutableStateOf(farm.location) }
    var size by rememberSaveable(farm.id) {
        mutableStateOf(farm.sizeHectares?.let { String.format(Locale.US, "%.2f", it).trimEnd('0').trimEnd('.') }.orEmpty())
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GethicsTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.farm_name_label),
            errorMessage = localizedFarmError(errors.name),
            modifier = Modifier.fillMaxWidth()
        )
        GethicsTextField(
            value = location,
            onValueChange = { location = it },
            label = stringResource(R.string.farm_location_label),
            errorMessage = localizedFarmError(errors.location),
            modifier = Modifier.fillMaxWidth()
        )
        GethicsTextField(
            value = size,
            onValueChange = { size = it },
            label = stringResource(R.string.farm_size_label),
            helperText = stringResource(R.string.farm_size_hint),
            keyboardType = KeyboardType.Decimal,
            errorMessage = localizedFarmError(errors.size),
            modifier = Modifier.fillMaxWidth()
        )
        if (saveState.message.isNotEmpty()) AlertBanner(text = localizedMessage(saveState.message))

        GethicsButton(
            text = stringResource(R.string.farm_edit_save),
            onClick = { onSave(name, location, size) },
            modifier = Modifier.fillMaxWidth(),
            isLoading = saveState.isLoading
        )
        GethicsButton(
            text = stringResource(R.string.animal_cancel),
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            outlined = true
        )
    }
}
