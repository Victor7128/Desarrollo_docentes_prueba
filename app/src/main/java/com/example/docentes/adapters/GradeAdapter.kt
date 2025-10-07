package com.example.docentes.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docentes.R
import com.example.docentes.models.Grade

class GradeAdapter(
    private val grades: List<Grade>,
    private val bimesterName: String = ""
) : RecyclerView.Adapter<GradeAdapter.GradeViewHolder>() {

    inner class GradeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivExpandIcon: ImageView = view.findViewById(R.id.ivGradeExpandIcon)
        val tvGrade: TextView = view.findViewById(R.id.tvGradeNumber)
        val btnEditGrade: ImageButton = view.findViewById(R.id.btnEditGrade)
        val btnDeleteGrade: ImageButton = view.findViewById(R.id.btnDeleteGrade)
        val rvSections: RecyclerView = view.findViewById(R.id.recyclerViewSections)
        val layoutSections: LinearLayout = view.findViewById(R.id.layoutSectionsContainer)
        val btnAddSection: Button = view.findViewById(R.id.btnAddSection)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grade, parent, false)
        return GradeViewHolder(view)
    }

    override fun onBindViewHolder(holder: GradeViewHolder, position: Int) {
        val grade = grades[position]
        holder.tvGrade.text = "${grade.number}°"

        // Configurar adaptador de secciones
        val sectionAdapter = SectionAdapter(grade.sections, grade.number, bimesterName)
        holder.rvSections.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.rvSections.adapter = sectionAdapter

        // Estado de expansión
        var isExpanded = false

        // Click en todo el header para expandir/colapsar
        val headerLayout = holder.itemView.findViewById<View>(R.id.layoutGradeHeader)
        headerLayout.setOnClickListener {
            isExpanded = !isExpanded
            holder.layoutSections.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.ivExpandIcon.rotation = if (isExpanded) 180f else 0f
        }

        // Botón editar
        holder.btnEditGrade.setOnClickListener {
            // TODO: Implementar edición
        }

        // Botón eliminar
        holder.btnDeleteGrade.setOnClickListener {
            // TODO: Implementar eliminación
        }

        // Botón agregar sección
        holder.btnAddSection.setOnClickListener {
            // TODO: Implementar agregar sección
        }
    }

    override fun getItemCount() = grades.size
}