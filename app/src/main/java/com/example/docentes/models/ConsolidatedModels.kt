package com.example.docentes.models

// ✅ Respuesta que coincide EXACTAMENTE con tu endpoint Rust
data class ConsolidatedResponse(
    val students: List<ConsolidatedStudent>,
    val sessions: List<ConsolidatedSession>,
    val competencies: List<ConsolidatedCompetency>,
    val abilities: List<ConsolidatedAbility>,
    val criteria: List<ConsolidatedCriterion>,
    val values: List<ConsolidatedValue>,
    val observations: List<ConsolidatedObservation>
)

// ✅ Modelos que coinciden con tu estructura Rust
data class ConsolidatedStudent(
    val id: Int,
    val full_name: String
)

data class ConsolidatedSession(
    val id: Int,
    val title: String?,
    val number: Int
)

data class ConsolidatedCompetency(
    val id: Int,
    val session_id: Int,
    val display_name: String
)

data class ConsolidatedAbility(
    val id: Int,
    val competency_id: Int,
    val display_name: String
)

data class ConsolidatedCriterion(
    val id: Int,
    val ability_id: Int,
    val display_name: String
)

data class ConsolidatedValue(
    val student_id: Int,
    val criterion_id: Int,
    val value: String
)

data class ConsolidatedObservation(
    val student_id: Int,
    val ability_id: Int,
    val observation: String
)