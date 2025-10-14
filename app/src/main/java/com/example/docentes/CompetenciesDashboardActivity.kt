package com.example.docentes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.docentes.adapters.CompetenciesDashboardAdapter
import com.example.docentes.adapters.CompetenciasTemplateAdapter
import com.example.docentes.models.*
import com.example.docentes.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

class CompetenciesDashboardActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CompetenciesDashboard"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etBuscarCompetencias: TextInputEditText
    private lateinit var spinnerAreas: Spinner
    private lateinit var cardAgregarCompetencia: MaterialCardView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvCompetencies: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: CompetenciesDashboardAdapter

    private var sessionId: Int = -1
    private var sessionTitle: String = ""
    private var areas: List<Area> = emptyList()
    private var todasCompetenciasTemplate: List<CompetenciaTemplate> = emptyList()
    private var competenciasActuales: List<Competency> = emptyList()
    private var competencyAbilities: Map<Int, List<Ability>> = emptyMap()
    private var abilityCriteria: Map<Int, List<Criterion>> = emptyMap()

    // Estados de expansión
    private var expandedCompetencyId: Int? = null
    private var expandedAbilityId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_competencies_dashboard)

        initViews()
        sessionId = intent.getIntExtra("SESSION_ID", -1)
        sessionTitle = intent.getStringExtra("SESSION_TITLE") ?: "Competencias"

        if (sessionId == -1) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupSearchAndFilters()
        setupButtons()
        loadInitialData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        etBuscarCompetencias = findViewById(R.id.etBuscarCompetencias)
        spinnerAreas = findViewById(R.id.spinnerAreas)
        cardAgregarCompetencia = findViewById(R.id.cardAgregarCompetencia)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvCompetencies = findViewById(R.id.rvCompetencies)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        toolbar.title = "Competencias - $sessionTitle"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = CompetenciesDashboardAdapter(
            competencies = emptyList(),
            competencyAbilities = emptyMap(),
            abilityCriteria = emptyMap(),
            expandedCompetencyId = expandedCompetencyId,
            expandedAbilityId = expandedAbilityId,
            onCompetencyExpanded = { competencyId, isExpanded ->
                Log.d(TAG, "Competency expanded: $competencyId, expanded: $isExpanded")
                expandedCompetencyId = if (isExpanded) competencyId else null
                if (!isExpanded) expandedAbilityId = null
                loadAbilitiesForCompetency(competencyId)
            },
            onCompetencyOptionsClick = { competency, view ->
                showCompetencyOptionsMenu(competency, view)
            },
            onAddAbility = { competencyId ->
                showAddAbilityDialog(competencyId)
            },
            onAbilityExpanded = { abilityId, isExpanded ->
                Log.d(TAG, "Ability expanded: $abilityId, expanded: $isExpanded")
                expandedAbilityId = if (isExpanded) abilityId else null
                loadCriteriaForAbility(abilityId)
            },
            onAbilityOptionsClick = { ability, view ->
                showAbilityOptionsMenu(ability, view)
            },
            onAddCriterion = { abilityId ->
                showAddCriterionDialog(abilityId)
            },
            onCriterionOptionsClick = { criterion, view ->
                showCriterionOptionsMenu(criterion, view)
            }
        )
        rvCompetencies.layoutManager = LinearLayoutManager(this)
        rvCompetencies.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadCompetencies()
        }
    }

    private fun setupSearchAndFilters() {
        etBuscarCompetencias.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterCompetencies()
            }
        })
    }

    private fun setupButtons() {
        cardAgregarCompetencia.setOnClickListener {
            showSelectCompetencyDialog()
        }
    }

    private fun loadInitialData() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val areasDeferred = async { RetrofitClient.curriculumApiService.getAreas() }
                val competenciasTemplateDeferred = async {
                    RetrofitClient.curriculumApiService.getAllCompetencias()
                }

                areas = areasDeferred.await()
                todasCompetenciasTemplate = competenciasTemplateDeferred.await()

                Log.d(TAG, "Areas cargadas: ${areas.size}")
                Log.d(TAG, "Competencias template: ${todasCompetenciasTemplate.size}")

                setupAreaSpinner()
                loadCompetencies()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial data", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al cargar datos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupAreaSpinner() {
        val areasWithAll = listOf(Area(0, "Todas las áreas")) + areas
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            areasWithAll.map { it.nombre }
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAreas.adapter = spinnerAdapter

        spinnerAreas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterCompetencies()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadCompetencies() {
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                competenciasActuales = RetrofitClient.apiService.getCompetencies(sessionId)
                Log.d(TAG, "Competencias actuales: ${competenciasActuales.size}")

                competencyAbilities = emptyMap()
                abilityCriteria = emptyMap()

                updateAdapter()

                if (competenciasActuales.isEmpty()) {
                    Toast.makeText(
                        this@CompetenciesDashboardActivity,
                        "No hay competencias. Agrega una del catálogo.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading competencies", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al cargar competencias: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun filterCompetencies() {
        val searchText = etBuscarCompetencias.text.toString().lowercase()
        val selectedAreaPosition = spinnerAreas.selectedItemPosition

        val filteredCompetencies = competenciasActuales.filter { competency ->
            val matchesSearch = searchText.isEmpty() ||
                    competency.name.lowercase().contains(searchText) ||
                    competency.description?.lowercase()?.contains(searchText) == true

            val matchesArea = selectedAreaPosition == 0 ||
                    competency.description?.contains(areas[selectedAreaPosition - 1].nombre) == true

            matchesSearch && matchesArea
        }

        val filteredAbilities = competencyAbilities.filterKeys { competencyId ->
            filteredCompetencies.any { it.id == competencyId }
        }

        adapter.updateData(
            newCompetencies = filteredCompetencies,
            newAbilities = filteredAbilities,
            newCriteria = abilityCriteria,
            keepExpanded = true
        )
    }

    private fun updateAdapter() {
        adapter.updateData(
            newCompetencies = competenciasActuales,
            newAbilities = competencyAbilities,
            newCriteria = abilityCriteria,
            keepExpanded = true
        )
        adapter.updateExpandedStates(expandedCompetencyId, expandedAbilityId)
    }

    private fun loadAbilitiesForCompetency(competencyId: Int) {
        lifecycleScope.launch {
            try {
                val abilities = RetrofitClient.apiService.getAbilities(competencyId)
                Log.d(TAG, "Abilities loaded for competency $competencyId: ${abilities.size}")

                competencyAbilities = competencyAbilities.toMutableMap().apply {
                    put(competencyId, abilities)
                }

                updateAdapter()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading abilities for competency $competencyId", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al cargar capacidades: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadCriteriaForAbility(abilityId: Int) {
        lifecycleScope.launch {
            try {
                val criteria = RetrofitClient.apiService.getCriteria(abilityId)
                Log.d(TAG, "Criteria loaded for ability $abilityId: ${criteria.size}")

                abilityCriteria = abilityCriteria.toMutableMap().apply {
                    put(abilityId, criteria)
                }

                updateAdapter()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading criteria for ability $abilityId", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al cargar criterios: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showSelectCompetencyDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_competencies, null)
        val rvCompetenciasDialog = dialogView.findViewById<RecyclerView>(R.id.rvCompetenciasDialog)
        val spinnerAreasDialog = dialogView.findViewById<Spinner>(R.id.spinnerAreasDialog)
        val areasWithAll = listOf(Area(0, "Todas las áreas")) + areas
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            areasWithAll.map { it.nombre }
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAreasDialog.adapter = spinnerAdapter
        val selectionAdapter = CompetenciasTemplateAdapter(
            competencias = todasCompetenciasTemplate,
            onSelectionChanged = { selectedCompetencias ->
                // Manejar selección
            }
        )
        rvCompetenciasDialog.layoutManager = LinearLayoutManager(this)
        rvCompetenciasDialog.adapter = selectionAdapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Agregar Seleccionadas") { _, _ ->
                val selected = selectionAdapter.getSelectedCompetencias()
                addSelectedCompetencies(selected)
            }
            .setNegativeButton("Cancelar", null)
            .create()

        spinnerAreasDialog.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedArea = areasWithAll[position]
                val filtered = if (selectedArea.id == 0) {
                    todasCompetenciasTemplate
                } else {
                    todasCompetenciasTemplate.filter { it.area_id == selectedArea.id }
                }
                selectionAdapter.updateCompetencias(filtered)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        dialog.show()
    }

    private fun addSelectedCompetencies(competencias: List<CompetenciaTemplate>) {
        if (competencias.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una competencia", Toast.LENGTH_SHORT).show()
            return
        }

        val competenciasExistentesNombres = competenciasActuales.map { it.name.lowercase() }
        val competenciasNuevas = competencias.filter { competencia ->
            !competenciasExistentesNombres.contains(competencia.nombre.lowercase())
        }
        val competenciasDuplicadas = competencias.filter { competencia ->
            competenciasExistentesNombres.contains(competencia.nombre.lowercase())
        }

        if (competenciasNuevas.isEmpty()) {
            val mensaje = if (competenciasDuplicadas.size == 1) {
                "⚠️ La competencia ya existe en esta sesión"
            } else {
                "⚠️ Las competencias seleccionadas ya existen en esta sesión"
            }
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            return
        }

        if (competenciasDuplicadas.isNotEmpty()) {
            val mensaje = "⚠️ ${competenciasDuplicadas.size} competencia(s) ya existe(n), se agregarán ${competenciasNuevas.size} nueva(s)"
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        }

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                var savedCount = 0
                var errorCount = 0

                for (competencia in competenciasNuevas) { // ← Solo las nuevas
                    try {
                        val request = NewCompetencyRequest(
                            name = competencia.nombre,
                            description = "Área: ${competencia.area_nombre}"
                        )
                        RetrofitClient.apiService.createCompetency(sessionId, request)
                        savedCount++
                        Log.d(TAG, "Competencia guardada: ${competencia.nombre}")
                    } catch (e: Exception) {
                        errorCount++
                        Log.e(TAG, "Error guardando competencia: ${competencia.nombre}", e)
                    }
                }

                val message = when {
                    errorCount == 0 && savedCount > 0 ->
                        "✅ $savedCount competencia(s) agregada(s) exitosamente"
                    errorCount > 0 && savedCount > 0 ->
                        "⚠️ $savedCount agregada(s), $errorCount con error(es)"
                    else ->
                        "❌ No se pudieron agregar las competencias"
                }

                Toast.makeText(this@CompetenciesDashboardActivity, message, Toast.LENGTH_LONG).show()

                if (savedCount > 0) {
                    loadCompetencies()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error general guardando competencias", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showAddAbilityDialog(competencyId: Int) {
        lifecycleScope.launch {
            try {
                val competencia = competenciasActuales.find { it.id == competencyId }
                if (competencia == null) {
                    Toast.makeText(this@CompetenciesDashboardActivity,
                        "Error: Competencia no encontrada", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val competenciaTemplate = todasCompetenciasTemplate.find {
                    it.nombre == competencia.name
                }

                if (competenciaTemplate == null) {
                    Toast.makeText(this@CompetenciesDashboardActivity,
                        "No se encontraron capacidades en el catálogo", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val capacidades = RetrofitClient.curriculumApiService
                    .getCapacidadesByCompetencia(competenciaTemplate.id)

                showCapacidadesSelectionDialog(competencyId, capacidades)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading capacidades", e)
                Toast.makeText(this@CompetenciesDashboardActivity,
                    "Error al cargar capacidades: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showCapacidadesSelectionDialog(competencyId: Int, capacidades: List<Capacidad>) {
        val capacidadNames = capacidades.map { it.nombre }.toTypedArray()
        val selectedItems = BooleanArray(capacidades.size)

        MaterialAlertDialogBuilder(this)
            .setTitle("Seleccionar Capacidades")
            .setMultiChoiceItems(capacidadNames, selectedItems) { _, which, isChecked ->
                selectedItems[which] = isChecked
            }
            .setPositiveButton("Agregar") { _, _ ->
                val selectedCapacidades = capacidades.filterIndexed { index, _ ->
                    selectedItems[index]
                }
                addSelectedCapacidades(competencyId, selectedCapacidades)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addSelectedCapacidades(competencyId: Int, capacidades: List<Capacidad>) {
        if (capacidades.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una capacidad", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val capacidadesExistentes = try {
                    RetrofitClient.apiService.getAbilities(competencyId)
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudieron obtener capacidades existentes", e)
                    emptyList()
                }

                val capacidadesExistentesNombres = capacidadesExistentes.map { it.name?.lowercase() }

                val capacidadesNuevas = capacidades.filter { capacidad ->
                    !capacidadesExistentesNombres.contains(capacidad.nombre.lowercase())
                }
                val capacidadesDuplicadas = capacidades.filter { capacidad ->
                    capacidadesExistentesNombres.contains(capacidad.nombre.lowercase())
                }

                if (capacidadesNuevas.isEmpty()) {
                    val mensaje = if (capacidadesDuplicadas.size == 1) {
                        "⚠️ La capacidad ya existe en esta competencia"
                    } else {
                        "⚠️ Las ${capacidadesDuplicadas.size} capacidades seleccionadas ya existen en esta competencia"
                    }
                    Toast.makeText(this@CompetenciesDashboardActivity, mensaje, Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    return@launch
                }

                if (capacidadesDuplicadas.isNotEmpty()) {
                    val mensaje = "⚠️ ${capacidadesDuplicadas.size} capacidad(es) ya existe(n), se agregarán ${capacidadesNuevas.size} nueva(s)"
                    Toast.makeText(this@CompetenciesDashboardActivity, mensaje, Toast.LENGTH_LONG).show()
                }

                var savedCount = 0
                var errorCount = 0

                for (capacidad in capacidadesNuevas) { // ← Solo las nuevas
                    try {
                        val request = mapOf(
                            "name" to capacidad.nombre,
                            "description" to "Capacidad curricular"
                        )
                        RetrofitClient.apiService.createAbility(competencyId, request)
                        savedCount++
                        Log.d(TAG, "Capacidad guardada: ${capacidad.nombre}")
                    } catch (e: Exception) {
                        errorCount++
                        Log.e(TAG, "Error guardando capacidad: ${capacidad.nombre}", e)
                    }
                }

                val message = when {
                    errorCount == 0 && savedCount > 0 ->
                        "✅ $savedCount capacidad(es) agregada(s) exitosamente"
                    errorCount > 0 && savedCount > 0 ->
                        "⚠️ $savedCount agregada(s), $errorCount con error(es)"
                    else ->
                        "❌ No se pudieron agregar las capacidades"
                }

                Toast.makeText(this@CompetenciesDashboardActivity, message, Toast.LENGTH_LONG).show()

                if (savedCount > 0) {
                    loadAbilitiesForCompetency(competencyId)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error general guardando capacidades", e)
                Toast.makeText(this@CompetenciesDashboardActivity,
                    "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showAddCriterionDialog(abilityId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_criterion, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etCriterionName)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etCriterionDescription)

        MaterialAlertDialogBuilder(this)
            .setTitle("Crear Criterio de Evaluación")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val name = etName.text.toString().trim()
                val description = etDescription.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "El nombre del criterio es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                createCriterion(abilityId, name, description)
            }
            .setNegativeButton("Cancelar", null)
            .show()

        etName.requestFocus()
    }

    private fun createCriterion(abilityId: Int, name: String, description: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // ✅ VALIDAR DUPLICADOS EN CRITERIOS
                val criteriosExistentes = try {
                    RetrofitClient.apiService.getCriteria(abilityId)
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudieron obtener criterios existentes", e)
                    emptyList()
                }

                val criterioExiste = criteriosExistentes.any {
                    it.name?.lowercase() == name.lowercase()
                }

                if (criterioExiste) {
                    Toast.makeText(
                        this@CompetenciesDashboardActivity,
                        "⚠️ Ya existe un criterio con el nombre \"$name\" en esta capacidad",
                        Toast.LENGTH_LONG
                    ).show()
                    progressBar.visibility = View.GONE
                    return@launch
                }

                // Crear criterio si no existe
                val request = mapOf(
                    "name" to name,
                    "description" to description
                )
                val newCriterion = RetrofitClient.apiService.createCriterion(abilityId, request)
                Log.d(TAG, "Criterion created: $newCriterion")

                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "✅ Criterio creado: $name",
                    Toast.LENGTH_SHORT
                ).show()

                loadCriteriaForAbility(abilityId)

            } catch (e: Exception) {
                Log.e(TAG, "Error creating criterion", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al crear criterio: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showCompetencyOptionsMenu(competency: Competency, view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_delete_only, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    showDeleteCompetencyDialog(competency)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showAbilityOptionsMenu(ability: Ability, view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_delete_only, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    showDeleteAbilityDialog(ability)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showCriterionOptionsMenu(criterion: Criterion, view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_criterion_options, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    showEditCriterionDialog(criterion)
                    true
                }
                R.id.action_delete -> {
                    showDeleteCriterionDialog(criterion)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteCompetencyDialog(competency: Competency) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Competencia") // ✅ Corregido: faltaba paréntesis de cierre
            .setMessage("¿Estás seguro de que deseas eliminar la competencia \"${competency.name}\"?\n\n⚠️ Esta competencia proviene del Currículo Nacional y se eliminará de esta sesión junto con todas sus capacidades y criterios asociados.")
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Eliminar") { _, _ ->
                deleteCompetency(competency.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteAbilityDialog(ability: Ability) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Capacidad")
            .setMessage("¿Estás seguro de que deseas eliminar la capacidad \"${ability.name}\"?\n\n⚠️ Esta capacidad proviene del Currículo Nacional y se eliminará junto con todos sus criterios asociados.")
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Eliminar") { _, _ ->
                deleteAbility(ability.id, ability.competency_id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteCriterionDialog(criterion: Criterion) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Criterio")
            .setMessage("¿Estás seguro de que deseas eliminar el criterio \"${criterion.name}\"?\n\nEste criterio fue creado manualmente y se eliminará permanentemente.")
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Eliminar") { _, _ ->
                deleteCriterion(criterion.id, criterion.ability_id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCompetency(competencyId: Int) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.deleteCompetency(competencyId)
                Log.d(TAG, "Competency deleted: $competencyId")

                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Competencia eliminada",
                    Toast.LENGTH_SHORT
                ).show()

                loadCompetencies()

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting competency", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al eliminar competencia: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun deleteAbility(abilityId: Int, competencyId: Int) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.deleteAbility(abilityId)
                Log.d(TAG, "Ability deleted: $abilityId")

                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Capacidad eliminada",
                    Toast.LENGTH_SHORT
                ).show()

                loadAbilitiesForCompetency(competencyId)

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting ability", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al eliminar capacidad: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun deleteCriterion(criterionId: Int, abilityId: Int) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.deleteCriterion(criterionId)
                Log.d(TAG, "Criterion deleted: $criterionId")

                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Criterio eliminado",
                    Toast.LENGTH_SHORT
                ).show()

                loadCriteriaForAbility(abilityId)

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting criterion", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al eliminar criterio: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showEditCriterionDialog(criterion: Criterion) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_criterion, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etCriterionName)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etCriterionDescription)

        etName.setText(criterion.name)
        etDescription.setText(criterion.description ?: "")

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Criterio de Evaluación")
            .setView(dialogView)
            .setPositiveButton("Guardar Cambios") { _, _ ->
                val name = etName.text.toString().trim()
                val description = etDescription.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "El nombre del criterio es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (name == criterion.name && description == (criterion.description ?: "")) {
                    Toast.makeText(this, "No se realizaron cambios", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateCriterion(criterion.id, criterion.ability_id, name, description)
            }
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Eliminar") { _, _ ->
                showDeleteCriterionDialog(criterion)
            }
            .show()

        etName.selectAll()
        etName.requestFocus()
    }

    private fun updateCriterion(criterionId: Int, abilityId: Int, name: String, description: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val criteriosExistentes = try {
                    RetrofitClient.apiService.getCriteria(abilityId)
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudieron obtener criterios existentes", e)
                    emptyList()
                }

                val criterioConMismoNombre = criteriosExistentes.find {
                    it.name?.lowercase() == name.lowercase() && it.id != criterionId
                }

                if (criterioConMismoNombre != null) {
                    Toast.makeText(
                        this@CompetenciesDashboardActivity,
                        "⚠️ Ya existe otro criterio con el nombre \"$name\" en esta capacidad",
                        Toast.LENGTH_LONG
                    ).show()
                    progressBar.visibility = View.GONE
                    return@launch
                }
                val request = UpdateCriterionRequest(
                    name = name,
                    description = if (description.isEmpty()) null else description
                )
                val updatedCriterion = RetrofitClient.apiService.updateCriterion(criterionId, request)
                Log.d(TAG, "Criterion updated: $updatedCriterion")

                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "✅ Criterio actualizado: $name",
                    Toast.LENGTH_SHORT
                ).show()

                loadCriteriaForAbility(abilityId)

            } catch (e: Exception) {
                Log.e(TAG, "Error updating criterion", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al actualizar criterio: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}