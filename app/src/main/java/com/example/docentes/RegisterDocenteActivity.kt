package com.example.docentes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.docentes.dialogs.GoogleSignUpDialogFragment
import com.example.docentes.models.ErrorResponse
import com.example.docentes.models.RegisterDocenteRequest
import com.example.docentes.models.ReniecData
import com.example.docentes.models.ReniecRequest
import com.example.docentes.network.ReniecClient
import com.example.docentes.network.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterDocenteActivity : AppCompatActivity(),
    GoogleSignUpDialogFragment.GoogleSignUpListener {

    companion object {
        private const val TAG = "RegisterDocenteActivity"
        private const val RC_SIGN_IN = 9005
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var etDNI: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var actvArea: AutoCompleteTextView
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etPasswordConfirm: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnGoogleSignUp: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var llValidating: LinearLayout
    private lateinit var llValidationStatus: LinearLayout
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvReniecStatus: TextView

    private var reniecData: ReniecData? = null
    private var selectedAreaId: Int? = null
    private var selectedAreaName: String? = null

    // Variables temporales para Google Sign-Up
    private var pendingGoogleAccount: GoogleSignInAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_docente)

        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        initViews()
        loadAreasFromAPI()
        setupListeners()
    }

    private fun initViews() {
        etDNI = findViewById(R.id.etDNI)
        etFullName = findViewById(R.id.etFullName)
        actvArea = findViewById(R.id.actvArea)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp)
        progressBar = findViewById(R.id.progressBar)
        llValidating = findViewById(R.id.llValidating)
        llValidationStatus = findViewById(R.id.llValidationStatus)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        tvReniecStatus = findViewById(R.id.tvReniecStatus)

        findViewById<TextView>(R.id.tvGoToLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadAreasFromAPI() {
        lifecycleScope.launch {
            try {
                val areas = withContext(Dispatchers.IO) {
                    RetrofitClient.curriculumApiService.getAreas()
                }

                withContext(Dispatchers.Main) {
                    if (areas.isNotEmpty()) {
                        Log.d(TAG, "✅ ${areas.size} áreas cargadas")

                        val areaNames = areas.map { it.nombre }.toTypedArray()

                        val adapter = ArrayAdapter(
                            this@RegisterDocenteActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            areaNames
                        )
                        actvArea.setAdapter(adapter)

                        actvArea.setOnItemClickListener { _, _, position, _ ->
                            selectedAreaId = areas[position].id
                            selectedAreaName = areas[position].nombre
                            Log.d(TAG, "✅ Área seleccionada: $selectedAreaName (ID: $selectedAreaId)")
                        }
                    } else {
                        Log.e(TAG, "❌ No hay áreas disponibles")
                        Toast.makeText(
                            this@RegisterDocenteActivity,
                            "No hay áreas disponibles. Contacta al administrador.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cargar áreas", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RegisterDocenteActivity,
                        "Error cargando áreas: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupListeners() {
        // Auto-validar DNI (para registro con email)
        etDNI.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                val dni = s.toString().trim()
                llValidationStatus.visibility = View.GONE
                etFullName.setText("")
                reniecData = null

                if (dni.length == 8) {
                    validateDNIWithReniec(dni)
                }
            }
        })

        btnRegister.setOnClickListener {
            registerWithEmail()
        }

        // Google Sign-Up directo (el modal valida DNI + área)
        btnGoogleSignUp.setOnClickListener {
            signUpWithGoogle()
        }
    }

    private fun validateDNIWithReniec(dni: String) {
        llValidating.visibility = View.VISIBLE
        llValidationStatus.visibility = View.GONE
        etFullName.setText("")
        etDNI.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = ReniecRequest(dni = dni)
                val response = ReniecClient.apiService.validateDNI(
                    token = ReniecClient.getToken(),
                    request = request
                )

                withContext(Dispatchers.Main) {
                    llValidating.visibility = View.GONE
                    llValidationStatus.visibility = View.VISIBLE
                    etDNI.isEnabled = true

                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()!!.data!!
                        reniecData = data

                        etFullName.setText(data.nombreCompleto)
                        etFullName.isEnabled = false

                        ivStatusIcon.setImageResource(android.R.drawable.presence_online)
                        ivStatusIcon.setColorFilter(getColor(android.R.color.holo_green_dark))

                        tvReniecStatus.text = "✅ DNI validado correctamente"
                        tvReniecStatus.setTextColor(getColor(android.R.color.holo_green_dark))

                        llValidationStatus.setBackgroundColor(getColor(android.R.color.holo_green_light))

                        Log.d(TAG, "✅ DNI validado: ${data.nombreCompleto}")
                    } else {
                        showError("DNI no encontrado en RENIEC")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    llValidating.visibility = View.GONE
                    llValidationStatus.visibility = View.VISIBLE
                    etDNI.isEnabled = true
                    showError("Error de conexión: ${e.message}")
                    Log.e(TAG, "❌ Error validando DNI", e)
                }
            }
        }
    }

    private fun showError(message: String) {
        ivStatusIcon.setImageResource(android.R.drawable.presence_busy)
        ivStatusIcon.setColorFilter(getColor(android.R.color.holo_red_dark))

        tvReniecStatus.text = "❌ $message"
        tvReniecStatus.setTextColor(getColor(android.R.color.holo_red_dark))

        llValidationStatus.setBackgroundColor(getColor(android.R.color.holo_red_light))
        etFullName.setText("")
    }

    private fun registerWithEmail() {
        val dni = etDNI.text.toString().trim()
        val fullName = etFullName.text.toString().trim()
        val area = actvArea.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val passwordConfirm = etPasswordConfirm.text.toString().trim()

        if (!validateInput(dni, fullName, area, email, password, passwordConfirm)) {
            return
        }

        showLoading(true)

        // ✅ Crear cuenta en Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            Log.d(TAG, "📧 Correo de verificación enviado")
                        }
                    }

                    user?.getIdToken(true)?.addOnSuccessListener { result ->
                        val token = result.token
                        // ✅ Registrar en backend INMEDIATAMENTE
                        registerUserInBackend(token, email, fullName, dni, area)
                    }?.addOnFailureListener { e ->
                        showLoading(false)
                        Log.e(TAG, "❌ Error obteniendo token", e)
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()

                        // Eliminar cuenta de Firebase si falló
                        user?.delete()
                    }
                } else {
                    showLoading(false)
                    Log.e(TAG, "❌ Error creando cuenta Firebase", task.exception)
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun validateInput(
        dni: String,
        fullName: String,
        area: String,
        email: String,
        password: String,
        passwordConfirm: String
    ): Boolean {
        if (dni.length != 8) {
            Toast.makeText(this, "Ingresa un DNI válido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (reniecData == null) {
            Toast.makeText(this, "Debes validar tu DNI con RENIEC primero", Toast.LENGTH_SHORT).show()
            return false
        }

        if (area.isEmpty() || selectedAreaId == null) {
            Toast.makeText(this, "Selecciona tu área", Toast.LENGTH_SHORT).show()
            actvArea.requestFocus()
            return false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Correo inválido"
            return false
        }

        if (password.isEmpty() || password.length < 6) {
            etPassword.error = "Mínimo 6 caracteres"
            return false
        }

        if (password != passwordConfirm) {
            etPasswordConfirm.error = "Las contraseñas no coinciden"
            return false
        }

        return true
    }

    /**
     * ✅ Inicia Google Sign-In (el modal valida DNI + área)
     */
    private fun signUpWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.result
                Log.d(TAG, "✅ Google Sign-In exitoso: ${account.email}")

                // Guardar account y mostrar dialog
                pendingGoogleAccount = account
                showGoogleSignUpDialog(account)

            } catch (e: ApiException) {
                Log.w(TAG, "❌ Google sign in failed", e)
                Toast.makeText(this, "Error con Google: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * ✅ Muestra el diálogo para completar DNI y área
     */
    private fun showGoogleSignUpDialog(account: GoogleSignInAccount) {
        val dialog = GoogleSignUpDialogFragment.newInstance(
            email = account.email ?: "",
            displayName = account.displayName ?: ""
        )
        dialog.setListener(this)
        dialog.show(supportFragmentManager, "GoogleSignUpDialog")
    }

    /**
     * ✅ Callback: Usuario completó el formulario del modal
     * AHORA validamos el área antes de autenticar con Firebase
     */
    override fun onGoogleSignUpComplete(dni: String, fullName: String, areaId: Int, areaName: String) {
        val account = pendingGoogleAccount
        if (account == null) {
            Toast.makeText(this, "Error: Cuenta de Google no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "📋 Datos validados del modal:")
        Log.d(TAG, "  - DNI: $dni")
        Log.d(TAG, "  - Nombre: $fullName")
        Log.d(TAG, "  - Área: $areaName")
        Log.d(TAG, "  - Area ID: $areaId ✅")

        // ✅ Validación adicional
        if (areaId <= 0) {
            Log.e(TAG, "❌ Area ID inválido: $areaId")
            Toast.makeText(this, "Error: ID de área inválido. Por favor reintenta.", Toast.LENGTH_LONG).show()
            return
        }

        // ✅ VALIDAR que el área exista ANTES de autenticar Firebase
        validateAreaBeforeFirebaseAuth(account, dni, fullName, areaId, areaName)
    }

    /**
     * ✅ NUEVO: Valida que el área exista antes de crear cuenta Firebase
     */
    private fun validateAreaBeforeFirebaseAuth(
        account: GoogleSignInAccount,
        dni: String,
        fullName: String,
        areaId: Int,
        areaName: String
    ) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Verificar que el área exista en el backend
                Log.d(TAG, "🔍 Verificando área con ID: $areaId")

                val area = withContext(Dispatchers.IO) {
                    RetrofitClient.curriculumApiService.getArea(areaId)
                }

                Log.d(TAG, "✅ Área verificada: ${area.nombre}")

                withContext(Dispatchers.Main) {
                    // ✅ Área OK, proceder con Firebase
                    firebaseAuthWithGoogle(
                        idToken = account.idToken!!,
                        dni = dni,
                        fullName = fullName,
                        areaId = areaId,
                        areaName = area.nombre, // Usar el nombre del backend
                        email = account.email ?: ""
                    )
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Log.e(TAG, "❌ Error validando área", e)

                    val errorMsg = when {
                        e.message?.contains("404") == true ->
                            "El área seleccionada no existe en el sistema.\n\nPor favor contacta al administrador."
                        e.message?.contains("timeout") == true ->
                            "Error de conexión. Verifica tu internet."
                        else ->
                            "Error validando área: ${e.message}"
                    }

                    androidx.appcompat.app.AlertDialog.Builder(this@RegisterDocenteActivity)
                        .setTitle("❌ Error de Validación")
                        .setMessage(errorMsg)
                        .setPositiveButton("Reintentar") { _, _ ->
                            // Volver a mostrar el modal
                            showGoogleSignUpDialog(account)
                        }
                        .setNegativeButton("Cancelar") { _, _ ->
                            pendingGoogleAccount = null
                            googleSignInClient.signOut()
                        }
                        .show()
                }
            }
        }
    }

    /**
     * ✅ Callback: Usuario canceló el modal
     */
    override fun onGoogleSignUpCancelled() {
        pendingGoogleAccount = null
        googleSignInClient.signOut()
        Toast.makeText(this, "Registro cancelado", Toast.LENGTH_SHORT).show()
    }

    private fun firebaseAuthWithGoogle(
        idToken: String,
        dni: String,
        fullName: String,
        areaId: Int,
        areaName: String,
        email: String
    ) {
        showLoading(true)

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user == null) {
                        showLoading(false)
                        Toast.makeText(this, "Error: Usuario no disponible", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    Log.d(TAG, "✅ Firebase auth exitoso: ${user.uid}")

                    user.getIdToken(true).addOnSuccessListener { result ->
                        val token = result.token
                        // ✅ Registrar en backend INMEDIATAMENTE
                        registerUserInBackend(token, email, fullName, dni, areaName, areaId)
                    }.addOnFailureListener { e ->
                        showLoading(false)
                        Log.e(TAG, "❌ Error obteniendo token", e)
                        Toast.makeText(this, "Error obteniendo token: ${e.message}", Toast.LENGTH_SHORT).show()

                        // Eliminar cuenta si falló
                        user.delete()
                        googleSignInClient.signOut()
                    }
                } else {
                    showLoading(false)
                    Log.e(TAG, "❌ Firebase auth fallida", task.exception)
                    Toast.makeText(this, "Autenticación fallida: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    googleSignInClient.signOut()
                }
            }
    }

    private fun registerUserInBackend(
        token: String?,
        email: String,
        fullName: String,
        dni: String,
        areaName: String,
        areaId: Int? = selectedAreaId
    ) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val user = auth.currentUser
                if (user == null) {
                    showLoading(false)
                    Toast.makeText(this@RegisterDocenteActivity, "Error: usuario no autenticado", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // ✅ VALIDACIÓN: Asegurar que areaId NO sea null
                if (areaId == null) {
                    showLoading(false)
                    Log.e(TAG, "❌ ERROR CRÍTICO: areaId es null")
                    Toast.makeText(
                        this@RegisterDocenteActivity,
                        "Error: Área no seleccionada correctamente. Por favor reintenta.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Eliminar cuenta de Firebase
                    user.delete()
                    googleSignInClient.signOut()
                    return@launch
                }

                val request = RegisterDocenteRequest(
                    dni = dni,
                    fullName = fullName,
                    areaName = areaName,
                    areaId = areaId,  // ✅ Garantizado no-null
                    email = email,
                    firebaseUid = user.uid,
                    employeeCode = null,
                    specialization = null
                )

                Log.d(TAG, "📤 Enviando registro al backend:")
                Log.d(TAG, "  - DNI: $dni")
                Log.d(TAG, "  - Email: $email")
                Log.d(TAG, "  - Full Name: $fullName")
                Log.d(TAG, "  - Área: $areaName")
                Log.d(TAG, "  - Area ID: $areaId ✅")
                Log.d(TAG, "  - Firebase UID: ${user.uid}")

                // ✅ Log del JSON que se enviará
                val gson = com.google.gson.Gson()
                Log.d(TAG, "📦 JSON Request:\n${gson.toJson(request)}")

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.registerDocente(request)
                }

                Log.d(TAG, "📥 Respuesta del servidor: Code ${response.code()}")

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d(TAG, "✅ Registro exitoso en backend")
                        val isGoogleFlow = pendingGoogleAccount != null

                        val message = if (isGoogleFlow) {
                            "Tu cuenta de docente ha sido creada exitosamente.\n\n✅ Registro completado con Google"
                        } else {
                            "Tu cuenta de docente ha sido creada exitosamente.\n\n📧 Hemos enviado un correo de verificación a:\n$email\n\nPor favor verifica tu correo antes de iniciar sesión."
                        }

                        androidx.appcompat.app.AlertDialog.Builder(this@RegisterDocenteActivity)
                            .setTitle("✅ Cuenta Creada")
                            .setMessage(message)
                            .setPositiveButton("Ir a Login") { _, _ ->
                                val intent = Intent(this@RegisterDocenteActivity, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "❌ Error del servidor: $errorBody")

                        val errorResponse = try {
                            com.google.gson.Gson().fromJson(errorBody, ErrorResponse::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando respuesta de error", e)
                            null
                        }

                        val errorMessage = errorResponse?.error ?: "Error en el registro (${response.code()})"
                        val errorDetails = errorResponse?.details

                        Log.e(TAG, "Error message: $errorMessage")
                        Log.e(TAG, "Error details: $errorDetails")

                        // ❌ SI FALLA EL BACKEND, ELIMINAR CUENTA DE FIREBASE
                        user.delete().addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                Log.d(TAG, "🗑️ Cuenta de Firebase eliminada tras fallo en backend")
                            }
                        }
                        googleSignInClient.signOut()

                        androidx.appcompat.app.AlertDialog.Builder(this@RegisterDocenteActivity)
                            .setTitle("❌ Error en el Registro")
                            .setMessage("$errorMessage${if (errorDetails != null) "\n\n$errorDetails" else ""}")
                            .setPositiveButton("Reintentar") { _, _ ->
                                // Usuario puede reintentar
                            }
                            .show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Log.e(TAG, "❌ Error de conexión/excepción", e)

                    // ❌ SI HAY EXCEPCIÓN, ELIMINAR CUENTA DE FIREBASE
                    val user = auth.currentUser
                    user?.delete()?.addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Log.d(TAG, "🗑️ Cuenta de Firebase eliminada tras excepción")
                        }
                    }
                    googleSignInClient.signOut()

                    Toast.makeText(
                        this@RegisterDocenteActivity,
                        "Error de conexión: ${e.message}\n\nIntenta nuevamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !show
        btnGoogleSignUp.isEnabled = !show
    }
}