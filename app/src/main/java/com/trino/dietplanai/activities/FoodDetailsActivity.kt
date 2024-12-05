package com.trino.dietplanai.activities

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.trino.dietplanai.R
import com.trino.dietplanai.databinding.ActivityFoodDetailsBinding
import com.trino.dietplanai.util.Constant.DIAMOND
import com.trino.dietplanai.viewmodel.DietPlanViewModel
import kotlinx.coroutines.launch

class FoodDetailsActivity : AppCompatActivity() {
    private lateinit var binding:ActivityFoodDetailsBinding
    private val viewModel: DietPlanViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        setupSpinner()

    }

    private fun setupSpinner(){

        val foodList = listOf(
            "Apple", "Banana", "Carrot", "Chicken Breast", "Broccoli",
            "Rice", "Potato", "Tomato", "Spinach", "Salmon",
            "Eggplant", "Zucchini", "Strawberry", "Blueberry", "Orange",
            "Grapes", "Pineapple", "Watermelon", "Cucumber", "Lettuce",
            "Mango", "Peach", "Plum", "Pear", "Avocado",
            "Cantaloupe", "Kale", "Cabbage", "Cauliflower", "Asparagus",
            "Beetroot", "Sweet Potato", "Mushroom", "Onion", "Garlic",
            "Bell Pepper", "Chili Pepper", "Green Beans", "Peas", "Lemon",
            "Lime", "Almonds", "Walnuts", "Cashews", "Pistachios",
            "Yogurt", "Cheese", "Milk", "Oatmeal", "Granola",
            "Bread", "Pasta", "Tuna", "Steak", "Turkey",
            "Bacon", "Sausage", "Ham", "Hot Dog", "Salami"
        )

        binding.spinnerFood.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, foodList)

        binding.buttonExplore.setOnClickListener {
            binding.progress.isVisible = true
            viewModel.fetchFoodDetails(binding.spinnerFood.selectedItem.toString())
        }
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.dietPlanState.collect { state ->
                when (state) {
                    is DietPlanViewModel.UiState.Loading -> {
                    }

                    is DietPlanViewModel.UiState.Success -> {
                        binding.progress.isVisible = false
                        binding.groupFood.isVisible = false
                        binding.textScroll.isVisible = true
                        val cleanedDietPlan = enhanceDietPlanText(state.dietPlan)
                        binding.textResult.text = cleanedDietPlan
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
            .replace("**", "") // Remove bold markers entirely
            .replace("* ", "$DIAMOND ") // Replace list indicators with bullets
            .replace("*", "$DIAMOND ") // Replace list indicators with bullets
            .replace("##", "") // Remove heading markers
            .replace("\n", "<br>")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(enhancedText, Html.FROM_HTML_MODE_COMPACT)
        } else {
            HtmlCompat.fromHtml(enhancedText, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }
    }
}