package com.jamsell.gethics.livestock.presentation

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val LAST_UPDATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** US-36. "10/11/2026 14:30" en la zona horaria del dispositivo; null si nunca se actualizo. */
fun formatLastUpdate(epochMillis: Long?, zone: ZoneId = ZoneId.systemDefault()): String? =
    epochMillis?.let { LAST_UPDATE_FORMAT.format(Instant.ofEpochMilli(it).atZone(zone)) }
