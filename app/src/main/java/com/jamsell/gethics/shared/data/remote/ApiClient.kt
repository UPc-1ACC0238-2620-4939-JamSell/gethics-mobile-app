package com.jamsell.gethics.shared.data.remote

import com.jamsell.gethics.shared.common.Constants
import com.jamsell.gethics.shared.data.local.SessionStorage
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    fun create(sessionStorage: SessionStorage): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionStorage))
            .build()

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

/** Agrega "Authorization: Bearer <token>" a cada request si hay sesion. */
class AuthInterceptor(private val sessionStorage: SessionStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        sessionStorage.getToken()?.let { builder.addHeader("Authorization", "Bearer $it") }
        return chain.proceed(builder.build())
    }
}
