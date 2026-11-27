package com.jamsell.gethics.iot.data.repository

import com.jamsell.gethics.iot.data.IotService
import com.jamsell.gethics.shared.common.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.time.LocalDate

/**
 * US48. Retrofit y Gson reales; el "servidor" es un interceptor que registra la URL pedida y responde un JSON fijo.
 * Prueba de repositorio con servidor simulado: no toca el staging.
 */
class IotRepositoryReadingsTest {

    private val deviceId = "aaaaaaaa-0000-0000-0000-000000000001"

    private val pageJson = """
        {"content":[
          {"id":"bbbbbbbb-0000-0000-0000-000000000001","metric":"temperature","value":38.2,"recordedAt":"2026-11-27T08:00:00"}
        ],"page":0,"size":20,"totalElements":1,"totalPages":1}
    """.trimIndent()

    private class Server(val code: Int = 200, val body: String = "", val failure: IOException? = null) {
        val urls = mutableListOf<HttpUrl>()
    }

    private fun repository(server: Server): IotRepository {
        val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            server.urls += chain.request().url
            server.failure?.let { throw it }
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(server.code)
                .message("test")
                .body(server.body.toResponseBody("application/json".toMediaType()))
                .build()
        }).build()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return IotRepository(retrofit.create(IotService::class.java))
    }

    @Test
    fun `sin filtros no envia from ni to`() = runBlocking {
        val server = Server(body = pageJson)
        repository(server).getReadings(deviceId, 0, 20)

        val url = server.urls.single()
        assertEquals("/api/v1/devices/$deviceId/readings", url.encodedPath)
        assertEquals("0", url.queryParameter("page"))
        assertEquals("20", url.queryParameter("size"))
        assertTrue("from" !in url.queryParameterNames)
        assertTrue("to" !in url.queryParameterNames)
    }

    @Test
    fun `solo from envia from y no to`() = runBlocking {
        val server = Server(body = pageJson)
        repository(server).getReadings(deviceId, 0, 20, from = LocalDate.of(2026, 11, 1))

        val url = server.urls.single()
        assertEquals("2026-11-01", url.queryParameter("from"))
        assertTrue("to" !in url.queryParameterNames)
    }

    @Test
    fun `solo to envia to y no from`() = runBlocking {
        val server = Server(body = pageJson)
        repository(server).getReadings(deviceId, 0, 20, to = LocalDate.of(2026, 11, 27))

        val url = server.urls.single()
        assertEquals("2026-11-27", url.queryParameter("to"))
        assertTrue("from" !in url.queryParameterNames)
    }

    @Test
    fun `ambas fechas se envian como yyyy-MM-dd con ceros y pagina solicitada`() = runBlocking {
        val server = Server(body = pageJson)
        repository(server).getReadings(deviceId, 3, 20, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 9))

        val url = server.urls.single()
        assertEquals("2026-01-05", url.queryParameter("from"))
        assertEquals("2026-02-09", url.queryParameter("to"))
        assertEquals("3", url.queryParameter("page"))
    }

    @Test
    fun `un mismo dia envia from igual a to`() = runBlocking {
        val server = Server(body = pageJson)
        val day = LocalDate.of(2026, 11, 27)
        repository(server).getReadings(deviceId, 0, 20, day, day)

        val url = server.urls.single()
        assertEquals("2026-11-27", url.queryParameter("from"))
        assertEquals("2026-11-27", url.queryParameter("to"))
    }

    @Test
    fun `decodifica la pagina de lecturas`() = runBlocking {
        val result = repository(Server(body = pageJson)).getReadings(deviceId, 0, 20)

        val page = (result as Resource.Success).data!!
        val reading = page.content.single()
        assertEquals("bbbbbbbb-0000-0000-0000-000000000001", reading.id)
        assertEquals("temperature", reading.metric)
        assertEquals(38.2, reading.value, 0.0001)
        assertEquals("2026-11-27T08:00:00", reading.recordedAt) // sin zona: se conserva tal cual
        assertEquals(0, page.page)
        assertEquals(20, page.size)
        assertEquals(1L, page.totalElements)
        assertEquals(1, page.totalPages)
    }

    @Test
    fun `200 con content vacio es Success con lista vacia`() = runBlocking {
        val empty = """{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0}"""
        val result = repository(Server(body = empty)).getReadings(deviceId, 0, 20, LocalDate.of(2026, 11, 1), null)

        val page = (result as Resource.Success).data!!
        assertTrue(page.content.isEmpty())
        assertEquals(false, page.hasMore)
    }

    @Test
    fun `400 devuelve el mensaje del backend`() = runBlocking {
        val server = Server(code = 400, body = """{"message":"El parametro from no puede ser posterior a to."}""")
        val result = repository(server).getReadings(deviceId, 0, 20)

        assertTrue(result is Resource.Error)
        assertEquals("El parametro from no puede ser posterior a to.", result.message)
        assertNull(result.data)
    }

    @Test
    fun `404 devuelve el mensaje del backend`() = runBlocking {
        val server = Server(code = 404, body = """{"message":"Dispositivo no encontrado"}""")
        val result = repository(server).getReadings(deviceId, 0, 20)

        assertTrue(result is Resource.Error)
        assertEquals("Dispositivo no encontrado", result.message)
    }

    @Test
    fun `404 sin cuerpo JSON devuelve un mensaje generico con el codigo`() = runBlocking {
        val result = repository(Server(code = 404, body = "")).getReadings(deviceId, 0, 20)

        assertTrue(result is Resource.Error)
        assertTrue(result.message!!.contains("404"))
    }

    @Test
    fun `error de conexion devuelve el mensaje de conexion`() = runBlocking {
        val result = repository(Server(failure = IOException("sin red"))).getReadings(deviceId, 0, 20)

        assertTrue(result is Resource.Error)
        assertEquals(CONNECTION_ERROR, result.message)
    }
}
