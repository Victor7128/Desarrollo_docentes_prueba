package com.example.docentes.network

import com.example.docentes.models.Area
import com.example.docentes.models.Capacidad
import com.example.docentes.models.CompetenciaTemplate
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CurriculumApiService {

    @GET("areas")
    suspend fun getAreas(): List<Area>

    @GET("areas/{id}")
    suspend fun getArea(@Path("id") areaId: Int): Area

    @GET("areas/{id}/competencias")
    suspend fun getCompetenciasByArea(@Path("id") areaId: Int): List<CompetenciaTemplate>

    @GET("competencias")
    suspend fun getAllCompetencias(): List<CompetenciaTemplate>

    @GET("competencias/{id}")
    suspend fun getCompetencia(@Path("id") competenciaId: Int): CompetenciaTemplate

    @GET("capacidades")
    suspend fun getCapacidadesByCompetencia(@Query("competencia_id") competenciaId: Int): List<Capacidad>

    @GET("capacidades/{id}")
    suspend fun getCapacidad(@Path("id") capacidadId: Int): Capacidad
}