package com.jamsell.gethics.shared.navigation

import com.jamsell.gethics.iam.domain.model.Role

/** Todas las rutas en un solo lugar. Rutas con argumento: usar las funciones de abajo, no concatenar strings.
 * animalId es String porque el backend usa UUID. */
object Routes {
    // iam
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESET_PASSWORD = "reset_password"
    const val PROFILE = "profile"

    // livestock
    const val ANIMAL_LIST = "animal_list"
    const val ANIMAL_REGISTER = "animal_register"
    const val ANIMAL_DETAIL = "animal_detail/{animalId}"
    fun animalDetail(animalId: String) = "animal_detail/$animalId"

    // sanitary
    const val SANITARY_CALENDAR = "sanitary_calendar"
    const val REGISTER_EVENT = "register_event/{animalId}"
    fun registerEvent(animalId: String) = "register_event/$animalId"
    const val SCHEDULE_VACCINATION = "schedule_vaccination/{animalId}"
    fun scheduleVaccination(animalId: String) = "schedule_vaccination/$animalId"
    const val CLINICAL_HISTORY = "clinical_history/{animalId}"
    fun clinicalHistory(animalId: String) = "clinical_history/$animalId"

    // veterinary
    const val ASSIGNED_CLIENTS = "assigned_clients"
    const val CLIENT_PATIENTS = "client_patients/{clientId}"
    fun clientPatients(clientId: Long) = "client_patients/$clientId"

    // finance
    const val FINANCE_SUMMARY = "finance_summary"
    const val REGISTER_TRANSACTION = "register_transaction"

    // analytics
    const val REPORTS = "reports"

    // subscription
    const val PLANS = "plans"

    /** Pantalla inicial segun rol. */
    fun homeFor(role: Role): String = when (role) {
        Role.GANADERO -> ANIMAL_LIST
        Role.VETERINARIO -> ASSIGNED_CLIENTS
    }
}
