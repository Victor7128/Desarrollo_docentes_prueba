package com.example.docentes.models

data class BimesterData(
    val id: Int,
    val name: String,
    val grades: List<Grade>
)

data class Grade(
    val id: Int,
    val bimester_id: Int,
    val number: Int,
    val sections: List<Section>
)

data class Section(
    val id: Int,
    val grade_id: Int,
    val letter: String
)
