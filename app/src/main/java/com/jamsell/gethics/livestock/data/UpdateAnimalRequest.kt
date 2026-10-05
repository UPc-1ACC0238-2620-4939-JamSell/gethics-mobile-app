package com.jamsell.gethics.livestock.data

/**
 * Body de UpdateAnimalResource (US07). El PUT reemplaza todos los campos editables: lo que no se envia (null) se borra
 * en el backend. Por eso el formulario, que no pide peso ni foto, reenvia los valores actuales del animal.
 * El arete, el QR y el estado no se envian: no se editan aqui. Gson omite los null.
 */
data class UpdateAnimalRequest(
    val name: String?,
    val breed: String,
    val sex: String?,                  // MALE | FEMALE | null
    val birthDate: String,             // LocalDate ISO: yyyy-MM-dd
    val initialWeightKg: Double?,
    val photoUrl: String?,             // solo URLs del backend; las fotos locales (content://) no se envian
    val farmId: String?                // se reenvia el actual para no desasociar al animal; la granja se cambia por US10
)
