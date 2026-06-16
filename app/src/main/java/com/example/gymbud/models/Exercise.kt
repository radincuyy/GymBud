package com.example.gymbud.models

import com.google.gson.annotations.SerializedName

// WGER API response wrapper
data class ExerciseResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("next") val next: String?,
    @SerializedName("results") val results: List<ExerciseInfo>
)

data class ExerciseInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("category") val category: ExerciseCategory?,
    @SerializedName("images") val images: List<ExerciseImageInfo>?,
    @SerializedName("translations") val translations: List<ExerciseTranslation>?
)

data class ExerciseImageInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("image") val image: String,
    @SerializedName("is_main") val isMain: Boolean = false
)

data class ExerciseTranslation(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("language") val language: Int
)

data class Exercise(
    val id: Int,
    val name: String,
    val description: String = "",
    val category: Int = 0,
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
