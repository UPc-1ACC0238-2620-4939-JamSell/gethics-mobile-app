package com.jamsell.gethics.finance.presentation

import com.jamsell.gethics.finance.presentation.finance_summary.formatMoney
import com.jamsell.gethics.finance.presentation.register_transaction.TransactionFormError
import com.jamsell.gethics.finance.presentation.register_transaction.parseAmount
import com.jamsell.gethics.finance.presentation.register_transaction.validateTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinanceFormTest {

    @Test
    fun `formatMoney usa separador de miles y dos decimales`() {
        assertEquals("S/ 1,234.50", formatMoney(1234.5))
        assertEquals("S/ 0.00", formatMoney(0.0))
    }

    @Test
    fun `parseAmount acepta punto o coma decimal`() {
        assertEquals(150.5, parseAmount("150.50")!!, 0.0)
        assertEquals(150.5, parseAmount("150,50")!!, 0.0)
    }

    @Test
    fun `parseAmount rechaza vacio, cero, negativo y texto`() {
        assertNull(parseAmount(""))
        assertNull(parseAmount("0"))
        assertNull(parseAmount("-5"))
        assertNull(parseAmount("abc"))
    }

    @Test
    fun `validateTransaction pide monto mayor a cero y categoria`() {
        assertEquals(TransactionFormError.INVALID_AMOUNT, validateTransaction("0", "Alimento"))
        assertEquals(TransactionFormError.EMPTY_CATEGORY, validateTransaction("10", "  "))
        assertNull(validateTransaction("10", "Alimento"))
    }
}
