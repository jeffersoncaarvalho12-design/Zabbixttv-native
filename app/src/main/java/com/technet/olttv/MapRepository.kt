package com.technet.olttv

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface MapApiService {
    @GET("olttv/api/mapa-tv.php")
    suspend fun getMap(): MapResponse
}

class MapRepository {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("http://200.106.207.64:5009/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MapApiService::class.java)

    suspend fun fetchMap(): Result<MapResponse> {
        return try {
            Result.success(api.getMap())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
