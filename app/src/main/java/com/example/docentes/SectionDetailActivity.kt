package com.example.docentes

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class SectionDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_section_detail)

        // Obtener datos del intent
        val sectionId = intent.getIntExtra("section_id", -1)
        val gradeNumber = intent.getIntExtra("grade_number", -1)
        val sectionLetter = intent.getStringExtra("section_letter") ?: ""
        val bimesterName = intent.getStringExtra("bimester_name") ?: ""

        // Configurar toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Configurar título
        val tvTitle = findViewById<TextView>(R.id.tvSectionTitle)
        tvTitle.text = "$bimesterName Bimestre $gradeNumber° $sectionLetter"
    }
}