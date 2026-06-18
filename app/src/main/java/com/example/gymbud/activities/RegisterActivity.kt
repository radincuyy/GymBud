package com.example.gymbud.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gymbud.R
import com.example.gymbud.database.UserDao
import com.example.gymbud.models.User
import com.example.gymbud.utils.ThemeManager

class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvSignIn: TextView
    private lateinit var tvError: TextView

    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        setContentView(R.layout.activity_register)

        userDao = UserDao(this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvSignIn = findViewById(R.id.tvSignIn)
        tvError = findViewById(R.id.tvError)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Validasi input
            if (name.isEmpty()) {
                showError(getString(R.string.name_required))
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                showError(getString(R.string.email_required))
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showError(getString(R.string.password_required))
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                showError(getString(R.string.password_mismatch))
                return@setOnClickListener
            }
            if (userDao.emailExists(email)) {
                showError(getString(R.string.email_exists))
                return@setOnClickListener
            }

            // Insert user ke SQLite
            val user = User(name = name, email = email, password = password)
            val id = userDao.insertUser(user)

            if (id > 0) {
                Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        tvSignIn.setOnClickListener {
            finish()
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
}
