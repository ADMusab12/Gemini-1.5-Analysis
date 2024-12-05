package com.trino.dietplanai.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.trino.dietplanai.R
import com.trino.dietplanai.databinding.ActivityMainBinding
import com.trino.dietplanai.util.Extension.navigation

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            //todo lottie animation
            lottieGemini.setAnimation(R.raw.gemini)
            lottieGemini.playAnimation()

            //todo clicks

            imageDietPlan.setOnClickListener {
                navigation(DietPlanActivity::class.java)
            }

            imageFoodDetails.setOnClickListener {
                navigation(FoodDetailsActivity::class.java)
            }

            imageAnalyze.setOnClickListener {
                navigation(ImageAnalyzerActivity::class.java)
            }

            imageVideoAnalyze.setOnClickListener {
                navigation(VideoAnalyzeActivity::class.java)
            }

            imageAudioAnalyze.setOnClickListener {
                navigation(AudioAnalyzeActivity::class.java)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.lottieGemini.pauseAnimation()
    }
}