package com.example.gymbud.fragments

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.gymbud.R
import com.example.gymbud.activities.LoginActivity
import com.example.gymbud.database.WorkoutDao
import com.example.gymbud.utils.SessionManager
import com.example.gymbud.utils.ThemeManager
import com.example.gymbud.utils.WorkoutReminderReceiver
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView

    private lateinit var switchDarkMode: SwitchMaterial
    private lateinit var btnShareProgress: LinearLayout
    private lateinit var btnSetReminder: LinearLayout
    private lateinit var btnAbout: LinearLayout
    private lateinit var btnContactUs: LinearLayout
    private lateinit var btnLogout: LinearLayout

    private lateinit var sessionManager: SessionManager
    private lateinit var themeManager: ThemeManager
    private lateinit var workoutDao: WorkoutDao

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showTimePicker()
        } else {
            Toast.makeText(requireContext(), "Izin notifikasi diperlukan untuk reminder", Toast.LENGTH_SHORT).show()
        }
    }

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
        btnSetReminder = view.findViewById(R.id.btnSetReminder)
        btnAbout = view.findViewById(R.id.btnAbout)
        btnContactUs = view.findViewById(R.id.btnContactUs)
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

        // Workout Reminder
        btnSetReminder.setOnClickListener {
            checkNotificationPermission()
        }

        // About dialog
        btnAbout.setOnClickListener {
            showAboutDialog()
        }

        // Contact Us (Implicit Intent)
        btnContactUs.setOnClickListener {
            openWhatsApp()
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

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                showTimePicker()
            }
        } else {
            showTimePicker()
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            setAlarm(selectedHour, selectedMinute)
        }, hour, minute, true).show()
    }

    private fun setAlarm(hour: Int, minute: Int) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Check for Exact Alarm Permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        val intent = Intent(requireContext(), WorkoutReminderReceiver::class.java)
        
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            100,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If the time is in the past or exactly now, set it for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DATE, 1)
            }
        }

        // Use setExactAndAllowWhileIdle for precision
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        Toast.makeText(requireContext(), getString(R.string.reminder_set_for, timeString), Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.about)
            .setMessage(R.string.about_text)
            .setPositiveButton(R.string.cancel, null)
            .show()
    }

    private fun openWhatsApp() {
        val phoneNumber = "+6281210926089"
        val message = "Halo GymBud, saya ingin bertanya tentang Kelompok 5"
        val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp tidak terinstal", Toast.LENGTH_SHORT).show()
        }
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
