package com.example.gymbud.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gymbud.R
import com.example.gymbud.activities.WorkoutFormActivity
import com.example.gymbud.activities.ActiveWorkoutActivity
import com.example.gymbud.adapters.WorkoutAdapter
import com.example.gymbud.database.WorkoutDao
import com.example.gymbud.utils.SessionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvWorkoutCount: TextView
    private lateinit var tvVolume: TextView
    private lateinit var tvStreak: TextView
    private lateinit var rvTodayWorkouts: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var btnStartPlan: Button
    private lateinit var fabAddWorkout: FloatingActionButton

    private lateinit var workoutDao: WorkoutDao
    private lateinit var sessionManager: SessionManager
    private lateinit var workoutAdapter: WorkoutAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        workoutDao = WorkoutDao(requireContext())
        sessionManager = SessionManager(requireContext())

        // Bind Views
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvWorkoutCount = view.findViewById(R.id.tvWorkoutCount)
        tvVolume = view.findViewById(R.id.tvVolume)
        tvStreak = view.findViewById(R.id.tvStreak)
        rvTodayWorkouts = view.findViewById(R.id.rvTodayWorkouts)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        btnStartPlan = view.findViewById(R.id.btnStartPlan)
        fabAddWorkout = view.findViewById(R.id.fabAddWorkout)

        // Set User Name
        tvUserName.text = sessionManager.getUserName()

        // Set Greeting based on time of day
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 0..11 -> R.string.good_morning
            in 12..17 -> R.string.good_afternoon
            else -> R.string.good_evening
        }
        tvGreeting.setText(greetingRes)

        // Setup RecyclerView
        rvTodayWorkouts.layoutManager = LinearLayoutManager(requireContext())
        workoutAdapter = WorkoutAdapter(emptyList()) { workout ->
            // Edit Workout click callback
            val intent = Intent(requireContext(), WorkoutFormActivity::class.java).apply {
                putExtra("workout_id", workout.id)
            }
            startActivity(intent)
        }
        rvTodayWorkouts.adapter = workoutAdapter

        // Setup FAB Add Workout click
        fabAddWorkout.setOnClickListener {
            val intent = Intent(requireContext(), WorkoutFormActivity::class.java)
            startActivity(intent)
        }

        // Start Plan click
        btnStartPlan.setOnClickListener {
            val intent = Intent(requireContext(), ActiveWorkoutActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val userId = sessionManager.getUserId()
        if (userId == -1L) return

        // 1. Stats Cards
        val workoutsCount = workoutDao.getWeeklyWorkoutCount(userId)
        val weeklyVolume = workoutDao.getWeeklyVolume(userId)
        val currentStreak = workoutDao.getCurrentStreak(userId)

        tvWorkoutCount.text = workoutsCount.toString()
        tvVolume.text = String.format(Locale.getDefault(), "%.0f", weeklyVolume)
        tvStreak.text = String.format(Locale.getDefault(), "%d", currentStreak)

        // 2. Today's Plan
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayWorkouts = workoutDao.getWorkoutsByDate(userId, todayStr)

        if (todayWorkouts.isEmpty()) {
            rvTodayWorkouts.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
            btnStartPlan.visibility = View.GONE
        } else {
            rvTodayWorkouts.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            btnStartPlan.visibility = View.VISIBLE
            workoutAdapter.updateData(todayWorkouts)
        }
    }
}
