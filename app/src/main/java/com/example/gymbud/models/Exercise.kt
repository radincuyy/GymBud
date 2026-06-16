package com.example.gymbud.models

import com.google.gson.annotations.SerializedName

// WGER API response wrapper
data class ExerciseResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("next") val next: String?,
    @SerializedName("results") val results: List<Exercise>
)

data class Exercise(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("category") val category: Int = 0,
    var categoryName: String = "",
    var imageUrl: String = ""
)

data class ExerciseCategoryResponse(
    @SerializedName("results") val results: List<ExerciseCategory>
)

data class ExerciseCategory(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class ExerciseImageResponse(
    @SerializedName("results") val results: List<ExerciseImage>
)

data class ExerciseImage(
    @SerializedName("id") val id: Int,
    @SerializedName("exercise_base") val exerciseBase: Int,
    @SerializedName("image") val image: String
)
