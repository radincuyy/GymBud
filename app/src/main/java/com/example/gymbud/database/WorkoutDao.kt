package com.example.gymbud.database

import android.content.ContentValues
import android.content.Context
import com.example.gymbud.models.Workout

class WorkoutDao(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    // CREATE
    fun insertWorkout(workout: Workout): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_WORKOUT_USER_ID, workout.userId)
            put(DatabaseHelper.COL_WORKOUT_EXERCISE, workout.exerciseName)
            put(DatabaseHelper.COL_WORKOUT_SETS, workout.sets)
            put(DatabaseHelper.COL_WORKOUT_REPS, workout.reps)
            put(DatabaseHelper.COL_WORKOUT_WEIGHT, workout.weight)
            put(DatabaseHelper.COL_WORKOUT_NOTES, workout.notes)
            put(DatabaseHelper.COL_WORKOUT_DATE, workout.workoutDate)
        }
        val id = db.insert(DatabaseHelper.TABLE_WORKOUTS, null, values)
        db.close()
        return id
    }

    // READ
    fun getWorkoutsByUser(userId: Long): List<Workout> {
        val workouts = mutableListOf<Workout>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_WORKOUTS,
            null,
            "${DatabaseHelper.COL_WORKOUT_USER_ID} = ?",
            arrayOf(userId.toString()),
            null, null,
            "${DatabaseHelper.COL_WORKOUT_DATE} DESC, ${DatabaseHelper.COL_WORKOUT_CREATED_AT} DESC"
        )

        while (cursor.moveToNext()) {
            workouts.add(cursorToWorkout(cursor))
        }
        cursor.close()
        db.close()
        return workouts
    }

    // READ
    fun getWorkoutsByDate(userId: Long, date: String): List<Workout> {
        val workouts = mutableListOf<Workout>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_WORKOUTS,
            null,
            "${DatabaseHelper.COL_WORKOUT_USER_ID} = ? AND ${DatabaseHelper.COL_WORKOUT_DATE} = ?",
            arrayOf(userId.toString(), date),
            null, null,
            "${DatabaseHelper.COL_WORKOUT_CREATED_AT} ASC"
        )

        while (cursor.moveToNext()) {
            workouts.add(cursorToWorkout(cursor))
        }
        cursor.close()
        db.close()
        return workouts
    }

    // READ
    fun getWorkoutById(id: Long): Workout? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_WORKOUTS,
            null,
            "${DatabaseHelper.COL_WORKOUT_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        var workout: Workout? = null
        if (cursor.moveToFirst()) {
            workout = cursorToWorkout(cursor)
        }
        cursor.close()
        db.close()
        return workout
    }

    // UPDATE
    fun updateWorkout(workout: Workout): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_WORKOUT_EXERCISE, workout.exerciseName)
            put(DatabaseHelper.COL_WORKOUT_SETS, workout.sets)
            put(DatabaseHelper.COL_WORKOUT_REPS, workout.reps)
            put(DatabaseHelper.COL_WORKOUT_WEIGHT, workout.weight)
            put(DatabaseHelper.COL_WORKOUT_NOTES, workout.notes)
            put(DatabaseHelper.COL_WORKOUT_DATE, workout.workoutDate)
        }
        val rows = db.update(
            DatabaseHelper.TABLE_WORKOUTS,
            values,
            "${DatabaseHelper.COL_WORKOUT_ID} = ?",
            arrayOf(workout.id.toString())
        )
        db.close()
        return rows
    }

    // DELETE
    fun deleteWorkout(id: Long): Int {
        val db = dbHelper.writableDatabase
        val rows = db.delete(
            DatabaseHelper.TABLE_WORKOUTS,
            "${DatabaseHelper.COL_WORKOUT_ID} = ?",
            arrayOf(id.toString())
        )
        db.close()
        return rows
    }

    fun getWeeklyWorkoutCount(userId: Long): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_WORKOUTS} 
               WHERE ${DatabaseHelper.COL_WORKOUT_USER_ID} = ? 
               AND ${DatabaseHelper.COL_WORKOUT_DATE} >= date('now', '-7 days')""",
            arrayOf(userId.toString())
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return count
    }

    fun getWeeklyVolume(userId: Long): Double {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """SELECT COALESCE(SUM(${DatabaseHelper.COL_WORKOUT_SETS} * ${DatabaseHelper.COL_WORKOUT_REPS} * ${DatabaseHelper.COL_WORKOUT_WEIGHT}), 0) 
               FROM ${DatabaseHelper.TABLE_WORKOUTS} 
               WHERE ${DatabaseHelper.COL_WORKOUT_USER_ID} = ? 
               AND ${DatabaseHelper.COL_WORKOUT_DATE} >= date('now', '-7 days')""",
            arrayOf(userId.toString())
        )
        var volume = 0.0
        if (cursor.moveToFirst()) {
            volume = cursor.getDouble(0)
        }
        cursor.close()
        db.close()
        return volume
    }

    fun getWeeklyDistribution(userId: Long): Map<String, Int> {
        val distribution = linkedMapOf(
            "Mon" to 0, "Tue" to 0, "Wed" to 0,
            "Thu" to 0, "Fri" to 0, "Sat" to 0, "Sun" to 0
        )
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """SELECT 
                CASE CAST(strftime('%w', ${DatabaseHelper.COL_WORKOUT_DATE}) AS INTEGER)
                    WHEN 0 THEN 'Sun' WHEN 1 THEN 'Mon' WHEN 2 THEN 'Tue'
                    WHEN 3 THEN 'Wed' WHEN 4 THEN 'Thu' WHEN 5 THEN 'Fri'
                    WHEN 6 THEN 'Sat'
                END as day_name,
                COUNT(*) as count
               FROM ${DatabaseHelper.TABLE_WORKOUTS}
               WHERE ${DatabaseHelper.COL_WORKOUT_USER_ID} = ?
               AND ${DatabaseHelper.COL_WORKOUT_DATE} >= date('now', '-7 days')
               GROUP BY day_name""",
            arrayOf(userId.toString())
        )
        while (cursor.moveToNext()) {
            val day = cursor.getString(0)
            val count = cursor.getInt(1)
            if (day != null) {
                distribution[day] = count
            }
        }
        cursor.close()
        db.close()
        return distribution
    }

    fun getCurrentStreak(userId: Long): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """SELECT DISTINCT ${DatabaseHelper.COL_WORKOUT_DATE} 
               FROM ${DatabaseHelper.TABLE_WORKOUTS}
               WHERE ${DatabaseHelper.COL_WORKOUT_USER_ID} = ?
               ORDER BY ${DatabaseHelper.COL_WORKOUT_DATE} DESC""",
            arrayOf(userId.toString())
        )

        var streak = 0
        var previousDate = ""

        while (cursor.moveToNext()) {
            val date = cursor.getString(0)
            if (streak == 0) {
                streak = 1
                previousDate = date
            } else {
                val prevParts = previousDate.split("-").map { it.toInt() }
                val currParts = date.split("-").map { it.toInt() }

                val prevDay = prevParts[0] * 365 + prevParts[1] * 30 + prevParts[2]
                val currDay = currParts[0] * 365 + currParts[1] * 30 + currParts[2]

                if (prevDay - currDay == 1) {
                    streak++
                    previousDate = date
                } else {
                    break
                }
            }
        }
        cursor.close()
        db.close()
        return streak
    }

    private fun cursorToWorkout(cursor: android.database.Cursor): Workout {
        return Workout(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WORKOUT_ID)),
            userId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WORKOUT_USER_ID)),
            exerciseName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WORKOUT_EXERCISE)),
            sets = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WORKOUT_SETS)),
            reps = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WORKOUT_REPS)),
            weight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WORKOUT_WEIGHT)),
            notes = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WORKOUT_NOTES)) ?: "",
            workoutDate = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WORKOUT_DATE)),
            createdAt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WORKOUT_CREATED_AT)) ?: ""
        )
    }
}
