package com.jamsell.gethics.livestock.presentation

/**
 * Mensajes que arma la propia app cuando el backend no da uno (los ViewModels los guardan en espanol y las pruebas los
 * comparan tal cual). Las pantallas los traducen al idioma de la app con localizedMessage (AnimalTexts.kt).
 * Los mensajes que si envia el backend se muestran como llegan.
 */
const val LOAD_ANIMAL_ERROR = "No se pudo cargar el animal"
const val LOAD_INVENTORY_ERROR = "No se pudo cargar el inventario"
const val REGISTER_ANIMAL_ERROR = "No se pudo registrar el animal"
const val SAVE_ANIMAL_ERROR = "No se pudo guardar los cambios"
const val DEACTIVATE_ANIMAL_ERROR = "No se pudo dar de baja el animal"
const val SELL_ANIMAL_ERROR = "No se pudo registrar la venta"
const val QR_LOOKUP_ERROR = "No se pudo buscar el código QR"
const val LOAD_FARM_ERROR = "No se pudo cargar la granja"
const val SAVE_FARM_ERROR = "No se pudo guardar la granja"
