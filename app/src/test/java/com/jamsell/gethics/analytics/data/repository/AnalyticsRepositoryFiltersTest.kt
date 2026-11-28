package com.jamsell.gethics.analytics.data.repository

import com.jamsell.gethics.analytics.data.AnalyticsService
import com.jamsell.gethics.finance.data.FinancialTransactionType
import com.jamsell.gethics.shared.common.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.time.LocalDate

/**
 * US49. Retrofit y Gson reales; el "servidor" es un interceptor que registra la URL pedida y responde algo fijo.
 * Prueba de repositorio con servidor simulado: no toca el staging.
 */
class AnalyticsRepositoryFiltersTest {

    private val owner = "00000000-0000-0000-0000-000000000001"
    private val from = LocalDate.of(2026, 3, 1)
    private val to = LocalDate.of(2026, 3, 31)

    private val reportJson = """
        {"ownerId":"$owner","from":"2026-03-01","to":"2026-03-31","type":"EXPENSE","category":"Alimento",
         "finance":{"totalIncome":0,"totalExpense":200.00,"netResult":-200.00,"limitedData":false},
         "health":{"limitedData":true,"message":"Los datos aun son limitados para un analisis completo."},
         "productivity":{"limitedData":true,"message":"Los datos aun son limitados para un analisis completo."}}
    """.trimIndent()

    private class Server(
        val code: Int = 200,
        val body: String = "",
        val contentType: String = "application/json",
        val headers: Map<String, String> = emptyMap(),
        val failure: IOException? = null
    ) {
        val urls = mutableListOf<HttpUrl>()
    }

