package com.example.gymbud.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "gymbud.db"
        const val DATABASE_VERSION = 1

        // Users table
        const val TABLE_USERS = "users"
        const val COL_USER_ID = "id"
        const val COL_USER_NAME = "name"
        const val COL_USER_EMAIL = "email"
        const val COL_USER_PASSWORD = "password"
        const val COL_USER_CREATED_AT = "created_at"

        // Workouts table
        const val TABLE_WORKOUTS = "workouts"
        const val COL_WORKOUT_ID = "id"
        const val COL_WORKOUT_USER_ID = "user_id"
        const val COL_WORKOUT_EXERCISE = "exercise_name"
        const val COL_WORKOUT_SETS = "sets"
        const val COL_WORKOUT_REPS = "reps"
        const val COL_WORKOUT_WEIGHT = "weight"
        const val COL_WORKOUT_NOTES = "notes"
        const val COL_WORKOUT_DATE = "workout_date"
        const val COL_WORKOUT_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_NAME TEXT NOT NULL,
                $COL_USER_EMAIL TEXT NOT NULL UNIQUE,
                $COL_USER_PASSWORD TEXT NOT NULL,
                $COL_USER_CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()

        val createWorkoutsTable = """
            CREATE TABLE $TABLE_WORKOUTS (
                $COL_WORKOUT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_WORKOUT_USER_ID INTEGER NOT NULL,
                $COL_WORKOUT_EXERCISE TEXT NOT NULL,
                $COL_WORKOUT_SETS INTEGER NOT NULL,
                $COL_WORKOUT_REPS INTEGER NOT NULL,
                $COL_WORKOUT_WEIGHT REAL NOT NULL,
                $COL_WORKOUT_NOTES TEXT,
                $COL_WORKOUT_DATE TEXT NOT NULL,
                $COL_WORKOUT_CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY ($COL_WORKOUT_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID)
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createWorkoutsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WORKOUTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }
}
