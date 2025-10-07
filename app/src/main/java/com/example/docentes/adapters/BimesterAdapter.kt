package com.example.docentes.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docentes.R
import com.example.docentes.models.BimesterData

class BimesterAdapter(
    private val bimesters: List<BimesterData>,
    private val onAddGrade: (Int) -> Unit
) : RecyclerView.Adapter<BimesterAdapter.BimesterViewHolder>() {

    inner class BimesterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivExpandIcon: ImageView = view.findViewById(R.id.ivExpandIcon)
        val tvBimester: TextView = view.findViewById(R.id.tvBimesterName)
        val rvGrades: RecyclerView = view.findViewById(R.id.recyclerViewGrades)
        val layoutGrades: LinearLayout = view.findViewById(R.id.layoutGradesContainer)
        val btnAddGrade: Button = view.findViewById(R.id.btnAddGrade)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BimesterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bimester, parent, false)
        return BimesterViewHolder(view)
    }

    override fun onBindViewHolder(holder: BimesterViewHolder, position: Int) {
        val bimester = bimesters[position]
        holder.tvBimester.text = "${bimester.name} Bimestre"

        // Configurar adaptador de grados
        val gradeAdapter = GradeAdapter(bimester.grades, bimester.name)
        holder.rvGrades.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.rvGrades.adapter = gradeAdapter

        // Estado de expansión
        var isExpanded = false

        // Click en todo el header para expandir/colapsar
        val headerLayout = holder.itemView.findViewById<View>(R.id.layoutBimesterHeader)
        headerLayout.setOnClickListener {
            isExpanded = !isExpanded
            holder.layoutGrades.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.ivExpandIcon.rotation = if (isExpanded) 180f else 0f
        }

        // Botón agregar grado
        holder.btnAddGrade.setOnClickListener {
            onAddGrade(bimester.id)
        }
    }

    override fun getItemCount() = bimesters.size
}