package com.example.gymbud.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gymbud.R
import com.example.gymbud.activities.WorkoutFormActivity
import com.example.gymbud.adapters.HistoryAdapter
import com.example.gymbud.database.WorkoutDao
import com.example.gymbud.models.Workout
import com.example.gymbud.utils.SessionManager

class HistoryFragment : Fragment() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout

    private lateinit var workoutDao: WorkoutDao
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        workoutDao = WorkoutDao(requireContext())
        sessionManager = SessionManager(requireContext())

        rvHistory = view.findViewById(R.id.rvHistory)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)

        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        
        adapter = HistoryAdapter(
            workouts = emptyList(),
            onDeleteClick = { workout -> showDeleteConfirmation(workout) },
            onItemClick = { workout ->
                // Edit Workout
                val intent = Intent(requireContext(), WorkoutFormActivity::class.java).apply {
                    putExtra("workout_id", workout.id)
                }
                startActivity(intent)
            }
        )
        rvHistory.adapter = adapter

        return view
    }

    override fun onResume() {
        super.onResume()
        loadHistoryData()
    }

    private fun loadHistoryData() {
        val userId = sessionManager.getUserId()
        if (userId == -1L) return

        val workouts = workoutDao.getWorkoutsByUser(userId)

        if (workouts.isEmpty()) {
            rvHistory.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            rvHistory.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            adapter.updateData(workouts)
        }
    }

    private fun showDeleteConfirmation(workout: Workout) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_workout)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.delete) { _, _ ->
                workoutDao.deleteWorkout(workout.id)
                loadHistoryData()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
