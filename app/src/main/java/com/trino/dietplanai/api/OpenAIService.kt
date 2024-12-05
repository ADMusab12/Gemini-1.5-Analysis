package com.trino.dietplanai.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.trino.dietplanai.util.Constant.GEMINI_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class OpenAIService {
    private val apiKey = GEMINI_KEY

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    //todo for generate content
    suspend fun generateContent(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JsonObject().apply {
                add("contents", JsonObject().apply {
                    add("parts", JsonObject().apply {
                        addProperty("text", prompt)
                    })
                })
            }

            val jsonBody = requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "Empty Response Body"
                Log.d("checkobjectdetection", "Response: $responseBody")

                val jsonResponse = Gson().fromJson(responseBody, JsonObject::class.java)
                val candidates = jsonResponse.getAsJsonArray("candidates")
                val firstCandidate = candidates[0].asJsonObject
                val content = firstCandidate.getAsJsonObject("content")
                val parts = content.getAsJsonArray("parts")
                val text = parts[0].asJsonObject.get("text").asString

                Result.success(text)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e("checkobjectdetection", "Error: ${response.code} - $errorBody")
                Result.failure(Exception("API Error: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("checkobjectdetection", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun encodeImageToBase64(context: Context, imageUri: Uri): String {
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
    fun analyzeImage(context: Context, imageUri: Uri, callback: (Result<String>) -> Unit) {
        val imageBase64 = encodeImageToBase64(context, imageUri)
        Log.d("checkimageanalyze", "Base64 encoded image: $imageBase64")


        val jsonRequestBody = JsonObject().apply {
            add("contents", JsonArray().apply {
                add(JsonObject().apply {
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("text", "Details this image.")
                        })
                        add(JsonObject().apply {
                            add("inline_data", JsonObject().apply {
                                addProperty("mime_type", "image/jpeg")
                                addProperty("data", imageBase64)
                            })
                        })
                    })
                })
            })
        }

        val jsonBody = jsonRequestBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "Empty response"
                    //val jsonResponse = Gson().fromJson(responseBody, JsonObject::class.java)
                    //val analysisText = jsonResponse.getAsJsonObject("analysis")?.get("description")?.asString ?: "No description available"
                    Log.d("checkimageanalyze", "Full response: $responseBody")
                    Log.d("checkimageanalyze", "response: ${response.message}")
                    withContext(Dispatchers.Main) {
                        callback(Result.success(responseBody))
                    }
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.d("checkimageanalyze", "errorBody: $errorBody")
                    withContext(Dispatchers.Main) {
                        callback(Result.failure(Exception("API Error: ${response.code} - $errorBody")))
                    }
                }
            } catch (e: Exception) {
                Log.d("checkimageanalyze", "errorBody: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback(Result.failure(e))
                }
            }
        }
    }


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


    private fun encodeImage(image: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    //todo for analyze video
    fun analyzeVideo(context: Context, videoUri: Uri, callback: (Result<String>) -> Unit) {
        val frames = extractFramesFromVideo(context, videoUri)

        if (frames.isNotEmpty()) {
            Log.d("checkvideoinfo", "frames : ${frames.size}")

            val jsonRequestBody = JsonObject().apply {
                add("generationConfig", JsonObject().apply {
                    addProperty("temperature", 0.4)
                    addProperty("topK", 32)
                    addProperty("topP", 1.0)
                    addProperty("maxOutputTokens", 4096)
                })
                add("contents", JsonArray().apply {
                    add(JsonObject().apply {
                        add("parts", JsonArray().apply {
                            add(JsonObject().apply {
                                addProperty("text", "Analyze this video overall.")
                            })

                            frames.forEachIndexed { index, frame ->
                                val imageBase64 = encodeImage(frame)
                                Log.d("checkvideoinfo", "Processing frame $index")

                                add(JsonObject().apply {
                                    add("inline_data", JsonObject().apply {
                                        addProperty("mime_type", "image/jpeg")
                                        addProperty("data", imageBase64)
                                    })
                                })
                            }
                        })
                    })
                })
            }

            val jsonBody = jsonRequestBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody)
                .build()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: "Empty Response Body"
                        Log.d("checkvideoinfo", "Full response: $responseBody")
                        withContext(Dispatchers.Main) {
                            callback(Result.success(responseBody))
                        }
                    } else {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        Log.d("checkvideoinfo", "Error body: $errorBody")
                        withContext(Dispatchers.Main) {
                            callback(Result.failure(Exception("API Error: ${response.code} - $errorBody")))
                        }
                    }
                } catch (e: Exception) {
                    Log.d("checkvideoinfo", "Error: ${e.message}")
                    withContext(Dispatchers.Main) {
                        callback(Result.failure(e))
                    }
                }
            }
        } else {
            Log.e("checkvideoinfo", "Failed to extract frames from the video.")
            callback(Result.failure(Exception("Failed to extract frames from the video.")))
        }
    }

    //todo for analyze audio
    fun analyzeAudio(context: Context, audioUri: Uri, callback: (Result<String>) -> Unit) {
        // Convert audio to base64
        val inputStream: InputStream? = context.contentResolver.openInputStream(audioUri)
        if (inputStream == null) {
            callback(Result.failure(Exception("Unable to open audio file")))
            return
        }

        // Check file size
        val audioBytes = inputStream.readBytes()
        if (audioBytes.size > 10 * 1024 * 1024) { // Limit to 10MB
            callback(Result.failure(Exception("Audio file is too large. Maximum 10MB allowed.")))
            return
        }

        val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        Log.d("checkaudioanalyze", "Audio file size: ${audioBytes.size} bytes")
        Log.d("checkaudioanalyze", "Base64 encoded audio: ${audioBase64.take(100)}...")

        val jsonRequestBody = JsonObject().apply {
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", 0.4)
                addProperty("maxOutputTokens", 4096)
            })
            add("contents", JsonArray().apply {
                add(JsonObject().apply {
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("text", "Analyze the content of this audio file. Provide a detail summary of what the audio contains, including language, tone, and key content.")
                        })
                        add(JsonObject().apply {
                            add("inline_data", JsonObject().apply {
                                addProperty("mime_type", "audio/mpeg")
                                addProperty("data", audioBase64)
                            })
                        })
                    })
                })
            })
        }

        val jsonBody = jsonRequestBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

        // Increase timeout to 60 seconds
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "Empty response"
                    Log.d("checkaudioanalyze", "Full response: $responseBody")

                    // Parse the response to extract the text
                    val jsonResponse = Gson().fromJson(responseBody, JsonObject::class.java)
                    val candidates = jsonResponse.getAsJsonArray("candidates")
                    val content = candidates[0].asJsonObject.getAsJsonObject("content")
                    val parts = content.getAsJsonArray("parts")
                    val analysisText = parts[0].asJsonObject.get("text").asString

                    withContext(Dispatchers.Main) {
                        callback(Result.success(analysisText))
                    }
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.d("checkaudioanalyze", "Error body: $errorBody")
                    withContext(Dispatchers.Main) {
                        callback(Result.failure(Exception("API Error: ${response.code} - $errorBody")))
                    }
                }
            } catch (e: Exception) {
                Log.d("checkaudioanalyze", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback(Result.failure(e))
                }
            }
        }
    }
}
