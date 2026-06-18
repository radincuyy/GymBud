package com.example.gymbud.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.gymbud.R
import com.example.gymbud.utils.ThemeManager

class ExerciseDetailActivity : AppCompatActivity() {

    private lateinit var ivDetailImage: ImageView
    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailDesc: TextView
    private lateinit var btnWatchTutorial: Button
    private lateinit var btnFindGym: Button
    private lateinit var btnShareExercise: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager(this).applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail)

        // Bind Views
        ivDetailImage = findViewById(R.id.ivDetailImage)
        tvDetailName = findViewById(R.id.tvDetailName)
        tvDetailCategory = findViewById(R.id.tvDetailCategory)
        tvDetailDesc = findViewById(R.id.tvDetailDesc)
        btnWatchTutorial = findViewById(R.id.btnWatchTutorial)
        btnFindGym = findViewById(R.id.btnFindGym)
        btnShareExercise = findViewById(R.id.btnShareExercise)

        // Load Intent Extras
        val name = intent.getStringExtra("exercise_name") ?: "Exercise"
        val category = intent.getStringExtra("exercise_category") ?: "General"
        val desc = intent.getStringExtra("exercise_desc") ?: ""
        val imageUrl = intent.getStringExtra("exercise_image") ?: ""

        // Set Details
        tvDetailName.text = name
        tvDetailCategory.text = category
        tvDetailDesc.text = desc.ifEmpty { "No description available." }

        if (imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(ivDetailImage)
        } else {
            ivDetailImage.setImageResource(R.drawable.ic_launcher_background)
        }

        // Implicit Intent: YouTube search
        btnWatchTutorial.setOnClickListener {
            val query = Uri.encode("how to do $name")
            val uri = Uri.parse("https://www.youtube.com/results?search_query=$query")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        // Implicit Intent: Google Maps search for nearby gyms
        btnFindGym.setOnClickListener {
            val uri = Uri.parse("geo:0,0?q=gym")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps") // target Google Maps if installed
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback to web browser search
                val webUri = Uri.parse("https://www.google.com/maps/search/gym")
                startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        }

        // Implicit Intent: Share sheet
        btnShareExercise.setOnClickListener {
            val shareText = "Check out this exercise on GymBud: $name ($category)\n\nDescription: $desc"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_exercise)))
        }
    }
}
