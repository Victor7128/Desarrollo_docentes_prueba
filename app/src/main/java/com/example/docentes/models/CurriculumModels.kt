package com.example.docentes.models

data class Area(
    val id: Int,
    val nombre: String,
    val description: String? = null
)

data class CompetenciaTemplate(
    val id: Int,
    val nombre: String,
    val area_id: Int,
    val area_nombre: String
)

data class Capacidad(
    val id: Int,
    val nombre: String
)