package com.example.docentes.adapters

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.docentes.R
import com.example.docentes.models.Section
import com.example.docentes.SectionDetailActivity

class SectionAdapter(
    private val sections: List<Section>,
    private val gradeNumber: Int,
    private val bimesterName: String = ""
) : RecyclerView.Adapter<SectionAdapter.SectionViewHolder>() {

    inner class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSection: TextView = view.findViewById(R.id.tvSectionLetter)
        val btnEditSection: ImageButton = view.findViewById(R.id.btnEditSection)
        val btnDeleteSection: ImageButton = view.findViewById(R.id.btnDeleteSection)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_section, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]
        holder.tvSection.text = "Sección ${section.letter}"

        // Click en la sección para navegar
        holder.itemView.setOnClickListener {
            Log.d("SectionAdapter", "Click en sección: ${section.id}, grado: $gradeNumber, letra: ${section.letter}")

            val context = holder.itemView.context
            val intent = Intent(context, SectionDetailActivity::class.java).apply {
                putExtra("section_id", section.id)
                putExtra("grade_number", gradeNumber)
                putExtra("section_letter", section.letter)
                putExtra("bimester_name", bimesterName)
            }

            try {
                context.startActivity(intent)
                Log.d("SectionAdapter", "Intent ejecutado correctamente")
            } catch (e: Exception) {
                Log.e("SectionAdapter", "Error al abrir activity: ${e.message}")
                e.printStackTrace()
            }
        }

        // Botón editar
        holder.btnEditSection.setOnClickListener {
            // TODO: Implementar edición
            Log.d("SectionAdapter", "Editar sección ${section.letter}")
        }

        // Botón eliminar
        holder.btnDeleteSection.setOnClickListener {
            // TODO: Implementar eliminación
            Log.d("SectionAdapter", "Eliminar sección ${section.letter}")
        }
    }

    override fun getItemCount() = sections.size
}