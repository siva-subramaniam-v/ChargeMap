package com.example.chargemap.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL = "https://api.openchargemap.io/"
//https://api.openchargemap.io/v3/poi?
// latitude=12.8316089&
// longitude=80.0518032&
// distance=100&
// distanceunit=km&
//levelid=3&
// compact=true&
// key=30376f4c-b00f-4de6-ac2b-df6e7112cb99

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

interface ApiService {
    @GET("v3/poi")
    suspend fun getStations(
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String,
        @Query("distance") distance: Int = 5000,
        @Query("distanceunit") distanceUnit: String = "km",
        @Query("compact") compact: Boolean = true,
        @Query("key") key: String
    ): List<NetworkStation>
}

object Network {
    val retrofitService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}