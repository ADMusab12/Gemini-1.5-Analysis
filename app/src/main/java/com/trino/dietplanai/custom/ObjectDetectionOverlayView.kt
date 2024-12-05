package com.trino.dietplanai.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.objects.DetectedObject

class ObjectDetectionOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        style = Paint.Style.FILL
    }

    private var detectedObjects: List<DetectedObject> = listOf()

    fun setDetectedObjects(objects: List<DetectedObject>) {
        detectedObjects = objects
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        for (obj in detectedObjects) {
            // Get the bounding box
            val boundingBox = obj.boundingBox
            
            // Draw the rectangle
            canvas.drawRect(
                boundingBox.left.toFloat(), 
                boundingBox.top.toFloat(), 
                boundingBox.right.toFloat(), 
                boundingBox.bottom.toFloat(), 
                paint
            )
            
            // Draw labels
            val labels = obj.labels
            if (labels.isNotEmpty()) {
                val label = labels[0]
                val labelText = "${label.text}: ${String.format("%.2f", label.confidence)}"
                canvas.drawText(
                    labelText, 
                    boundingBox.left.toFloat(), 
                    boundingBox.top.toFloat() - 10, 
                    textPaint
                )
            }
        }
    }
}