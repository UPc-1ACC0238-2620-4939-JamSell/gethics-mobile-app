package com.jamsell.gethics.analytics.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** US49: cada texto reports_* existe en espanol e ingles con los mismos marcadores de formato (%1$s...). */
class ReportsStringsTest {

    private fun load(folder: String): Map<String, String> {
        val file = File("src/main/res/$folder/strings.xml") // los tests unitarios corren en el directorio del modulo app
        assertTrue("no se encontro ${file.absolutePath}", file.exists())
        return Regex("""<string name="(reports_[a-z_]+)">(.*?)</string>""").findAll(file.readText())
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    private val es = load("values")
    private val en = load("values-en")
    private fun placeholders(text: String) = Regex("""%\d\$[sd]""").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun `espanol e ingles definen las mismas claves reports`() = assertEquals(es.keys, en.keys)

    @Test
    fun `los marcadores de formato coinciden entre idiomas`() {
        for (key in es.keys) assertEquals(key, placeholders(es.getValue(key)), placeholders(en.getValue(key)))
    }

    @Test
    fun `los textos de US49 existen y estan traducidos`() {
        val keys = listOf(
            "reports_apply_filters", "reports_clear_filters", "reports_filter_all", "reports_filters_active",
            "reports_no_results_filtered", "reports_exported"
        )
        for (key in keys) {
            assertTrue("falta $key", es[key].orEmpty().isNotBlank() && en[key].orEmpty().isNotBlank())
            assertTrue("$key sin traducir", es[key] != en[key])
        }
    }
}
