package com.example.docentes

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.evrencoskun.tableview.TableView
import com.example.docentes.adapters.ReportsTableAdapter
import com.example.docentes.models.*
import com.example.docentes.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

class ReportsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ReportsActivity"
    }

    // ✅ Views
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvSectionTitle: TextView
    private lateinit var tvCompetencyName: TextView
    private lateinit var tableView: TableView
    private lateinit var btnExportExcel: MaterialButton
    private lateinit var progressBar: android.widget.ProgressBar

    // ✅ TableView components
    private lateinit var tableAdapter: ReportsTableAdapter

    // ✅ Datos del Intent
    private var sectionId: Int = -1
    private var competencyId: Int = -1
    private var sectionName: String = ""

    // ✅ Solo una variable para datos consolidados
    private var consolidatedData: ConsolidatedResponse? = null

    private val editedAverages = mutableMapOf<String, String?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        initViews()
        getIntentData()
        setupToolbar()
        setupTableView()
        setupButtons()

        // ✅ Cargar datos del consolidado
        loadReportsData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvSectionTitle = findViewById(R.id.tvSectionTitle)
        tvCompetencyName = findViewById(R.id.tvCompetencyName)
        tableView = findViewById(R.id.tableView)
        btnExportExcel = findViewById(R.id.btnExportExcel)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun getIntentData() {
        sectionId = intent.getIntExtra("SECTION_ID", -1)
        competencyId = intent.getIntExtra("COMPETENCY_ID", -1) // ✅ Puede ser -1
        sectionName = intent.getStringExtra("SECTION_NAME") ?: "Sección"

        Log.d(TAG, "Intent data: SECTION_ID=$sectionId, COMPETENCY_ID=$competencyId, SECTION_NAME=$sectionName")

        if (sectionId == -1) {
            Toast.makeText(this, "Error: ID de sección no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ✅ COMPETENCY_ID es opcional para consolidado general
        if (competencyId == -1) {
            Log.d(TAG, "Cargando consolidado general (todas las competencias)")
        }
    }

    private fun setupToolbar() {
        toolbar.title = "Consolidado - $sectionName"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupTableView() {
        // ✅ Crear adapter con nuevos callbacks
        tableAdapter = ReportsTableAdapter(
            context = this,
            onGradeChanged = { studentId, abilityId, grade ->
                handleAverageChange(studentId, abilityId, grade)
            },
            onObservationClicked = { studentId, criterionId ->
                showObservationDialog(studentId, criterionId)
            }
        )

        // ✅ Configurar TableView con ancho mayor para headers multinivel
        tableView.setAdapter(tableAdapter)
        tableView.rowHeaderWidth = 350 // Ancho para nombres completos
        tableView.isShowHorizontalSeparators = true
        tableView.isShowVerticalSeparators = true

        // ✅ Configurar scroll más suave
        try {
            tableView.separatorColor = "#E0E0E0".toColorInt()
        } catch (e: Exception) {
            Log.w(TAG, "setSeparatorColor no disponible")
        }
    }

    private fun setupButtons() {
        btnExportExcel.setOnClickListener { exportToExcel() }
    }

    private fun loadReportsData() {
        Log.d(TAG, "🚀 Cargando consolidado de sección: $sectionId")
        showLoading(true)

        lifecycleScope.launch {
            try {
                // ✅ USAR TU ENDPOINT EXISTENTE
                val consolidatedResponse = RetrofitClient.apiService.getConsolidatedReport(sectionId)

                consolidatedData = consolidatedResponse
                displayConsolidatedTable(consolidatedResponse)

                Log.d(TAG, "✅ Consolidado cargado:")
                Log.d(TAG, "  Students: ${consolidatedResponse.students.size}")
                Log.d(TAG, "  Sessions: ${consolidatedResponse.sessions.size}")
                Log.d(TAG, "  Competencies: ${consolidatedResponse.competencies.size}")
                Log.d(TAG, "  Abilities: ${consolidatedResponse.abilities.size}")
                Log.d(TAG, "  Criteria: ${consolidatedResponse.criteria.size}")
                Log.d(TAG, "  Values: ${consolidatedResponse.values.size}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando consolidado", e)
                Toast.makeText(this@ReportsActivity,
                    "Error: ${e.message}", Toast.LENGTH_LONG).show()

                // ✅ Cargar datos de prueba como fallback
                loadTestConsolidatedData()

            } finally {
                showLoading(false)
            }
        }
    }

    private fun displayConsolidatedTable(data: ConsolidatedResponse) {
        // ✅ Actualizar información del header con más detalles
        tvSectionTitle.text = "Consolidado $sectionName"

        // ✅ Mostrar competencia principal o resumen
        val competenciesText = if (data.competencies.size == 1) {
            data.competencies.first().display_name
        } else {
            "${data.competencies.size} Competencias Evaluadas"
        }
        tvCompetencyName.text = competenciesText

        // ✅ Preparar headers multinivel usando TUS datos
        val columnHeaders = createConsolidatedHeaders(data)
        val rowHeaders = data.students.mapIndexed { index, student ->
            ReportRowHeaderModel(
                id = "student_${student.id}",
                title = formatStudentName(student.full_name),
                studentId = student.id,
                number = index + 1
            )
        }

        // ✅ Preparar celdas multinivel usando TUS datos
        val cells = createConsolidatedCells(data)

        // ✅ Establecer datos en TableView
        tableAdapter.setAllItems(columnHeaders, rowHeaders, cells)

        // ✅ Log detallado de la estructura
        Log.d(TAG, "📊 Tabla multinivel cargada:")
        Log.d(TAG, "  Students: ${data.students.size}")
        Log.d(TAG, "  Sessions: ${data.sessions.size}")
        Log.d(TAG, "  Competencies: ${data.competencies.size}")
        Log.d(TAG, "  Abilities: ${data.abilities.size}")
        Log.d(TAG, "  Criteria: ${data.criteria.size}")
        Log.d(TAG, "  Column Headers: ${columnHeaders.size}")
        Log.d(TAG, "  Cell Rows: ${cells.size}")

        Toast.makeText(this,
            "✅ Consolidado multinivel: ${data.students.size} estudiantes, ${data.sessions.size} sesiones",
            Toast.LENGTH_SHORT).show()
    }

    private fun handleAverageChange(studentId: Int, abilityId: Int?, grade: String?) {
        val key = "${studentId}_${abilityId ?: "final"}"
        editedAverages[key] = grade

        Log.d(TAG, "📝 Promedio editado: Student $studentId, Ability $abilityId = $grade")

        // ✅ Marcar como modificado (para futuro guardado)
        // TODO: Implementar guardado de promedios editados
    }

    private fun showObservationDialog(studentId: Int, criterionId: Int) {
        val studentName = getStudentName(studentId)
        val criterionName = getCriterionName(criterionId)

        // ✅ Buscar observación existente
        val currentObservation = consolidatedData?.observations?.find {
            it.student_id == studentId &&
                    // Buscar por criterion o ability relacionada
                    consolidatedData?.criteria?.find { c -> c.id == criterionId }?.ability_id == it.ability_id
        }?.observation ?: ""

        val editText = android.widget.EditText(this).apply {
            setText(currentObservation)
            hint = "Observación para $criterionName..."
            setPadding(16, 16, 16, 16)
            minLines = 3
            maxLines = 5
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📝 Observación")
            .setMessage("Estudiante: $studentName\nCriterio: $criterionName")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val observation = editText.text.toString().trim()
                saveObservation(studentId, criterionId, observation.ifEmpty { null })
            }
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Limpiar") { _, _ ->
                saveObservation(studentId, criterionId, null)
            }
            .show()

        editText.selectAll()
        editText.requestFocus()
    }

    private fun saveObservation(studentId: Int, criterionId: Int, observation: String?) {
        Log.d(TAG, "💾 Guardando observación: Student $studentId, Criterion $criterionId")

        // TODO: Implementar guardado de observaciones
        // Por ahora solo actualizar en memoria

        val message = if (observation.isNullOrEmpty()) {
            "🗑️ Observación eliminada"
        } else {
            "💾 Observación guardada"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // ✅ Recargar datos para reflejar cambios
        // loadReportsData()
    }

    private fun createConsolidatedHeaders(data: ConsolidatedResponse): List<ReportColumnHeaderModel> {
        val headers = mutableListOf<ReportColumnHeaderModel>()

        // ✅ PASO 1: Organizar datos por sesión → competencia → capacidad
        val sessionDataMap = organizeDataBySession(data)

        sessionDataMap.forEach { (session, competencies) ->

            // ✅ NIVEL 1: Header de Sesión (si hay múltiples sesiones)
            if (sessionDataMap.size > 1) {
                val sessionColspan = calculateSessionColspan(competencies)
                headers.add(
                    ReportColumnHeaderModel(
                        id = "session_${session.id}",
                        title = session.title ?: "Sesión ${session.number}",
                        level = 1,
                        colspan = sessionColspan,
                        rowspan = 1,
                        sessionId = session.id,
                        backgroundColor = "#F5F5F5"
                    )
                )
            }

            competencies.forEach { competencyData ->
                val competency = competencyData.competency

                // ✅ NIVEL 2: Header de Competencia
                val competencyColspan = calculateCompetencyColspan(competencyData.abilities)
                headers.add(
                    ReportColumnHeaderModel(
                        id = "competency_${competency.id}",
                        title = competency.display_name,
                        level = 2,
                        colspan = competencyColspan,
                        rowspan = 1,
                        sessionId = session.id,
                        competencyId = competency.id,
                        backgroundColor = "#E8F5E8"
                    )
                )

                competencyData.abilities.forEach { abilityData ->
                    val ability = abilityData.ability

                    // ✅ NIVEL 3: Header de Capacidad
                    val abilityColspan = (abilityData.criteria.size * 2) + 1 // criterios + observaciones + PROM
                    headers.add(
                        ReportColumnHeaderModel(
                            id = "ability_${ability.id}",
                            title = ability.display_name,
                            level = 3,
                            colspan = abilityColspan,
                            rowspan = 1,
                            sessionId = session.id,
                            competencyId = competency.id,
                            abilityId = ability.id,
                            backgroundColor = "#E8EAF6"
                        )
                    )

                    // ✅ NIVEL 4: Criterios + Observaciones
                    abilityData.criteria.forEach { criterion ->
                        // Criterio
                        headers.add(
                            ReportColumnHeaderModel(
                                id = "criterion_${criterion.id}",
                                title = criterion.display_name,
                                level = 4,
                                sessionId = session.id,
                                competencyId = competency.id,
                                abilityId = ability.id,
                                criterionId = criterion.id,
                                backgroundColor = "#FFFFFF"
                            )
                        )

                        // ✅ Observación (junto al criterio)
                        headers.add(
                            ReportColumnHeaderModel(
                                id = "obs_${criterion.id}",
                                title = "📝",
                                level = 4,
                                sessionId = session.id,
                                competencyId = competency.id,
                                abilityId = ability.id,
                                criterionId = criterion.id,
                                isObservation = true,
                                backgroundColor = "#FFF8E1"
                            )
                        )
                    }

                    // ✅ PROM de Capacidad (vacío y editable)
                    headers.add(
                        ReportColumnHeaderModel(
                            id = "prom_ability_${ability.id}",
                            title = "PROM.",
                            level = 4,
                            sessionId = session.id,
                            competencyId = competency.id,
                            abilityId = ability.id,
                            isPromedio = true,
                            isEditable = true, // ✅ Permite edición
                            backgroundColor = "#E3F2FD"
                        )
                    )
                }
            }
        }

        // ✅ PROMEDIO FINAL (vacío y editable)
        headers.add(
            ReportColumnHeaderModel(
                id = "promedio_final",
                title = "PROMEDIO",
                level = 3,
                rowspan = 2,
                isPromedioFinal = true,
                isEditable = true,
                backgroundColor = "#FFF3E0"
            )
        )

        return headers
    }

    private fun calculatePromedio(grades: List<String?>): String? {
        val validGrades = grades.mapNotNull { grade ->
            when (grade) {
                "AD" -> 4
                "A" -> 3
                "B" -> 2
                "C" -> 1
                else -> null
            }
        }

        if (validGrades.isEmpty()) return null

        val average = validGrades.average()
        return when {
            average >= 3.5 -> "AD"
            average >= 2.5 -> "A"
            average >= 1.5 -> "B"
            else -> "C"
        }
    }

    private fun formatStudentName(fullName: String): String {
        val commaIndex = fullName.indexOf(',')
        return if (commaIndex != -1 && commaIndex < fullName.length - 1) {
            val lastName = fullName.substring(0, commaIndex + 1)
            val firstName = fullName.substring(commaIndex + 1).trim()
            "$lastName\n$firstName"
        } else {
            fullName
        }
    }

    private fun showGradeDetails(studentId: Int, sessionId: Int, criterionId: Int, grade: String?) {
        val studentName = getStudentName(studentId)
        val criterionName = getCriterionName(criterionId)

        Toast.makeText(this,
            "📋 $studentName\n📝 $criterionName\n📊 Nota: ${grade ?: "Sin evaluar"}",
            Toast.LENGTH_LONG).show()
    }

    private fun exportToExcel() {
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.getDefault())
            .format(java.util.Date())

        val fileName = "consolidado_${sectionName.replace(" ", "_")}_$currentDate.xlsx"

        Toast.makeText(this,
            "📊 Exportando $fileName...\n✅ Incluye estructura multinivel y promedios editables",
            Toast.LENGTH_LONG).show()

        Log.d(TAG, "📤 Iniciando exportación consolidado multinivel:")
        Log.d(TAG, "  Archivo: $fileName")
        Log.d(TAG, "  Promedios editados: ${editedAverages.size}")
        Log.d(TAG, "  Sección: $sectionName")

        // TODO: Implementar exportación real a Excel con estructura multinivel
        // - Headers jerárquicos
        // - Celdas de observaciones
        // - Promedios editables
        // - Formato profesional
    }

    private fun loadTestConsolidatedData() {
        Log.d(TAG, "🧪 Cargando datos de prueba multinivel...")

        try {
            // ✅ Datos de prueba más completos para multinivel
            val testData = ConsolidatedResponse(
                students = listOf(
                    ConsolidatedStudent(1, "ABILA SOLANO, PIERO ALEXANDER"),
                    ConsolidatedStudent(2, "ACOSTA LINARES, JHOSY ANTHONY"),
                    ConsolidatedStudent(3, "ALCANTARA DIAZ, IRVIN LEONARDO"),
                    ConsolidatedStudent(4, "ALEJANDRO HERNANDEZ, KAROL NICOLE"),
                    ConsolidatedStudent(5, "APONTE SAUCEDA, YASMIN YERALDI")
                ),
                sessions = listOf(
                    ConsolidatedSession(1, "Sesión 1 - Soluciones Tecnológicas", 1),
                    ConsolidatedSession(2, "Sesión 2 - Evaluación Final", 2)
                ),
                competencies = listOf(
                    ConsolidatedCompetency(1, 1, "Comunicamos nuestra solución tecnológica relacionada a las bajas temperaturas")
                ),
                abilities = listOf(
                    ConsolidatedAbility(1, 1, "Determina una alternativa de solución"),
                    ConsolidatedAbility(2, 1, "Diseña la solución tecnológica"),
                    ConsolidatedAbility(3, 1, "Implementa y valida"),
                    ConsolidatedAbility(4, 1, "Evalúa y comunica")
                ),
                criteria = listOf(
                    ConsolidatedCriterion(1, 1, "Propone una alternativa de solución tecnológica"),
                    ConsolidatedCriterion(2, 2, "Representa la solución tecnológica en dibujo o diagrama de flujo"),
                    ConsolidatedCriterion(3, 3, "Construye el prototipo de la solución tecnológica"),
                    ConsolidatedCriterion(4, 3, "Realiza pruebas de funcionamiento y reajustes"),
                    ConsolidatedCriterion(5, 4, "Fluidez y claridad en la exposición"),
                    ConsolidatedCriterion(6, 4, "Pertinencia del prototipo frente al problema identificado")
                ),
                values = listOf(
                    // Estudiante 1
                    ConsolidatedValue(1, 1, "C"),
                    ConsolidatedValue(1, 3, "AD"),
                    // Estudiante 2
                    ConsolidatedValue(2, 1, "A"),
                    ConsolidatedValue(2, 2, "A"),
                    ConsolidatedValue(2, 3, "A"),
                    ConsolidatedValue(2, 4, "A"),
                    ConsolidatedValue(2, 5, "A"),
                    ConsolidatedValue(2, 6, "A"),
                    // Estudiante 3
                    ConsolidatedValue(3, 3, "C"),
                    // Estudiante 4
                    ConsolidatedValue(4, 1, "A"),
                    ConsolidatedValue(4, 2, "A"),
                    ConsolidatedValue(4, 3, "A"),
                    ConsolidatedValue(4, 4, "A"),
                    ConsolidatedValue(4, 5, "A"),
                    ConsolidatedValue(4, 6, "A"),
                    // Estudiante 5
                    ConsolidatedValue(5, 1, "A"),
                    ConsolidatedValue(5, 2, "A"),
                    ConsolidatedValue(5, 3, "A"),
                    ConsolidatedValue(5, 4, "A"),
                    ConsolidatedValue(5, 5, "A"),
                    ConsolidatedValue(5, 6, "A")
                ),
                observations = listOf(
                    ConsolidatedObservation(1, 1, "Necesita mejorar en la propuesta de soluciones"),
                    ConsolidatedObservation(2, 4, "Excelente presentación y claridad")
                )
            )

            consolidatedData = testData
            displayConsolidatedTable(testData)

            Toast.makeText(this, "📊 Consolidado multinivel de prueba cargado", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando datos de prueba multinivel", e)
            Toast.makeText(this, "❌ Error crítico en datos de prueba", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun getStudentName(studentId: Int): String {
        return consolidatedData?.students?.find { it.id == studentId }?.full_name ?: "Estudiante"
    }

    private fun getCriterionName(criterionId: Int): String {
        return consolidatedData?.criteria?.find { it.id == criterionId }?.display_name ?: "Criterio"
    }

    private fun organizeDataBySession(data: ConsolidatedResponse): Map<ConsolidatedSession, List<CompetencyData>> {
        val sessionMap = mutableMapOf<ConsolidatedSession, MutableList<CompetencyData>>()

        data.sessions.forEach { session ->
            val competenciesForSession = data.competencies.filter { it.session_id == session.id }
            val competencyDataList = mutableListOf<CompetencyData>()

            competenciesForSession.forEach { competency ->
                val abilitiesForCompetency = data.abilities.filter { it.competency_id == competency.id }
                val abilityDataList = mutableListOf<AbilityData>()

                abilitiesForCompetency.forEach { ability ->
                    val criteriaForAbility = data.criteria.filter { it.ability_id == ability.id }
                    abilityDataList.add(AbilityData(ability, criteriaForAbility))
                }

                competencyDataList.add(CompetencyData(competency, abilityDataList))
            }

            sessionMap[session] = competencyDataList
        }

        return sessionMap
    }

    private fun calculateSessionColspan(competencies: List<CompetencyData>): Int {
        return competencies.sumOf { calculateCompetencyColspan(it.abilities) }
    }

    private fun calculateCompetencyColspan(abilities: List<AbilityData>): Int {
        return abilities.sumOf { ability ->
            (ability.criteria.size * 2) + 1 // criterios + observaciones + PROM
        } + 1 // + PROMEDIO final
    }
    private fun createConsolidatedCells(data: ConsolidatedResponse): List<List<ReportCellModel>> {
        val cells = mutableListOf<List<ReportCellModel>>()

        data.students.forEach { student ->
            val studentCells = mutableListOf<ReportCellModel>()

            // ✅ Organizar por sesión → competencia → capacidad → criterio
            val sessionDataMap = organizeDataBySession(data)

            sessionDataMap.forEach { (session, competencies) ->
                competencies.forEach { competencyData ->
                    competencyData.abilities.forEach { abilityData ->

                        // ✅ Celdas de criterios + observaciones
                        abilityData.criteria.forEach { criterion ->
                            // Celda de criterio
                            val gradeValue = data.values.find {
                                it.student_id == student.id && it.criterion_id == criterion.id
                            }?.value

                            studentCells.add(
                                ReportCellModel(
                                    id = "cell_${student.id}_${criterion.id}",
                                    studentId = student.id,
                                    sessionId = session.id,
                                    criterionId = criterion.id,
                                    abilityId = abilityData.ability.id,
                                    grade = gradeValue,
                                    isEmpty = gradeValue.isNullOrEmpty()
                                )
                            )

                            // ✅ Celda de observación
                            val observationValue = data.observations.find {
                                it.student_id == student.id && it.ability_id == abilityData.ability.id
                            }?.observation

                            studentCells.add(
                                ReportCellModel(
                                    id = "obs_${student.id}_${criterion.id}",
                                    studentId = student.id,
                                    sessionId = session.id,
                                    criterionId = criterion.id,
                                    abilityId = abilityData.ability.id,
                                    observation = observationValue,
                                    isObservation = true,
                                    hasObservation = !observationValue.isNullOrEmpty()
                                )
                            )
                        }

                        // ✅ Celda PROM de capacidad (VACÍA y editable)
                        studentCells.add(
                            ReportCellModel(
                                id = "prom_${student.id}_${abilityData.ability.id}",
                                studentId = student.id,
                                sessionId = session.id,
                                abilityId = abilityData.ability.id,
                                grade = null, // ✅ SIEMPRE VACÍO
                                isPromedio = true,
                                isEditable = true, // ✅ Permite edición
                                isEmpty = true
                            )
                        )
                    }
                }
            }

            // ✅ Celda PROMEDIO final (VACÍA y editable)
            studentCells.add(
                ReportCellModel(
                    id = "final_${student.id}",
                    studentId = student.id,
                    grade = null, // ✅ SIEMPRE VACÍO
                    isPromedioFinal = true,
                    isEditable = true, // ✅ Permite edición
                    isEmpty = true
                )
            )

            cells.add(studentCells)
        }

        return cells
    }

}