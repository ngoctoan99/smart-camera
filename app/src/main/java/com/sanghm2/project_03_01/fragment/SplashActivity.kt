package com.sanghm2.project_03_01.fragment

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.ActionBar
import com.sanghm2.project_03_01.MainActivity
import com.sanghm2.project_03_01.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var actionBar: ActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actionBar = supportActionBar!!
        actionBar.hide()
        /// handle screen show about 1500  == 1.5s
        Handler().postDelayed(Runnable {
            startActivity(Intent(this , MainActivity::class.java))
            finish()
        },1500)
    }
}