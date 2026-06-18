package com.example.gymbud.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.gymbud.R
import com.example.gymbud.database.WorkoutDao
import com.example.gymbud.utils.SessionManager
import android.widget.LinearLayout
import java.util.Locale

class StatsFragment : Fragment() {

    private lateinit var tvProfileWorkouts: TextView
    private lateinit var tvProfileVolume: TextView
    private lateinit var tvProfileStreak: TextView
    private lateinit var layoutPRContainer: LinearLayout

    private lateinit var barMon: View
    private lateinit var barTue: View
    private lateinit var barWed: View
    private lateinit var barThu: View
    private lateinit var barFri: View
    private lateinit var barSat: View
    private lateinit var barSun: View

    private lateinit var tvMonVal: TextView
    private lateinit var tvTueVal: TextView
    private lateinit var tvWedVal: TextView
    private lateinit var tvThuVal: TextView
    private lateinit var tvFriVal: TextView
    private lateinit var tvSatVal: TextView
    private lateinit var tvSunVal: TextView

    private lateinit var sessionManager: SessionManager
    private lateinit var workoutDao: WorkoutDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        sessionManager = SessionManager(requireContext())
        workoutDao = WorkoutDao(requireContext())

        // Bind views
        tvProfileWorkouts = view.findViewById(R.id.tvProfileWorkouts)
        tvProfileVolume = view.findViewById(R.id.tvProfileVolume)
        tvProfileStreak = view.findViewById(R.id.tvProfileStreak)
        layoutPRContainer = view.findViewById(R.id.layoutPRContainer)

        barMon = view.findViewById(R.id.barMon)
        barTue = view.findViewById(R.id.barTue)
        barWed = view.findViewById(R.id.barWed)
        barThu = view.findViewById(R.id.barThu)
        barFri = view.findViewById(R.id.barFri)
        barSat = view.findViewById(R.id.barSat)
        barSun = view.findViewById(R.id.barSun)

        tvMonVal = view.findViewById(R.id.tvMonVal)
        tvTueVal = view.findViewById(R.id.tvTueVal)
        tvWedVal = view.findViewById(R.id.tvWedVal)
        tvThuVal = view.findViewById(R.id.tvThuVal)
        tvFriVal = view.findViewById(R.id.tvFriVal)
        tvSatVal = view.findViewById(R.id.tvSatVal)
        tvSunVal = view.findViewById(R.id.tvSunVal)

        loadProfileStats()

        return view
    }

    private fun loadProfileStats() {
        val userId = sessionManager.getUserId()
        if (userId == -1L) return

        val workoutsCount = workoutDao.getWeeklyWorkoutCount(userId)
        val weeklyVolume = workoutDao.getWeeklyVolume(userId)
        val currentStreak = workoutDao.getCurrentStreak(userId)

        tvProfileWorkouts.text = workoutsCount.toString()
        tvProfileVolume.text = String.format(Locale.getDefault(), "%.0f", weeklyVolume)
        tvProfileStreak.text = currentStreak.toString()

        val distribution = workoutDao.getWeeklyDistribution(userId)
        val maxVal = distribution.values.maxOrNull() ?: 0

        val maxBarHeightPx = (80 * resources.displayMetrics.density).toInt()
        val minBarHeightPx = (4 * resources.displayMetrics.density).toInt()

        setBarHeight(barMon, tvMonVal, distribution["Mon"] ?: 0, maxVal, maxBarHeightPx, minBarHeightPx)
        setBarHeight(barTue, tvTueVal, distribution["Tue"] ?: 0, maxVal, maxBarHeightPx, minBarHeightPx)
        setBarHeight(barWed, tvWedVal, distribution["Wed"] ?: 0, maxVal, maxBarHeightPx, minBarHeightPx)
        setBarHeight(barThu, tvThuVal, distribution["Thu"] ?: 0, maxVal, maxBarHeightPx, minBarHeightPx)
        setBarHeight(barFri, tvFriVal, distribution["Fri"] ?: 0, maxVal, maxBarHeightPx, minBarHeightPx)
        setBarHeight(barSat, tvSatVal, distribution["Sat"] ?: 0, maxVal, maxBarHeightPx, minBarHeightPx)
        setBarHeight(barSun, tvSunVal, distribution["Sun"] ?: 0, maxVal, maxBarHeightPx, minBarHeightPx)

        // Populate Personal Records (PR)
        val prMap = workoutDao.getPersonalRecords(userId)
        populatePersonalRecords(prMap)
    }

    private fun populatePersonalRecords(records: Map<String, Double>) {
        layoutPRContainer.removeAllViews()

        if (records.isEmpty()) {
            val emptyTextView = TextView(requireContext()).apply {
                text = "Belum ada rekor tercatat. Mulai latihan untuk mencatat PR!"
                textSize = 14f
                setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, 8, 0, 8)
            }
            layoutPRContainer.addView(emptyTextView)
            return
        }

        for ((exercise, maxWeight) in records) {
            val rowView = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 12, 0, 12)
                }
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val tvName = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = exercise
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
            }

            val tvVal = TextView(requireContext()).apply {
                text = String.format(Locale.getDefault(), "%.1f kg", maxWeight)
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.lime_accent))
            }

            rowView.addView(tvName)
            rowView.addView(tvVal)
            layoutPRContainer.addView(rowView)

            // Add thin divider
            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant))
            }
            layoutPRContainer.addView(divider)
        }
    }

    private fun getThemeColor(attrId: Int): Int {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(attrId, typedValue, true)
        return typedValue.data
    }

    private fun setBarHeight(bar: View, valText: TextView, value: Int, maxVal: Int, maxHeightPx: Int, minHeightPx: Int) {
        val params = bar.layoutParams
        if (maxVal > 0 && value > 0) {
            params.height = ((value.toDouble() / maxVal.toDouble()) * maxHeightPx).toInt().coerceAtLeast(minHeightPx)
            valText.text = value.toString()
            valText.visibility = View.VISIBLE
        } else {
            params.height = minHeightPx
            valText.visibility = View.INVISIBLE
        }
        bar.layoutParams = params
    }
}
