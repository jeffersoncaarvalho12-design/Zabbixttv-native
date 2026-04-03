package com.technet.olttv

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface MapApiService {
    @GET("olttv/api/mapa-tv.php")
    suspend fun getMap(): MapResponse
}

class MapRepository {

    private val api = Retrofit.Builder()
        .baseUrl("http://200.106.207.64/")
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
