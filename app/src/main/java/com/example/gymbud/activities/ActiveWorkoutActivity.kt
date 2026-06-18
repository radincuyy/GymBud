package com.example.gymbud.activities

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gymbud.R
import com.example.gymbud.database.WorkoutDao
import com.example.gymbud.models.Workout
import com.example.gymbud.utils.SessionManager
import com.example.gymbud.utils.ThemeManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActiveWorkoutActivity : AppCompatActivity() {

    private lateinit var tvActiveTitle: TextView
    private lateinit var tvTimerClock: TextView
    private lateinit var tvTimerLabel: TextView
    private lateinit var tvTimerStatus: TextView
    private lateinit var tvCurrentExerciseName: TextView
    private lateinit var tvTargetInfo: TextView
    private lateinit var tvCurrentSetNumber: TextView
    private lateinit var etActualReps: EditText
    private lateinit var etActualWeight: EditText
    private lateinit var layoutActualWeight: LinearLayout
    private lateinit var btnCompleteSet: Button
    private lateinit var btnNextExercise: Button
    private lateinit var btnFinishWorkout: Button

    private lateinit var workoutDao: WorkoutDao
    private lateinit var sessionManager: SessionManager

    private var workoutsList = mutableListOf<Workout>()
    private var currentExerciseIndex = 0
    private var currentSetIndex = 1
    private var restTimer: CountDownTimer? = null
    private var restDurationSeconds = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager(this).applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_workout)

        workoutDao = WorkoutDao(this)
        sessionManager = SessionManager(this)

        // Bind Views
        tvActiveTitle = findViewById(R.id.tvActiveTitle)
        tvTimerClock = findViewById(R.id.tvTimerClock)
        tvTimerLabel = findViewById(R.id.tvTimerLabel)
        tvTimerStatus = findViewById(R.id.tvTimerStatus)
        tvCurrentExerciseName = findViewById(R.id.tvCurrentExerciseName)
        tvTargetInfo = findViewById(R.id.tvTargetInfo)
        tvCurrentSetNumber = findViewById(R.id.tvCurrentSetNumber)
        etActualReps = findViewById(R.id.etActualReps)
        etActualWeight = findViewById(R.id.etActualWeight)
        layoutActualWeight = findViewById(R.id.layoutActualWeight)
        btnCompleteSet = findViewById(R.id.btnCompleteSet)
        btnNextExercise = findViewById(R.id.btnNextExercise)
        btnFinishWorkout = findViewById(R.id.btnFinishWorkout)

        // Load today's plan
        loadTodayWorkouts()

        // Set Click Listeners
        btnCompleteSet.setOnClickListener {
            completeSet()
        }

        btnNextExercise.setOnClickListener {
            nextExercise()
        }

        btnFinishWorkout.setOnClickListener {
            finishWorkout()
        }

        // Tap timer clock to skip rest timer
        tvTimerClock.setOnClickListener {
            skipRestTimer()
        }

        // Tap rest timer label to customize duration
        updateTimerLabelUI()
        tvTimerLabel.setOnClickListener {
            showCustomRestTimeDialog()
        }
    }

    private fun loadTodayWorkouts() {
        val userId = sessionManager.getUserId()
        if (userId == -1L) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val plan = workoutDao.getWorkoutsByDate(userId, todayStr)

        if (plan.isEmpty()) {
            Toast.makeText(this, "Tidak ada rencana latihan untuk hari ini", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        workoutsList.addAll(plan)
        currentExerciseIndex = 0
        currentSetIndex = 1
        updateExerciseUI()
    }

    private fun updateExerciseUI() {
        if (currentExerciseIndex >= workoutsList.size) return

        val currentWorkout = workoutsList[currentExerciseIndex]
        tvCurrentExerciseName.text = currentWorkout.exerciseName

        val isBodyweight = isBodyweightExercise(currentWorkout.exerciseName)
        if (isBodyweight) {
            tvTargetInfo.text = "Target: ${currentWorkout.sets} Set x ${currentWorkout.reps} Reps (Tanpa Beban)"
            layoutActualWeight.visibility = View.GONE
        } else {
            tvTargetInfo.text = "Target: ${currentWorkout.sets} Set x ${currentWorkout.reps} Reps | ${currentWorkout.weight} kg"
            layoutActualWeight.visibility = View.VISIBLE
            etActualWeight.setText(currentWorkout.weight.toString())
        }

        etActualReps.setText(currentWorkout.reps.toString())
        btnCompleteSet.isEnabled = true
        updateSetUI()

        if (currentExerciseIndex == workoutsList.size - 1) {
            btnNextExercise.visibility = View.GONE
        } else {
            btnNextExercise.visibility = View.VISIBLE
        }
    }

    private fun updateSetUI() {
        val currentWorkout = workoutsList[currentExerciseIndex]
        tvCurrentSetNumber.text = "Set $currentSetIndex dari ${currentWorkout.sets}"
        btnCompleteSet.text = "Selesaikan Set $currentSetIndex"
    }

    private fun completeSet() {
        val repsStr = etActualReps.text.toString().trim()
        val weightStr = etActualWeight.text.toString().trim()

        if (repsStr.isEmpty()) {
            Toast.makeText(this, "Masukkan reps yang dicapai", Toast.LENGTH_SHORT).show()
            return
        }

        val actualReps = repsStr.toIntOrNull() ?: 0
        val currentWorkout = workoutsList[currentExerciseIndex]
        val isBodyweight = isBodyweightExercise(currentWorkout.exerciseName)
        val actualWeight = if (isBodyweight) 0.0 else (weightStr.toDoubleOrNull() ?: 0.0)

        // Update database and local list with actual logged values
        val updatedWorkout = currentWorkout.copy(
            reps = actualReps,
            weight = actualWeight
        )
        workoutDao.updateWorkout(updatedWorkout)
        workoutsList[currentExerciseIndex] = updatedWorkout

        Toast.makeText(this, "Set $currentSetIndex tercatat! (${actualReps} reps" + (if (isBodyweight) "" else ", ${actualWeight} kg") + ")", Toast.LENGTH_SHORT).show()
        startRestTimer()
    }

    private fun getThemeColor(attrId: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attrId, typedValue, true)
        return typedValue.data
    }

    private fun startRestTimer() {
        restTimer?.cancel()

        tvTimerStatus.text = "Istirahat... (Ketuk waktu untuk skip)"
        btnCompleteSet.isEnabled = false
        tvTimerClock.setTextColor(getColor(R.color.lime_accent))

        restTimer = object : CountDownTimer(restDurationSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                tvTimerClock.text = String.format(Locale.getDefault(), "00:%02d", secondsRemaining)
            }

            override fun onFinish() {
                resetTimerState()
                advanceSet()
            }
        }.start()
    }

    private fun skipRestTimer() {
        if (restTimer != null) {
            restTimer?.cancel()
            restTimer = null
            resetTimerState()
            advanceSet()
            Toast.makeText(this, "Istirahat dilewati", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetTimerState() {
        tvTimerClock.text = "00:00"
        tvTimerClock.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
        tvTimerStatus.text = "Siap untuk set berikutnya"
        btnCompleteSet.isEnabled = true
    }

    private fun advanceSet() {
        val currentWorkout = workoutsList[currentExerciseIndex]
        if (currentSetIndex < currentWorkout.sets) {
            currentSetIndex++
            updateSetUI()
        } else {
            Toast.makeText(this, "Semua set selesai untuk latihan ini!", Toast.LENGTH_LONG).show()
            btnCompleteSet.isEnabled = false
            btnCompleteSet.text = "Selesai"
            tvTimerStatus.text = if (currentExerciseIndex < workoutsList.size - 1) {
                "Semua set selesai! Silakan lanjut ke latihan berikutnya."
            } else {
                "Semua latihan selesai! Ketuk 'Selesai Latihan' di bawah."
            }
        }
    }

    private fun nextExercise() {
        if (currentExerciseIndex < workoutsList.size - 1) {
            restTimer?.cancel()
            restTimer = null
            resetTimerState()

            currentExerciseIndex++
            currentSetIndex = 1
            updateExerciseUI()
        }
    }

    private fun finishWorkout() {
        AlertDialog.Builder(this)
            .setTitle("Selesai Latihan!")
            .setMessage("Selamat! Anda telah menyelesaikan seluruh latihan hari ini. Kerja bagus, terus pertahankan konsistensi Anda!")
            .setPositiveButton("Selesai") { _, _ ->
                finish()
            }
            .setNegativeButton("Kembali", null)
            .show()
    }

    private fun isBodyweightExercise(name: String): Boolean {
        val lower = name.lowercase(Locale.getDefault())
        return lower.contains("push-up") || 
               lower.contains("push up") || 
               lower.contains("pull-up") || 
               lower.contains("pull up") || 
               lower.contains("plank") || 
               lower.contains("crunch") || 
               lower.contains("sit-up") || 
               lower.contains("sit up") || 
               lower.contains("chin-up") || 
               lower.contains("chin up") || 
               lower.contains("dip") || 
               lower.contains("bodyweight") || 
               lower.contains("stretching") || 
               lower.contains("running") || 
               lower.contains("jumping") || 
               lower.contains("cardio") || 
               lower.contains("burpee") || 
               lower.contains("squat (bodyweight)")
    }

    private fun updateTimerLabelUI() {
        tvTimerLabel.text = "WAKTU ISTIRAHAT: ${restDurationSeconds}S (UBAH)"
    }

    private fun showCustomRestTimeDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(restDurationSeconds.toString())
            setSelection(text.length)
            val paddingPx = (24 * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Kustomisasi Waktu Istirahat")
            .setMessage("Masukkan waktu istirahat dalam detik:")
            .setView(container)
            .setPositiveButton("Simpan") { _, _ ->
                val seconds = input.text.toString().toIntOrNull() ?: 60
                if (seconds > 0) {
                    restDurationSeconds = seconds
                    updateTimerLabelUI()
                    Toast.makeText(this, "Waktu istirahat diubah menjadi $seconds detik", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Waktu tidak valid", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        restTimer?.cancel()
    }
}
