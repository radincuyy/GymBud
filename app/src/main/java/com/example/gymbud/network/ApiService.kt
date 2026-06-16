package com.example.gymbud.network

import com.example.gymbud.models.ExerciseCategoryResponse
import com.example.gymbud.models.ExerciseImageResponse
import com.example.gymbud.models.ExerciseResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("exerciseinfo/")
    fun getExercises(
        @Query("format") format: String = "json",
        @Query("language") language: Int = 2,   // English
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Call<ExerciseResponse>

    @GET("exerciseinfo/")
    fun getExercisesByCategory(
        @Query("format") format: String = "json",
        @Query("language") language: Int = 2,
        @Query("category") category: Int,
        @Query("limit") limit: Int = 50
    ): Call<ExerciseResponse>

    @GET("exercisecategory/")
    fun getCategories(
        @Query("format") format: String = "json"
    ): Call<ExerciseCategoryResponse>

    @GET("exerciseimage/")
    fun getExerciseImages(
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 100
    ): Call<ExerciseImageResponse>
}
