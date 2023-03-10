package com.sanghm2.project_03_01.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import com.bumptech.glide.Glide
import com.sanghm2.project_03_01.R
import com.sanghm2.project_03_01.databinding.ActivityFullScreenImageBinding

class FullScreenImage : AppCompatActivity() {
    private lateinit var binding : ActivityFullScreenImageBinding
    private var imageLink : String = ""
    private lateinit var actionBar : ActionBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actionBar = supportActionBar!!
        actionBar.hide()
        imageLink = ""+intent.getStringExtra("uri")
        loadImageFull()
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }
    private fun loadImageFull() {
            Glide.with(this).load(imageLink).placeholder(R.drawable.ic_baseline_image_24).into(binding.imagePick)
    }
}