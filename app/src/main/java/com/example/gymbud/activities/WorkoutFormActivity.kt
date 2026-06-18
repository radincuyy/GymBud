package com.example.gymbud.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import com.example.gymbud.R
import com.example.gymbud.database.WorkoutDao
import com.example.gymbud.models.Workout
import com.example.gymbud.utils.SessionManager
import com.example.gymbud.utils.ThemeManager
import com.example.gymbud.network.RetrofitClient
import retrofit2.awaitResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WorkoutFormActivity : AppCompatActivity() {

    private lateinit var tvFormTitle: TextView
    private lateinit var autoCompleteExercises: AutoCompleteTextView
    private lateinit var etSets: EditText
    private lateinit var etReps: EditText
    private lateinit var etWeight: EditText
    private lateinit var etDate: EditText
    private lateinit var btnSaveWorkout: Button
    private lateinit var btnDeleteWorkout: Button
    private lateinit var layoutWeightInput: LinearLayout
    private lateinit var layoutDateInput: LinearLayout

    private lateinit var workoutDao: WorkoutDao
    private lateinit var sessionManager: SessionManager

    private var workoutId: Long = -1L
    private var isEditMode = false

    private val activityJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + activityJob)

    private val exercisesList = arrayOf(
        "Bench Press", "Squat", "Deadlift", "Overhead Press", "Barbell Row",
        "Dumbbell Bicep Curl", "Tricep Pushdown", "Lateral Raise", "Pull-up",
        "Push-up", "Leg Press", "Leg Curl", "Leg Extension", "Plank", "Crunch"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager(this).applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_form)

        workoutDao = WorkoutDao(this)
        sessionManager = SessionManager(this)

        // Bind Views
        tvFormTitle = findViewById(R.id.tvFormTitle)
        autoCompleteExercises = findViewById(R.id.autoCompleteExercises)
        etSets = findViewById(R.id.etSets)
        etReps = findViewById(R.id.etReps)
        etWeight = findViewById(R.id.etWeight)
        etDate = findViewById(R.id.etDate)
        btnSaveWorkout = findViewById(R.id.btnSaveWorkout)
        btnDeleteWorkout = findViewById(R.id.btnDeleteWorkout)

        layoutWeightInput = findViewById(R.id.layoutWeightInput)
        layoutDateInput = findViewById(R.id.layoutDateInput)

        // Setup Exposed Dropdown Menu (initially static)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, exercisesList.toMutableList())
        autoCompleteExercises.setAdapter(adapter)

        // Setup dynamic field toggling
        autoCompleteExercises.setOnItemClickListener { _, _, _, _ ->
            updateFieldsVisibility()
        }
        autoCompleteExercises.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateFieldsVisibility()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Fetch exercise catalog dynamically from WGER API
        loadWgerExercises()

        // Setup Date Picker
        etDate.setOnClickListener {
            showDatePicker()
        }

        // Check Mode
        workoutId = intent.getLongExtra("workout_id", -1L)
        if (workoutId != -1L) {
            isEditMode = true
            loadWorkoutData(workoutId)
        } else {
            // Default to today's date
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            etDate.setText(today)
            tvFormTitle.setText(R.string.add_workout)
            btnDeleteWorkout.visibility = View.GONE
            updateFieldsVisibility()
        }

        // Save Click
        btnSaveWorkout.setOnClickListener {
            saveWorkout()
        }

        // Delete Click
        btnDeleteWorkout.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun loadWorkoutData(id: Long) {
        val workout = workoutDao.getWorkoutById(id)
        if (workout != null) {
            tvFormTitle.setText(R.string.edit_workout)
            btnDeleteWorkout.visibility = View.VISIBLE

            // Select Dropdown item
            autoCompleteExercises.setText(workout.exerciseName, false)

            etSets.setText(workout.sets.toString())
            etReps.setText(workout.reps.toString())
            etWeight.setText(workout.weight.toString())
            etDate.setText(workout.workoutDate)
            
            updateFieldsVisibility()
        } else {
            Toast.makeText(this, "Workout not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        // If date already set, parse it to display on picker
        val currentSetDate = etDate.text.toString().trim()
        if (currentSetDate.isNotEmpty()) {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentSetDate)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                // Ignore and use current date
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            etDate.setText(formattedDate)
        }, year, month, day).show()
    }

    private fun saveWorkout() {
        val exercise = autoCompleteExercises.text.toString().trim()
        val setsStr = etSets.text.toString().trim()
        val repsStr = etReps.text.toString().trim()
        val weightStr = etWeight.text.toString().trim()
        val dateStr = etDate.text.toString().trim()

        val isBodyweight = isBodyweightExercise(exercise)

        if (setsStr.isEmpty() || repsStr.isEmpty() || dateStr.isEmpty() || (!isBodyweight && weightStr.isEmpty())) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val sets = setsStr.toIntOrNull()
        val reps = repsStr.toIntOrNull()
        val parsedWeight = if (isBodyweight) 0.0 else weightStr.toDoubleOrNull()

        if (sets == null || reps == null || (!isBodyweight && parsedWeight == null)) {
            Toast.makeText(this, "Invalid number inputs", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = sessionManager.getUserId()
        if (userId == -1L) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show()
            return
        }

        val setsVal: Int = sets ?: 0
        val repsVal: Int = reps ?: 0
        val weightVal: Double = parsedWeight ?: 0.0

        val workout = Workout(
            id = if (isEditMode) workoutId else 0,
            userId = userId,
            exerciseName = exercise,
            sets = setsVal,
            reps = repsVal,
            weight = weightVal,
            notes = "",
            workoutDate = dateStr
        )

        val result = if (isEditMode) {
            workoutDao.updateWorkout(workout).toLong()
        } else {
            workoutDao.insertWorkout(workout)
        }

        if (result > 0) {
            Toast.makeText(this, getString(R.string.workout_saved), Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Error saving workout", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadWgerExercises() {
        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getExercises().awaitResponse()
                }
                if (response.isSuccessful) {
                    val apiExercises = response.body()?.results ?: emptyList()
                    val names = apiExercises.mapNotNull { ex ->
                        val translation = ex.translations?.find { it.language == 2 }
                            ?: ex.translations?.firstOrNull()
                        translation?.name?.trim()?.ifEmpty { null }
                    }.distinct().sorted()

                    if (names.isNotEmpty()) {
                        val adapter = ArrayAdapter(
                            this@WorkoutFormActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            names
                        )
                        autoCompleteExercises.setAdapter(adapter)
                    }
                }
            } catch (e: Exception) {
                // Fallback to static list if network error
            }
        }
    }

    private fun updateFieldsVisibility() {
        val exerciseName = autoCompleteExercises.text.toString().trim()
        val isBodyweight = isBodyweightExercise(exerciseName)

        val dateParams = layoutDateInput.layoutParams as LinearLayout.LayoutParams
        if (isBodyweight) {
            layoutWeightInput.visibility = View.GONE
            dateParams.weight = 2.0f
            dateParams.marginStart = 0
        } else {
            layoutWeightInput.visibility = View.VISIBLE
            dateParams.weight = 1.0f
            dateParams.marginStart = (8 * resources.displayMetrics.density).toInt()
        }
        layoutDateInput.layoutParams = dateParams
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

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_workout)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.delete) { _, _ ->
                val rows = workoutDao.deleteWorkout(workoutId)
                if (rows > 0) {
                    Toast.makeText(this, getString(R.string.workout_deleted), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error deleting workout", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityJob.cancel()
    }
}
