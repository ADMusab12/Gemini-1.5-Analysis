package com.trino.dietplanai.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.trino.dietplanai.custom.ObjectDetectionOverlayView
import com.trino.dietplanai.databinding.ActivityObjectDetectionBinding
import com.trino.dietplanai.util.Extension.showMessage
import com.trino.dietplanai.viewmodel.DietPlanViewModel

class ObjectDetectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityObjectDetectionBinding
    private lateinit var objectDetectionOverlay: ObjectDetectionOverlayView
    private val viewModel: DietPlanViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            showMessage("Camera permission is required")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObjectDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        objectDetectionOverlay = binding.objectDetectionOverlay

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val previewView = binding.previewView
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.surfaceProvider = previewView.surfaceProvider

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()

            val objectDetector: ObjectDetector = ObjectDetection.getClient(options)

            objectDetector.process(inputImage)
                .addOnSuccessListener { detectedObjects ->
                    runOnUiThread {
                        objectDetectionOverlay.setDetectedObjects(detectedObjects)
                    }


                    if (detectedObjects.isEmpty()) {
                        Log.d("ObjectDetectionInfo", "No objects detected in the current frame.")
                        binding.textResult.text = "No objects detected in the current frame."
                    } else {
                        var totalConfidence = 0.0f
                        var detectedObjectsCount = 0

                        for (obj in detectedObjects) {
                            val labels = obj.labels
                            for (label in labels) {
                                val confidence = label.confidence
                                totalConfidence += confidence
                                detectedObjectsCount++
                                Log.d("ObjectDetectionInfo", "Detected object: $label with confidence: $confidence")
                            }
                        }
                        binding.textResult.text = "Detected $detectedObjectsCount objects with total confidence: $totalConfidence"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ObjectDetectionInfo", "Object detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}