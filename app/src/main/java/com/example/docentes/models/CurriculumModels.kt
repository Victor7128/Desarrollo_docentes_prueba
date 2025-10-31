package com.example.docentes.models

import com.google.gson.annotations.SerializedName

data class Area(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("color")
    val color: String? = null
)

data class CompetenciaTemplate(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("area_id")
    val area_id: Int,

    @SerializedName("area_nombre")
    val area_nombre: String
)

data class Capacidad(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("competencia_id")
    val competenciaId: Int? = null,

    @SerializedName("description")
    val description: String? = null
)