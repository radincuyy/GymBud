package com.example.gymbud.models

data class Workout(
    val id: Long = 0,
    val userId: Long,
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val weight: Double,
    val notes: String = "",
    val workoutDate: String,
    val createdAt: String = ""
)
