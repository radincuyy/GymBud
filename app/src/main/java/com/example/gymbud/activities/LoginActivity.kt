package com.example.gymbud.activities

import android.content.Intent
import android.graphics.Matrix
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gymbud.R
import com.example.gymbud.database.UserDao
import com.example.gymbud.utils.SessionManager
import com.example.gymbud.utils.ThemeManager

class LoginActivity : AppCompatActivity() {

    private lateinit var ivHero: ImageView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignIn: Button
    private lateinit var tvSignUp: TextView
    private lateinit var tvError: TextView

    private lateinit var userDao: UserDao
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        setContentView(R.layout.activity_login)

        userDao = UserDao(this)
        session = SessionManager(this)

        if (session.isLoggedIn()) {
            navigateToMain()
            return
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        ivHero = findViewById(R.id.ivHero)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        tvSignUp = findViewById(R.id.tvSignUp)
        tvError = findViewById(R.id.tvError)

        adjustHeroImageTopCrop()
    }

    private fun setupListeners() {
        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validasi input
            if (email.isEmpty()) {
                showError(getString(R.string.email_required))
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showError(getString(R.string.password_required))
                return@setOnClickListener
            }

            val user = userDao.validateLogin(email, password)
            if (user != null) {
                session.createLoginSession(user.id, user.name, user.email)
                navigateToMain()
            } else {
                showError(getString(R.string.invalid_credentials))
            }
        }

        tvSignUp.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun adjustHeroImageTopCrop() {
        ivHero.post {
            val drawable = ivHero.drawable ?: return@post
            val dWidth = drawable.intrinsicWidth
            val dHeight = drawable.intrinsicHeight
            val vWidth = ivHero.width
            val vHeight = ivHero.height

            if (dWidth <= 0 || dHeight <= 0 || vWidth <= 0 || vHeight <= 0) return@post

            val scale = if (dWidth * vHeight > vWidth * dHeight) {
                vHeight.toFloat() / dHeight.toFloat()
            } else {
                vWidth.toFloat() / dWidth.toFloat()
            }

            val matrix = Matrix()
            matrix.setScale(scale, scale)
            ivHero.scaleType = ImageView.ScaleType.MATRIX
            ivHero.imageMatrix = matrix
        }
    }
}
