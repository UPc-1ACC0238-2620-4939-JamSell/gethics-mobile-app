package com.jamsell.gethics.iot.presentation.readings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** US48: cada texto iot_* existe en espanol e ingles con los mismos marcadores de formato (%1$s, %2$d...). */
class IotStringsTest {

    private fun load(folder: String): Map<String, String> {
        val file = File("src/main/res/$folder/strings.xml") // los tests unitarios corren en el directorio del modulo app
        assertTrue("no se encontro ${file.absolutePath}", file.exists())
        return Regex("""<string name="(iot_[a-z_]+)">(.*?)</string>""").findAll(file.readText())
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    private val es = load("values")
    private val en = load("values-en")
    private fun placeholders(text: String) = Regex("""%\d\$[sd]""").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun `espanol e ingles definen las mismas claves iot`() = assertEquals(es.keys, en.keys)

    @Test
    fun `los marcadores de formato coinciden entre idiomas`() {
        for (key in es.keys) assertEquals(key, placeholders(es.getValue(key)), placeholders(en.getValue(key)))
    }

    @Test
    fun `los textos de US48 existen y estan traducidos`() {
        val keys = listOf(
            "iot_filter_from", "iot_filter_to", "iot_filter_apply", "iot_filter_clear", "iot_filter_invalid_range",
            "iot_filter_active", "iot_readings_empty_filtered", "iot_readings_error_connection", "iot_readings_retry",
            "iot_readings_device_unavailable"
        )
        for (key in keys) {
            assertTrue("falta $key", es[key].orEmpty().isNotBlank() && en[key].orEmpty().isNotBlank())
            assertTrue("$key sin traducir", es[key] != en[key])
        }
    }
}
