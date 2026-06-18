package com.example.gymbud.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gymbud.R
import com.example.gymbud.models.Workout
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    private var workouts: List<Workout>,
    private val onDeleteClick: (Workout) -> Unit,
    private val onItemClick: (Workout) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHistoryDateHeader: TextView = view.findViewById(R.id.tvHistoryDateHeader)
        val tvHistoryExerciseName: TextView = view.findViewById(R.id.tvHistoryExerciseName)
        val tvHistoryDetails: TextView = view.findViewById(R.id.tvHistoryDetails)
        val btnDeleteHistory: ImageView = view.findViewById(R.id.btnDeleteHistory)
        val cardHistoryItem: View = view.findViewById(R.id.cardHistoryItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val workout = workouts[position]
        
        // Date Grouping logic
        val showHeader = if (position == 0) {
            true
        } else {
            val prevWorkout = workouts[position - 1]
            prevWorkout.workoutDate != workout.workoutDate
        }

        if (showHeader) {
            holder.tvHistoryDateHeader.text = formatDate(workout.workoutDate)
            holder.tvHistoryDateHeader.visibility = View.VISIBLE
        } else {
            holder.tvHistoryDateHeader.visibility = View.GONE
        }

        holder.tvHistoryExerciseName.text = workout.exerciseName
        
        val details = "${workout.sets} sets x ${workout.reps} reps @ ${workout.weight} kg"
        holder.tvHistoryDetails.text = details

        holder.btnDeleteHistory.setOnClickListener {
            onDeleteClick(workout)
        }

        holder.cardHistoryItem.setOnClickListener {
            onItemClick(workout)
        }
    }

    override fun getItemCount(): Int = workouts.size

    fun updateData(newWorkouts: List<Workout>) {
        workouts = newWorkouts
        notifyDataSetChanged()
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
            val date = parser.parse(dateStr)
            if (date != null) formatter.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
}
