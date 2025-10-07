package com.example.docentes

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docentes.adapters.BimesterAdapter
import com.example.docentes.models.BimesterData
import com.example.docentes.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var rvBimesters: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvBimesters = findViewById(R.id.recyclerViewBimesters)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        rvBimesters.layoutManager = LinearLayoutManager(this)

        loadBimesters()
    }

    private fun loadBimesters() {
        progressBar.visibility = ProgressBar.VISIBLE
        tvEmptyState.visibility = TextView.GONE

        RetrofitClient.instance.getBimesters().enqueue(object : Callback<List<BimesterData>> {
            override fun onResponse(
                call: Call<List<BimesterData>>,
                response: Response<List<BimesterData>>
            ) {
                progressBar.visibility = ProgressBar.GONE
                if (response.isSuccessful) {
                    val bimesters = response.body() ?: emptyList()
                    if (bimesters.isEmpty()) {
                        tvEmptyState.visibility = TextView.VISIBLE
                    } else {
                        val adapter = BimesterAdapter(bimesters) { bimesterId ->
                            // TODO: Implementar agregar grado
                            Toast.makeText(this@MainActivity, "Agregar grado a bimestre $bimesterId", Toast.LENGTH_SHORT).show()
                        }
                        rvBimesters.adapter = adapter
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<BimesterData>>, t: Throwable) {
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}