    private fun repository(server: Server): AnalyticsRepository {
        val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            server.urls += chain.request().url
            server.failure?.let { throw it }
            val builder = Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(server.code)
                .message("test")
                .body(server.body.toResponseBody(server.contentType.toMediaType()))
            server.headers.forEach { (name, value) -> builder.header(name, value) }
            builder.build()
        }).build()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return AnalyticsRepository(retrofit.create(AnalyticsService::class.java))
    }

    private fun HttpUrl.hasParam(name: String) = name in queryParameterNames

    @Test
    fun `sin filtros nuevos solo envia ownerId from y to como antes`() = runBlocking {
        val server = Server(body = reportJson)
        repository(server).getReport(owner, from, to)

        val url = server.urls.single()
        assertEquals("/api/v1/reports", url.encodedPath)
        assertEquals(owner, url.queryParameter("ownerId"))
        assertEquals("2026-03-01", url.queryParameter("from"))
        assertEquals("2026-03-31", url.queryParameter("to"))
        assertEquals(setOf("ownerId", "from", "to"), url.queryParameterNames)
    }

    @Test
    fun `type INCOME se envia como INCOME`() = runBlocking {
        val server = Server(body = reportJson)
        repository(server).getReport(owner, from, to, type = FinancialTransactionType.INCOME)

        val url = server.urls.single()
        assertEquals("INCOME", url.queryParameter("type"))
        assertTrue(!url.hasParam("category"))
    }

    @Test
    fun `type EXPENSE se envia como EXPENSE`() = runBlocking {
        val server = Server(body = reportJson)
        repository(server).getReport(owner, from, to, type = FinancialTransactionType.EXPENSE)

        assertEquals("EXPENSE", server.urls.single().queryParameter("type"))
    }

    @Test
    fun `category se envia tal cual sin cambiar mayusculas`() = runBlocking {
        val server = Server(body = reportJson)
        repository(server).getReport(owner, from, to, category = "AlImEnTo")

        val url = server.urls.single()
        assertEquals("AlImEnTo", url.queryParameter("category"))
        assertTrue(!url.hasParam("type"))
    }

    @Test
    fun `type y category combinados viajan juntos en el reporte y en la exportacion`() = runBlocking {
        val report = Server(body = reportJson)
        val export = Server(body = "x", contentType = "text/csv")
        repository(report).getReport(owner, from, to, FinancialTransactionType.EXPENSE, "Alimento")
        repository(export).exportReport(owner, from, to, FinancialTransactionType.EXPENSE, "Alimento")

        // Mismos parametros en los dos endpoints; solo cambia la ruta
        assertEquals("/api/v1/reports", report.urls.single().encodedPath)
        assertEquals("/api/v1/reports/export", export.urls.single().encodedPath)
        assertEquals(report.urls.single().encodedQuery, export.urls.single().encodedQuery)
        assertEquals("EXPENSE", export.urls.single().queryParameter("type"))
        assertEquals("Alimento", export.urls.single().queryParameter("category"))
    }

    @Test
    fun `una categoria vacia o con solo espacios no se envia`() = runBlocking {
        val server = Server(body = reportJson)
        val repository = repository(server)
        repository.getReport(owner, from, to, category = "")
        repository.getReport(owner, from, to, category = "   ")
        repository.exportReport(owner, from, to, category = "  ")

        assertTrue(server.urls.none { it.hasParam("category") })
    }

    @Test
    fun `una categoria con espacios alrededor se envia recortada`() = runBlocking {
        val server = Server(body = reportJson)
        repository(server).getReport(owner, from, to, category = "  Alimento ")

        assertEquals("Alimento", server.urls.single().queryParameter("category"))
    }

    @Test
    fun `las categorias con acentos y simbolos se codifican en la URL`() = runBlocking {
        val server = Server(body = reportJson)
        repository(server).getReport(owner, from, to, category = "Vacunación & más")

        assertEquals("Vacunación & más", server.urls.single().queryParameter("category"))
    }

    @Test
    fun `decodifica el reporte con los filtros que aplico el backend`() = runBlocking {
        val result = repository(Server(body = reportJson)).getReport(owner, from, to, FinancialTransactionType.EXPENSE, "Alimento")

        val report = (result as Resource.Success).data!!
        assertEquals("EXPENSE", report.type)
        assertEquals("Alimento", report.category)
        assertEquals(200.0, report.finance.totalExpense, 0.0)
        assertEquals(-200.0, report.finance.netResult, 0.0)
        assertTrue(report.health.limitedData)
    }

    @Test
    fun `el reporte sin filtros sigue decodificandose sin type ni category`() = runBlocking {
        val json = """
            {"ownerId":"$owner","from":"2026-03-01","to":"2026-03-31",
             "finance":{"totalIncome":500.00,"totalExpense":120.00,"netResult":380.00,"limitedData":false},
             "health":{"limitedData":true,"message":"m"},"productivity":{"limitedData":true,"message":"m"}}
        """.trimIndent()
        val report = (repository(Server(body = json)).getReport(owner, from, to) as Resource.Success).data!!

        assertNull(report.type)
        assertNull(report.category)
        assertEquals(380.0, report.finance.netResult, 0.0)
    }

    @Test
    fun `un 400 devuelve el mensaje del backend`() = runBlocking {
        val server = Server(code = 400, body = """{"message":"Parametro invalido: type"}""")
        val result = repository(server).getReport(owner, from, to)

        assertEquals("Parametro invalido: type", (result as Resource.Error).message)
    }

    @Test
    fun `la exportacion devuelve los bytes del CSV tal cual y el nombre del Content-Disposition`() = runBlocking {
        val csv = "﻿Seccion,Indicador,Valor\r\nFinanzas,Egresos totales,200.00\r\n"
        val server = Server(
            body = csv,
            contentType = "text/csv;charset=UTF-8",
            headers = mapOf("Content-Disposition" to "attachment; filename=\"reporte-ganado-2026-03-01-2026-03-31.csv\"")
        )
        val result = repository(server).exportReport(owner, from, to, FinancialTransactionType.EXPENSE, "Alimento")

        val exported = (result as Resource.Success).data!!
        assertEquals("reporte-ganado-2026-03-01-2026-03-31.csv", exported.fileName)
        assertArrayEquals(csv.toByteArray(Charsets.UTF_8), exported.bytes) // Android no parsea ni regenera el CSV
    }

    @Test
    fun `la exportacion sin Content-Disposition arma el nombre con el rango`() = runBlocking {
        val result = repository(Server(body = "x", contentType = "text/csv")).exportReport(owner, from, to)

        assertEquals("reporte-ganado-2026-03-01-2026-03-31.csv", (result as Resource.Success).data!!.fileName)
    }

    @Test
    fun `un 404 de exportacion sin datos devuelve el aviso del backend`() = runBlocking {
        val server = Server(code = 404, body = """{"message":"No hay información disponible para el periodo seleccionado"}""")
        val result = repository(server).exportReport(owner, from, to, FinancialTransactionType.INCOME, "Nada")

        assertEquals("No hay información disponible para el periodo seleccionado", (result as Resource.Error).message)
    }

    @Test
    fun `sin red devuelve el error de conexion en reporte y exportacion`() = runBlocking {
        val repository = repository(Server(failure = IOException("sin red")))

        assertTrue(repository.getReport(owner, from, to) is Resource.Error)
        assertTrue(repository.exportReport(owner, from, to) is Resource.Error)
    }
}
