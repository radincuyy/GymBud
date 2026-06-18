package com.example.gymbud.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gymbud.R
import com.example.gymbud.models.Workout

class WorkoutAdapter(
    private var workouts: List<Workout>,
    private val onEditClick: (Workout) -> Unit
) : RecyclerView.Adapter<WorkoutAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvExerciseName: TextView = view.findViewById(R.id.tvExerciseName)
        val tvWorkoutDetails: TextView = view.findViewById(R.id.tvWorkoutDetails)
        val btnEditWorkout: ImageView = view.findViewById(R.id.btnEditWorkout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val workout = workouts[position]
        holder.tvExerciseName.text = workout.exerciseName
        
        val details = "${workout.sets} sets x ${workout.reps} reps @ ${workout.weight} kg"
        holder.tvWorkoutDetails.text = details

        holder.btnEditWorkout.setOnClickListener {
            onEditClick(workout)
        }
    }

    override fun getItemCount(): Int = workouts.size

    fun updateData(newWorkouts: List<Workout>) {
        workouts = newWorkouts
        notifyDataSetChanged()
    }
}
