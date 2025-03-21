package com.example.janus
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Log.d("LoginActivity", "Before setting content view")
        setContentView(R.layout.activity_login)
        Log.d("LoginActivity", "After setting content view")
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        val emailField = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.login_button)
        val signUpButton = findViewById<TextView>(R.id.signup_button)
        val guestButton = findViewById<Button>(R.id.guestButton)
        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                GlobalScope.launch {
                    try {
                        val result = auth.signInWithEmailAndPassword(email, password).await()
                        if (result.user?.isEmailVerified == true) {
                            Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, SearchActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "You don't have an account yet? Sign up now!", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Login failed: ${e.message}")
                        Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        guestButton.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
            finish()
        }
    }
}