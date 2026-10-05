package com.jamsell.gethics.finance.presentation.register_transaction

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.finance.data.RegisterTransactionRequest
import com.jamsell.gethics.finance.data.TransactionType
import com.jamsell.gethics.finance.data.repository.FinanceRepository
import com.jamsell.gethics.shared.common.Constants
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.launch

class RegisterTransactionViewModel(private val repository: FinanceRepository) : ViewModel() {

    // data = Unit cuando se guardo: la pantalla vuelve atras
    private val _state = mutableStateOf(UIState<Unit>())
    val state: State<UIState<Unit>> get() = _state

    fun register(type: TransactionType, amountText: String, category: String, date: String, description: String) {
        val amount = amountText.replace(',', '.').toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _state.value = UIState(message = "El monto debe ser mayor a cero")
            return
        }
        if (category.isBlank()) {
            _state.value = UIState(message = "Ingresa una categoría")
            return
        }
        val parsedDate = try {
            LocalDate.parse(date.trim())
        } catch (e: DateTimeParseException) {
            _state.value = UIState(message = "Fecha inválida. Usa el formato AAAA-MM-DD")
            return
        }

        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            val request = RegisterTransactionRequest(
                ownerId = Constants.DEV_OWNER_ID, // TODO: usar el usuario real cuando iam devuelva UUID
                type = type,
                amount = amount,
                category = category.trim(),
                date = parsedDate.toString(),
                description = description.trim().ifEmpty { null }
            )
            _state.value = when (val result = repository.registerTransaction(request)) {
                is Resource.Success -> UIState(data = Unit)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo registrar el movimiento")
            }
        }
    }
}