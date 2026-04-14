package com.example.scrizhal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ClericLoginActivity : AppCompatActivity() {

    private lateinit var prefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cleric_login)

        prefManager = SharedPrefManager(this)

        val loginField = findViewById<EditText>(R.id.login)
        val passwordField = findViewById<EditText>(R.id.password)
        val button = findViewById<Button>(R.id.button)
        val textError = findViewById<TextView>(R.id.textError)
        val progress = findViewById<RelativeLayout>(R.id.progress_bar)
        val tvBack = findViewById<TextView>(R.id.tvBackToMain)

        button.setOnClickListener {
            progress.visibility = View.VISIBLE
            textError.visibility = View.GONE

            val login = loginField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (login.isEmpty() || password.isEmpty()) {
                textError.text = "Заполните все поля"
                textError.visibility = View.VISIBLE
                progress.visibility = View.GONE
                return@setOnClickListener
            }

            // Один тестовый аккаунт клирика: Владимир Орлов (id = 14)
            if (login == "orlov" && password == "1234") {
                val clericId = 14
                prefManager.setCurrentClericId(clericId)
                prefManager.saveFcmUserRole(FcmService.NOTIF_TYPE_CLERIC)
                FirestoreManager.registerToken(
                    role = FcmService.NOTIF_TYPE_CLERIC,
                    userId = "cleric_$clericId"
                )
                progress.visibility = View.GONE
                startActivity(Intent(this, ClericProfileActivity::class.java))
                finish()
            } else {
                textError.text = "Неверный логин или пароль"
                textError.visibility = View.VISIBLE
                progress.visibility = View.GONE
            }
        }

        tvBack.setOnClickListener {
            finish()
        }
    }
}

