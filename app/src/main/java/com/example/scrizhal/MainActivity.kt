package com.example.scrizhal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefManager = SharedPrefManager(this)

        if (prefManager.getSavedLogin() == null) {
            prefManager.saveUser(Constants.DEFAULT_LOGIN, Constants.DEFAULT_PASSWORD)
        }

        if (prefManager.isLoggedIn()) {
            goToDashboard(prefManager.getCurrentUser() ?: "")
            return
        }

        setContentView(R.layout.activity_main)

        val spinnerRegion = findViewById<Spinner>(R.id.spinnerRegion)
        val regions = arrayOf("Красноярский край")
        spinnerRegion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, regions)

        val loginField = findViewById<EditText>(R.id.login)
        val passwordField = findViewById<EditText>(R.id.password)
        val button = findViewById<Button>(R.id.button)
        val textError = findViewById<TextView>(R.id.textError)
        val progress = findViewById<RelativeLayout>(R.id.progress_bar)
        val tvWorkshopLogin = findViewById<TextView>(R.id.tvWorkshopLogin)
        val tvClericLogin = findViewById<TextView>(R.id.tvClericLogin)

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

            val savedLogin = prefManager.getSavedLogin()
            val savedPassword = prefManager.getSavedPassword()

            if (login == savedLogin && password == savedPassword) {
                prefManager.setLoggedIn(login)
                prefManager.saveFcmUserRole(FcmService.NOTIF_TYPE_METROPOLITAN)
                FirestoreManager.registerToken(
                    role = FcmService.NOTIF_TYPE_METROPOLITAN
                )
                progress.visibility = View.GONE
                goToDashboard(login)
            } else {
                textError.text = "Неверный логин или пароль"
                textError.visibility = View.VISIBLE
                progress.visibility = View.GONE
            }
        }

        tvWorkshopLogin.setOnClickListener {
            startActivity(Intent(this, WorkshopLoginActivity::class.java))
        }

        tvClericLogin.setOnClickListener {
            startActivity(Intent(this, ClericLoginActivity::class.java))
        }
    }

    private fun goToDashboard(username: String) {
        val intent = Intent(this, Main::class.java)
        intent.putExtra(Constants.KEY_USERNAME, username)
        startActivity(intent)
        finish()
    }
}
