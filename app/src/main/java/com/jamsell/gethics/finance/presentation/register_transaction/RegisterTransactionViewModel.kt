package com.jamsell.gethics.finance.presentation.register_transaction

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.finance.data.FinancialTransactionType
import com.jamsell.gethics.finance.data.RegisterTransactionRequest
import com.jamsell.gethics.finance.data.repository.FinanceRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Errores de validacion local del formulario; la pantalla los traduce a texto segun el idioma. */
enum class TransactionFormError { INVALID_AMOUNT, EMPTY_CATEGORY }

/** "150,50" o "150.50" -> 150.5. Devuelve null si no es un numero mayor a cero. */
fun parseAmount(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

/** Reglas del backend (monto > 0 y categoria obligatoria) validadas antes de llamar. Devuelve el error o null. */
fun validateTransaction(amountText: String, category: String): TransactionFormError? = when {
    parseAmount(amountText) == null -> TransactionFormError.INVALID_AMOUNT
    category.isBlank() -> TransactionFormError.EMPTY_CATEGORY
    else -> null
}

class RegisterTransactionViewModel(
    private val repository: FinanceRepository,
    private val ownerId: String
) : ViewModel() {

    // data = true -> movimiento guardado; message = error del backend
    private val _state = mutableStateOf(UIState<Boolean>())
    val state: State<UIState<Boolean>> get() = _state

    fun save(type: FinancialTransactionType, amountText: String, category: String, date: LocalDate, description: String) {
        val amount = parseAmount(amountText) ?: return
        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            val request = RegisterTransactionRequest(
                ownerId = ownerId,
                type = type,
                amount = amount,
                category = category.trim(),
                date = date.toString(),
                description = description.trim().ifEmpty { null }
            )
            _state.value = when (val result = repository.registerTransaction(request)) {
                is Resource.Success -> UIState(data = true)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo registrar el movimiento")
            }
        }
    }
}
