package com.trino.dietplanai.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trino.dietplanai.api.OpenAIService
import com.trino.dietplanai.util.Constant.gemini_reply
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

class DietPlanViewModel : ViewModel() {
    private val openAIService = OpenAIService()

    private val _dietPlanState = MutableStateFlow<UiState>(UiState.Initial)
    val dietPlanState: StateFlow<UiState> = _dietPlanState

    sealed class UiState {
        object Initial : UiState()
        object Loading : UiState()
        data class Success(val dietPlan: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    //todo diet plan
    fun fetchDietPlan(age: Int, weight: Double, height: Double, gender: String, activity: String, goal: String, preference: String) {
        val messaage = " Create a personalized diet plan for a $age-year-old $gender weighing $weight kg and $height cm tall.\n" +
                "                Activity level: $activity\n" +
                "                Goal: $goal\n" +
                "                Diet preferences: $preference\n" +
                "                Provide a comprehensive diet plan including:\n" +
                "                1. Daily calorie recommendation\n" +
                "                2. Meal breakdown (Breakfast, Lunch, Dinner)\n" +
                "                3. Snack suggestions\n" +
                "                4. Nutritional guidelines\n" +
                "                5. Hydration recommendations"
        viewModelScope.launch {
            _dietPlanState.value = UiState.Loading

            openAIService.generateContent(messaage)
                .onSuccess { dietPlan ->
                    _dietPlanState.value = UiState.Success(dietPlan)
                    gemini_reply = dietPlan
                    Log.d("checkviewmodel", "onSuccess: $dietPlan")
                }
                .onFailure { error ->
                    _dietPlanState.value = UiState.Error(error.message ?: "Unknown error")
                    Log.d("checkviewmodel", "onFailure: ${error.message}")
                }
        }
    }


    //todo food details
    fun fetchFoodDetails(foodName: String) {
        val messaage = "Provide the nutritional information for the food item: $foodName. The response should include:\n" +
                "        - Calories\n" +
                "        - Macronutrients (Carbs, Protein, Fats)\n" +
                "        - Micronutrients (e.g., vitamins, minerals)\n" +
                "        - Food Label\n" +
                "        - Composition(e.g., water, proteins, lipids)\n" +
                "        - Portion size and any other relevant information"
        viewModelScope.launch {
            _dietPlanState.value = UiState.Loading

            openAIService.generateContent(messaage)
                .onSuccess { foodDetails ->
                    _dietPlanState.value = UiState.Success(foodDetails)
                    gemini_reply = foodDetails
                    Log.d("checkviewmodel", "onSuccess: $foodDetails")
                }
                .onFailure { error ->
                    _dietPlanState.value = UiState.Error(error.message ?: "Unknown error")
                    Log.d("checkviewmodel", "onFailure: ${error.message}")
                }
        }
    }


    //todo food details
    fun fetchObjectDetails(text: String,value:Float) {
        val messaage = "I am getting text $text and confidence value $value from ml kit of each object detection provide me details about that"
        viewModelScope.launch {
            _dietPlanState.value = UiState.Loading

            openAIService.generateContent(messaage)
                .onSuccess { foodDetails ->
                    _dietPlanState.value = UiState.Success(foodDetails)
                    gemini_reply = foodDetails
                    Log.d("checkviewmodel", "onSuccess: $foodDetails")
                }
                .onFailure { error ->
                    _dietPlanState.value = UiState.Error(error.message ?: "Unknown error")
                    Log.d("checkviewmodel", "onFailure: ${error.message}")
                }
        }
    }

    //todo for image analyzer

    fun fetchImageAnalysis(context: Context, imageUri: Uri) {
        _dietPlanState.value = UiState.Loading

        openAIService.analyzeImage(context, imageUri) { result ->
            if (result.isSuccess) {
                val analysisText = result.getOrNull() ?: "No description available"
                _dietPlanState.value = UiState.Success(analysisText)
                Log.d("checkimageinfo", "analyzeImage: $analysisText")
            } else {
                // Handle failure case
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                _dietPlanState.value = UiState.Error("Failed to analyze image: $errorMessage")
                Log.d("checkimageinfo", "analyzeImage: Failed to analyze image")
            }
        }
    }

    fun fetchVideoAnalysis(context: Context, videoUri: Uri) {
        _dietPlanState.value = UiState.Loading
        val frames = extractFramesFromVideo(context, videoUri)
        Log.d("checkvideoanalysis", "frames : ${frames.size}")
        if (frames.isNotEmpty()) {
            openAIService.analyzeVideo(context, videoUri) { result ->
                if (result.isSuccess) {
                    val analysisText = result.getOrNull() ?: "No description available"
                    _dietPlanState.value = UiState.Success(analysisText)
                    Log.d("checkvideoanalysis", "analyzeVideo: $analysisText")
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    _dietPlanState.value = UiState.Error("Failed to analyze video: $errorMessage")
                    Log.d("checkvideoanalysis", "analyzeVideo: Failed to analyze video")
                }
            }
        } else {
            _dietPlanState.value = UiState.Error("Failed to extract frames from the video.")
            Log.d("checkvideoanalysis", "Failed to extract frames from video")
        }
    }

    //todo etract frames from videos
    private fun extractFramesFromVideo(context: Context, videoUri: Uri): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)

        val frames = mutableListOf<Bitmap>()
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L

        // Adjust frame interval based on video duration
        val frameInterval = when {
            duration <= 5000 -> 250000L // 250ms for very short videos
            duration <= 30000 -> 500000L // 500ms for short videos
            duration <= 60000 -> 1000000L // 1 second for medium-length videos
            else -> 2000000L // 2 seconds for longer videos
        }

        Log.d("checkvideoanalysis", "Video duration: $duration")

        var currentTime = 0L
        while (currentTime < duration) {
            Log.d("checkvideoanalysis", "Trying to extract frame at time: $currentTime")

            val frame = retriever.getFrameAtTime(
                currentTime,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            if (frame != null) {
                frames.add(frame)
                Log.d("checkvideoanalysis", "Frame extracted at time: $currentTime")
            }

            // Increment by the dynamic frame interval
            currentTime += frameInterval
        }

        // Add the last frame if not already included
        val lastFrameTime = duration * 1000 - 1 // Convert to microseconds
        val lastFrame = retriever.getFrameAtTime(
            lastFrameTime,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
        if (lastFrame != null && !frames.contains(lastFrame)) {
            frames.add(lastFrame)
        }

        retriever.release()

        // Log the number of frames extracted
        Log.d("checkvideoanalysis", "Extracted ${frames.size} frames.")
        return frames
    }


    //todo audio analyze
    fun fetchAudioAnalysis(context: Context, audioUri: Uri) {
        _dietPlanState.value = UiState.Loading

        openAIService.analyzeAudio(context, audioUri) { result ->
            if (result.isSuccess) {
                val analysisText = result.getOrNull() ?: "No description available"
                _dietPlanState.value = UiState.Success(analysisText)
                Log.d("checkaudioinfo", "analyzeAudio: $analysisText")
            } else {
                // Handle failure case
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                _dietPlanState.value = UiState.Error("Failed to analyze audio: $errorMessage")
                Log.d("checkaudioinfo", "analyzeAudio: Failed to analyze audio")
            }
        }
    }
}

