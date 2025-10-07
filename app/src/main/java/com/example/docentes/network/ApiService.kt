package com.example.docentes.network

import com.example.docentes.models.BimesterData
import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("bimesters/full")
    fun getBimesters(): Call<List<BimesterData>>
}