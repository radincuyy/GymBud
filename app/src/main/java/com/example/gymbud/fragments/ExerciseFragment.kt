package com.example.gymbud.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gymbud.R
import com.example.gymbud.activities.ExerciseDetailActivity
import com.example.gymbud.adapters.ExerciseGridAdapter
import com.example.gymbud.models.Exercise
import com.example.gymbud.models.ExerciseCategoryResponse
import com.example.gymbud.models.ExerciseImageResponse
import com.example.gymbud.models.ExerciseResponse
import com.example.gymbud.network.RetrofitClient
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class ExerciseFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var rvExercises: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var errorLayout: LinearLayout
    private lateinit var btnRetry: Button

    private lateinit var adapter: ExerciseGridAdapter
    private var allExercises = mutableListOf<Exercise>()
    private var filteredExercises = mutableListOf<Exercise>()
    
    private val categoryMap = mutableMapOf<Int, String>()
    private val imageMap = mutableMapOf<Int, String>()
    
    private val fragmentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_exercise, container, false)

        etSearch = view.findViewById(R.id.etSearch)
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)
        rvExercises = view.findViewById(R.id.rvExercises)
        pbLoading = view.findViewById(R.id.pbLoading)
        errorLayout = view.findViewById(R.id.errorLayout)
        btnRetry = view.findViewById(R.id.btnRetry)

        // RecyclerView setup
        rvExercises.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter = ExerciseGridAdapter(filteredExercises) { exercise ->
            // Tap card callback
            val intent = Intent(requireContext(), ExerciseDetailActivity::class.java).apply {
                putExtra("exercise_id", exercise.id)
                putExtra("exercise_name", exercise.name)
                putExtra("exercise_desc", exercise.description)
                putExtra("exercise_category", exercise.categoryName)
                putExtra("exercise_image", exercise.imageUrl)
            }
            startActivity(intent)
        }
        rvExercises.adapter = adapter

        // Search text listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterData()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Chips listener
        chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            filterData()
        }

        btnRetry.setOnClickListener {
            loadExerciseData()
        }

        loadExerciseData()

        return view
    }

    private fun loadExerciseData() {
        pbLoading.visibility = View.VISIBLE
        errorLayout.visibility = View.GONE
        rvExercises.visibility = View.GONE

        coroutineScope.launch {
            try {
                // Fetch in background
                withContext(Dispatchers.IO) {
                    // 1. Fetch categories
                    val catResponse = RetrofitClient.apiService.getCategories().awaitResponse()
                    if (catResponse.isSuccessful) {
                        catResponse.body()?.results?.forEach {
                            categoryMap[it.id] = it.name
                        }
                    }

                    // 2. Fetch images
                    val imgResponse = RetrofitClient.apiService.getExerciseImages().awaitResponse()
                    if (imgResponse.isSuccessful) {
                        imgResponse.body()?.results?.forEach {
                            imageMap[it.exerciseBase] = it.image
                        }
                    }

                    // 3. Fetch exercises
                    val exResponse = RetrofitClient.apiService.getExercises().awaitResponse()
                    if (exResponse.isSuccessful) {
                        val exercisesList = exResponse.body()?.results ?: emptyList()
                        allExercises.clear()
                        exercisesList.forEach { ex ->
                            val translation = ex.translations?.find { it.language == 2 }
                                ?: ex.translations?.firstOrNull()
                            val name = translation?.name ?: ""

                            if (name.isNotEmpty()) {
                                val rawDesc = translation?.description ?: ""
                                val cleanDesc = if (rawDesc.isNotEmpty()) {
                                    android.text.Html.fromHtml(rawDesc, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                                } else {
                                    ""
                                }
                                val categoryId = ex.category?.id ?: 0
                                val categoryName = ex.category?.name ?: categoryMap[categoryId] ?: "General"
                                
                                val imageFromInfo = ex.images?.find { it.isMain }?.image 
                                    ?: ex.images?.firstOrNull()?.image 
                                    ?: imageMap[ex.id] 
                                    // Fallback to WGER standard path using exercise base id if any
                                    ?: ""

                                val exercise = Exercise(
                                    id = ex.id,
                                    name = name,
                                    description = cleanDesc,
                                    category = categoryId,
                                    categoryName = categoryName,
                                    imageUrl = imageFromInfo
                                )
                                allExercises.add(exercise)
                            }
                        }
                    } else {
                        throw Exception("Error loading exercises")
                    }
                }

                // Show success UI
                pbLoading.visibility = View.GONE
                rvExercises.visibility = View.VISIBLE
                filterData()

            } catch (e: Exception) {
                // Show error UI
                pbLoading.visibility = View.GONE
                errorLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun filterData() {
        val query = etSearch.text.toString().trim().lowercase()
        val checkedChipId = chipGroupCategories.checkedChipId

        // WGER category ID mapping
        val selectedCategory = when (checkedChipId) {
            R.id.chipChest -> "chest"
            R.id.chipBack -> "back"
            R.id.chipLegs -> "legs"
            R.id.chipArms -> "arms"
            R.id.chipShoulders -> "shoulders"
            else -> "all"
        }

        filteredExercises.clear()
        allExercises.forEach { ex ->
            val matchesSearch = ex.name.lowercase().contains(query) || ex.description.lowercase().contains(query)
            val matchesCategory = selectedCategory == "all" || ex.categoryName.lowercase().contains(selectedCategory)

            if (matchesSearch && matchesCategory) {
                filteredExercises.add(ex)
            }
        }
        adapter.updateData(filteredExercises)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentJob.cancel()
    }
}
