package com.example.docentes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

class LoginActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_SIGN_IN = 9001
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var tvRegister: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        tvRegister = findViewById(R.id.tvRegister)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                signInWithEmail(email, password)
            }
        }

        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, SelectRegisterTypeActivity::class.java))
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            etEmail.error = "Ingresa tu correo"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Correo inválido"
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = "Ingresa tu contraseña"
            return false
        }

        if (password.length < 6) {
            etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return false
        }

        return true
    }

    private fun signInWithEmail(email: String, password: String) {
        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser

                    // Obtener token para backend
                    user?.getIdToken(true)?.addOnSuccessListener { result ->
                        val token = result.token
                        Log.d(TAG, "Firebase token: $token")

                        // Verificar rol en backend y redirigir
                        verifyUserAndRedirect(token)
                    }
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        this,
                        "Autenticación fallida: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun signInWithGoogle() {
        // ✅ FORZAR SELECCIÓN DE CUENTA cada vez
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Una vez cerrada sesión, iniciar el flujo de sign-in
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.result
                Log.d(TAG, "firebaseAuthWithGoogle: ${account.id}")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Error en inicio de sesión con Google: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser

                    user?.getIdToken(true)?.addOnSuccessListener { result ->
                        val token = result.token
                        Log.d(TAG, "✅ Google Sign-In exitoso, verificando con backend...")
                        verifyUserAndRedirect(token)
                    }?.addOnFailureListener { e ->
                        showLoading(false)
                        Log.e(TAG, "❌ Error obteniendo token", e)
                        Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showLoading(false)
                    Log.w(TAG, "signInWithCredential:failure", task.exception)

                    // Mensaje más específico para Google Sign-In
                    val errorMessage = when {
                        task.exception?.message?.contains("network error") == true ->
                            "Error de red. Verifica tu conexión."
                        task.exception?.message?.contains("invalid credential") == true ->
                            "Credenciales inválidas. Intenta nuevamente."
                        else -> "Error en inicio de sesión con Google: ${task.exception?.message}"
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun verifyUserAndRedirect(token: String?) {
        if (token == null) {
            Toast.makeText(this, "Error: No se pudo obtener el token", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val user = auth.currentUser
                if (user == null) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(
                            this@LoginActivity,
                            "Error: Usuario no autenticado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Llamar al endpoint /api/auth/me
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getCurrentUser(user.uid)
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.success && body.data != null) {
                            val userResponse = body.data

                            Log.d(TAG, "Usuario verificado - Rol: ${userResponse.role}, Status: ${userResponse.status}")

                            // Verificar que el rol sea DOCENTE
                            if (userResponse.role == "DOCENTE") {
                                // Verificar que la cuenta esté activa
                                if (userResponse.status == "ACTIVE") {
                                    // Guardar datos del usuario en SharedPreferences
                                    saveUserData(userResponse)

                                    // Redirigir a MainActivity
                                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // Cuenta no activa
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Tu cuenta está ${userResponse.status}. Contacta al administrador.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // Cerrar sesión
                                    auth.signOut()
                                    googleSignInClient.signOut()
                                }
                            } else {
                                // El usuario no es docente
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Esta aplicación es solo para docentes. Tu rol es: ${userResponse.role}",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Cerrar sesión
                                auth.signOut()
                                googleSignInClient.signOut()
                            }
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Error: ${body?.message ?: "Respuesta inválida"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        when (response.code()) {
                            401 -> {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "No autorizado. Por favor inicia sesión nuevamente.",
                                    Toast.LENGTH_LONG
                                ).show()
                                auth.signOut()
                                googleSignInClient.signOut()
                            }
                            404 -> {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Usuario no encontrado en el sistema",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> {
                                val errorBody = response.errorBody()?.string()
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Error ${response.code()}: $errorBody",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Log.e(TAG, "Error verificando usuario", e)
                    Toast.makeText(
                        this@LoginActivity,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun saveUserData(userResponse: com.example.docentes.models.UserResponse) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putInt("user_id", userResponse.id)
        editor.putString("user_email", userResponse.email)
        editor.putString("user_role", userResponse.role)
        editor.putString("user_status", userResponse.status)

        // Guardar datos del perfil
        val profileData = userResponse.profileData
        editor.putString("user_full_name", profileData["full_name"] as? String)
        editor.putInt("user_area_id", (profileData["area_id"] as? Double)?.toInt() ?: 0)
        editor.putString("user_employee_code", profileData["employee_code"] as? String)

        editor.apply()

        Log.d(TAG, "Datos del usuario guardados en SharedPreferences")
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnGoogleSignIn.isEnabled = !show
    }

    override fun onStart() {
        super.onStart()

        // ✅ CORREGIDO: Verificar usuario con el backend antes de redirigir
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Usuario ya autenticado, verificando con backend...")

            currentUser.getIdToken(true).addOnSuccessListener { result ->
                val token = result.token
                verifyUserAndRedirect(token)
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error obteniendo token en onStart", e)
                // Si hay error obteniendo el token, cerrar sesión
                auth.signOut()
                googleSignInClient.signOut()
            }
        }
    }
}