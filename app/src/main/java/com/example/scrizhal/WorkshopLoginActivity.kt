package com.example.scrizhal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WorkshopLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workshop_login)

        val loginField = findViewById<EditText>(R.id.login)
        val passwordField = findViewById<EditText>(R.id.password)
        val button = findViewById<Button>(R.id.button)
        val textError = findViewById<TextView>(R.id.textError)
        val progress = findViewById<RelativeLayout>(R.id.progress_bar)
        val tvMetropolitanLogin = findViewById<TextView>(R.id.tvMetropolitanLogin)

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

            if (login == Constants.WORKSHOP_LOGIN && password == Constants.WORKSHOP_PASSWORD) {
                FirestoreManager.registerToken(
                    role = FcmService.NOTIF_TYPE_WORKSHOP
                )
                progress.visibility = View.GONE
                val intent = Intent(this, WorkshopMainActivity::class.java)
                intent.putExtra("WORKSHOP_NAME", "Красноярская Ризница")
                startActivity(intent)
                finish()
            } else {
                textError.text = "Неверный логин или пароль"
                textError.visibility = View.VISIBLE
                progress.visibility = View.GONE
            }
        }

        tvMetropolitanLogin.setOnClickListener {
            finish()
        }
    }
}
