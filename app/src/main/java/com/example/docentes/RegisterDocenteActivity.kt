package com.example.docentes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.docentes.models.ErrorResponse
import com.example.docentes.models.RegisterDocenteRequest
import com.example.docentes.models.ReniecData
import com.example.docentes.models.ReniecRequest
import com.example.docentes.network.CurriculumApiService
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

class RegisterDocenteActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_docente)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Configure Google Sign In
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
    }

    private fun loadAreasFromAPI() {
        lifecycleScope.launch {
            try {
                val areas = withContext(Dispatchers.IO) {
                    RetrofitClient.curriculumApiService.getAreas()
                }

                withContext(Dispatchers.Main) {
                    if (areas.isNotEmpty()) {
                        val areaNames = areas.map { it.nombre }.toTypedArray()

                        val adapter = ArrayAdapter(
                            this@RegisterDocenteActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            areaNames
                        )
                        actvArea.setAdapter(adapter)

                        // Guardar el ID cuando se selecciona un área
                        actvArea.setOnItemClickListener { _, _, position, _ ->
                            selectedAreaId = areas[position].id
                            Log.d(
                                TAG,
                                "Área seleccionada: ${areas[position].nombre} (ID: ${areas[position].id})"
                            )
                        }

                    } else {
                        // Si la API devuelve una lista vacía
                        Toast.makeText(
                            this@RegisterDocenteActivity,
                            "No hay áreas disponibles en el servidor",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar áreas desde API", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RegisterDocenteActivity,
                        "Error al obtener áreas: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupListeners() {
        // Auto-validar DNI
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

                    if (response.isSuccessful && response.body() != null) {
                        val reniecResponse = response.body()!!

                        if (reniecResponse.success && reniecResponse.data != null) {
                            val data = reniecResponse.data
                            reniecData = data

                            etFullName.setText(data.nombreCompleto)
                            etFullName.isEnabled = false

                            ivStatusIcon.setImageResource(android.R.drawable.presence_online)
                            ivStatusIcon.setColorFilter(getColor(android.R.color.holo_green_dark))

                            tvReniecStatus.text = "✅ DNI validado correctamente"
                            tvReniecStatus.setTextColor(getColor(android.R.color.holo_green_dark))

                            llValidationStatus.setBackgroundColor(getColor(android.R.color.holo_green_light))

                            Log.d(TAG, "RENIEC Success: ${data.nombreCompleto}")
                        } else {
                            showError("DNI no encontrado en RENIEC")
                        }
                    } else {
                        val errorMessage = when (response.code()) {
                            401 -> "Token de API inválido"
                            404 -> "DNI no encontrado"
                            429 -> "Límite de consultas excedido"
                            else -> "Error ${response.code()}"
                        }
                        showError(errorMessage)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    llValidating.visibility = View.GONE
                    llValidationStatus.visibility = View.VISIBLE
                    etDNI.isEnabled = true
                    showError("Error de conexión: ${e.message}")
                    Log.e(TAG, "RENIEC Exception", e)
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

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Enviar verificación de correo
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            Log.d(TAG, "Correo de verificación enviado")
                        }
                    }

                    user?.getIdToken(true)?.addOnSuccessListener { result ->
                        val token = result.token
                        registerUserInBackend(token, email, fullName, dni, area)
                    }
                } else {
                    showLoading(false)
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

        if (area.isEmpty()) {
            Toast.makeText(this, "Selecciona tu área", Toast.LENGTH_SHORT).show()
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
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Error con Google: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.getIdToken(true)?.addOnSuccessListener { result ->
                        val token = result.token
                        val dni = etDNI.text.toString().trim()
                        val fullName = etFullName.text.toString().trim()
                        val area = actvArea.text.toString().trim()
                        registerUserInBackend(token, user.email ?: "", fullName, dni, area)
                    }
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUserInBackend(
        token: String?,
        email: String,
        fullName: String,
        dni: String,
        area: String
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

                // Preparar request
                val request = RegisterDocenteRequest(
                    dni = dni,
                    fullName = fullName,
                    areaName = area,
                    areaId = selectedAreaId,
                    email = email,
                    firebaseUid = user.uid,
                    employeeCode = null,
                    specialization = null
                )

                // Llamar al backend
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.registerDocente(request)
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.success) {
                            Log.d(TAG, "Docente registrado exitosamente")

                            androidx.appcompat.app.AlertDialog.Builder(this@RegisterDocenteActivity)
                                .setTitle("✅ Cuenta Creada")
                                .setMessage("Tu cuenta de docente ha sido creada exitosamente.\n\n📧 Hemos enviado un correo de verificación a:\n$email\n\nPor favor verifica tu correo antes de iniciar sesión.")
                                .setPositiveButton("Ir a Login") { _, _ ->
                                    val intent = Intent(this@RegisterDocenteActivity, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .setCancelable(false)
                                .show()
                        } else {
                            Toast.makeText(
                                this@RegisterDocenteActivity,
                                "Error: ${body?.message ?: "Respuesta vacía del servidor"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorResponse = try {
                            com.google.gson.Gson().fromJson(errorBody, ErrorResponse::class.java)
                        } catch (e: Exception) {
                            null
                        }

                        val errorMessage = errorResponse?.error ?: "Error en el registro (${response.code()})"
                        Toast.makeText(this@RegisterDocenteActivity, errorMessage, Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Error response: $errorBody")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Log.e(TAG, "Error registrando docente", e)
                    Toast.makeText(
                        this@RegisterDocenteActivity,
                        "Error de conexión: ${e.message}",
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