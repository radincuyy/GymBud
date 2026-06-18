package com.example.gymbud.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gymbud.R
import com.example.gymbud.models.Exercise

class ExerciseGridAdapter(
    private var exercises: List<Exercise>,
    private val onItemClick: (Exercise) -> Unit
) : RecyclerView.Adapter<ExerciseGridAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivExerciseImage: ImageView = view.findViewById(R.id.ivExerciseImage)
        val tvExerciseCategory: TextView = view.findViewById(R.id.tvExerciseCategory)
        val tvExerciseName: TextView = view.findViewById(R.id.tvExerciseName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.tvExerciseName.text = exercise.name
        holder.tvExerciseCategory.text = exercise.categoryName.ifEmpty { "General" }

        if (exercise.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(exercise.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(holder.ivExerciseImage)
        } else {
            holder.ivExerciseImage.setImageResource(R.drawable.ic_launcher_background)
        }

        holder.itemView.setOnClickListener {
            onItemClick(exercise)
        }
    }

    override fun getItemCount(): Int = exercises.size

    fun updateData(newExercises: List<Exercise>) {
        exercises = newExercises
        notifyDataSetChanged()
    }
}
