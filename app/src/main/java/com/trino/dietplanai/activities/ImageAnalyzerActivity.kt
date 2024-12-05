package com.trino.dietplanai.activities

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.TextUtils.replace
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.trino.dietplanai.R
import com.trino.dietplanai.databinding.ActivityImageAnalyzerBinding
import com.trino.dietplanai.util.Constant.BULLET_START
import com.trino.dietplanai.util.Constant.BULLET_START_CIRCLE
import com.trino.dietplanai.util.Extension.showMessage
import com.trino.dietplanai.viewmodel.DietPlanViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

class ImageAnalyzerActivity : AppCompatActivity() {
    private lateinit var binding:ActivityImageAnalyzerBinding
    private val viewModel: DietPlanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageAnalyzerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                binding.imageGallery.isVisible = true
                Glide.with(this).load(it).into(binding.imageGallery)
                viewModel.fetchImageAnalysis(this, it)
                binding.progress.isVisible = true
            } ?: run {
                showMessage("No image selected")
            }
        }

        binding.buttonPick.setOnClickListener {
            pickImage.launch("image/*")
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.dietPlanState.collect { state ->
                when (state) {
                    is DietPlanViewModel.UiState.Loading -> {
                        binding.textResult.text = "Generating..."
                    }

                    is DietPlanViewModel.UiState.Success -> {
                        binding.progress.isVisible = false
                        binding.textScroll.isVisible = true
                        val parse =extractMainText(state.dietPlan)
                        binding.textResult.text = enhanceDietPlanText(parse)
                    }

                    is DietPlanViewModel.UiState.Error -> {
                        binding.textResult.text = "No description available"
                    }

                    is DietPlanViewModel.UiState.Initial -> {

                    }
                }
            }
        }
    }

    private fun extractMainText(response: String): String {
        return try {
            val jsonObject = JSONObject(response)
            val candidates = jsonObject.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    parts.getJSONObject(0).getString("text")
                } else {
                    "No description available."
                }
            } else {
                "No candidates found."
            }
        } catch (e: Exception) {
            "Failed to parse response: ${e.message}"
        }
    }
    private fun enhanceDietPlanText(apiResponse: String): Spanned {
        val enhancedText = apiResponse
            .replace("**", "") // Remove bold markers entirely
            .replace("* ", "$BULLET_START_CIRCLE ") // Replace list indicators with bullets
            .replace("##", "") // Remove heading markers
            .replace(">", "$BULLET_START_CIRCLE ") // Remove heading markers
            .replace("\n", "<br>")


        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(enhancedText, Html.FROM_HTML_MODE_COMPACT)
        } else {
            HtmlCompat.fromHtml(enhancedText, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }
    }

}