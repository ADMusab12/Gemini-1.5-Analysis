package com.trino.dietplanai.activities

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.trino.dietplanai.R
import com.trino.dietplanai.databinding.ActivityDietPlanBinding
import com.trino.dietplanai.util.Constant.BULLET_START_CIRCLE
import com.trino.dietplanai.util.Extension.showMessage
import com.trino.dietplanai.viewmodel.DietPlanViewModel
import kotlinx.coroutines.launch

class DietPlanActivity : AppCompatActivity() {
    private lateinit var binding:ActivityDietPlanBinding
    private val viewModel: DietPlanViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDietPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        observeViewModel()
        getData()
    }

    private fun getData(){
        binding.apply {
            buttonGenerate.setOnClickListener {
                val age = etAge.text.toString().toIntOrNull()
                val weight = etWeight.text.toString().toDoubleOrNull()
                val height = etHeight.text.toString().toDoubleOrNull()
                val gender = spinnerGender.selectedItem.toString()
                val activity = spinnerActivity.selectedItem.toString()
                val goals = spinnerGoals.selectedItem.toString()
                val preference = spinnerPreference.selectedItem.toString()

                if (age != null && weight != null && height != null && activity.isNotEmpty() && goals.isNotEmpty() && preference.isNotEmpty()) {
                    progress.isVisible = true
                    viewModel.fetchDietPlan(age, weight, height, gender,activity,goals,preference)
                }else{
                    showMessage("Please fill all required fields")
                }
            }
        }
    }

    private fun setupSpinners() {
        val genderList = listOf("Male","Female")
        val activityLevels = listOf("Sedentary", "Light", "Moderate", "Active", "Very Active")
        val goals = listOf("Lose weight", "Gain weight", "Maintain weight")
        val dietPreferences = listOf("None", "Vegetarian", "Vegan", "Gluten-free")
        binding.spinnerGender.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderList)
        binding.spinnerActivity.adapter =ArrayAdapter(this, android.R.layout.simple_spinner_item, activityLevels)
        binding.spinnerGoals.adapter =ArrayAdapter(this, android.R.layout.simple_spinner_item, goals)
        binding.spinnerPreference.adapter =ArrayAdapter(this, android.R.layout.simple_spinner_item, dietPreferences)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.dietPlanState.collect { state ->
                when (state) {
                    is DietPlanViewModel.UiState.Loading -> {
                    }

                    is DietPlanViewModel.UiState.Success -> {
                        binding.progress.isVisible = false
                        binding.group.isVisible = false
                        binding.textScroll.isVisible = true
                        //val cleanedDietPlan = state.dietPlan.replace("*", "").replace("**", "<b>").replace("##", "<h2>").replace("\n", "<br>")
                        binding.textResult.text = enhanceDietPlanText(state.dietPlan)
                    }

                    is DietPlanViewModel.UiState.Error -> {

                    }

                    is DietPlanViewModel.UiState.Initial -> {

                    }
                }
            }
        }
    }

    private fun enhanceDietPlanText(apiResponse: String): Spanned {
        val enhancedText = apiResponse
            .replace("**", "")
            .replace("* ", "$BULLET_START_CIRCLE ")
            .replace("##", "")
            .replace("\n", "<br>")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(enhancedText, Html.FROM_HTML_MODE_COMPACT)
        } else {
            HtmlCompat.fromHtml(enhancedText, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }
    }
}