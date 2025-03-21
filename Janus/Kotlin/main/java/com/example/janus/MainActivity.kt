
package com.example.janus
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.janus.data.Repository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Repository.initialize(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        GlobalScope.launch {
            delay(3000L)
            Log.d("MainActivity", "Navigating to LoginActivity")
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }
}
