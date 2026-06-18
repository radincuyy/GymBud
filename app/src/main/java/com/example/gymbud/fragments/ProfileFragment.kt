package com.example.gymbud.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.gymbud.R
import com.example.gymbud.activities.LoginActivity
import com.example.gymbud.database.WorkoutDao
import com.example.gymbud.utils.SessionManager
import com.example.gymbud.utils.ThemeManager
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView

    private lateinit var switchDarkMode: SwitchMaterial
    private lateinit var btnShareProgress: LinearLayout
    private lateinit var btnAbout: LinearLayout
    private lateinit var btnLogout: LinearLayout

    private lateinit var sessionManager: SessionManager
    private lateinit var themeManager: ThemeManager
    private lateinit var workoutDao: WorkoutDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        sessionManager = SessionManager(requireContext())
        themeManager = ThemeManager(requireContext())
        workoutDao = WorkoutDao(requireContext())

        // Bind views
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail)

        switchDarkMode = view.findViewById(R.id.switchDarkMode)
        btnShareProgress = view.findViewById(R.id.btnShareProgress)
        btnAbout = view.findViewById(R.id.btnAbout)
        btnLogout = view.findViewById(R.id.btnLogout)

        // Populate User Info
        tvProfileName.text = sessionManager.getUserName()
        tvProfileEmail.text = sessionManager.getUserEmail()

        // Setup Dark Mode Toggle
        switchDarkMode.isChecked = themeManager.isDarkMode()
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            themeManager.setDarkMode(isChecked)
            activity?.recreate()
        }

        // Share Progress (Implicit Intent)
        btnShareProgress.setOnClickListener {
            shareProgress()
        }

        // About dialog
        btnAbout.setOnClickListener {
            showAboutDialog()
        }

        // Logout
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        return view
    }

    private fun shareProgress() {
        val userId = sessionManager.getUserId()
        val workoutsCount = workoutDao.getWeeklyWorkoutCount(userId)
        val weeklyVolume = workoutDao.getWeeklyVolume(userId)

        val shareMessage = "I've logged $workoutsCount workouts and lifted ${String.format(Locale.getDefault(), "%.0f", weeklyVolume)} kg this week on GymBud! Join me in hitting those gains! 🏋️‍♂️💪"
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_progress)))
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.about)
            .setMessage(R.string.about_text)
            .setPositiveButton(R.string.cancel, null)
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.confirm_logout)
            .setPositiveButton(R.string.logout) { _, _ ->
                sessionManager.logout()
                val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
