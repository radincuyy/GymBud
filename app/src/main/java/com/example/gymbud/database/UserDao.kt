package com.example.gymbud.database

import android.content.ContentValues
import android.content.Context
import com.example.gymbud.models.User

class UserDao(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun insertUser(user: User): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_USER_NAME, user.name)
            put(DatabaseHelper.COL_USER_EMAIL, user.email)
            put(DatabaseHelper.COL_USER_PASSWORD, user.password)
        }
        val id = db.insert(DatabaseHelper.TABLE_USERS, null, values)
        db.close()
        return id
    }

    fun getUserByEmail(email: String): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            null,
            "${DatabaseHelper.COL_USER_EMAIL} = ?",
            arrayOf(email),
            null, null, null
        )

        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_EMAIL)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PASSWORD)),
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_CREATED_AT)) ?: ""
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun getUserById(id: Long): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            null,
            "${DatabaseHelper.COL_USER_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_EMAIL)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PASSWORD)),
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_CREATED_AT)) ?: ""
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun emailExists(email: String): Boolean {
        return getUserByEmail(email) != null
    }

    fun validateLogin(email: String, password: String): User? {
        val user = getUserByEmail(email)
        return if (user != null && user.password == password) user else null
    }
}
