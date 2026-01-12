package com.example.wiremaster

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.hypot

class AnalysisActivity : AppCompatActivity() {

    private lateinit var wireView: WireAnnotationView
    private var currentBitmap: Bitmap? = null
    private var realSpan: Double = 0.0

    private lateinit var btnSetP1: Button
    private lateinit var btnSetP2: Button
    private lateinit var btnSetSag: Button

    private val colorBlue = "#2196F3".toColorInt()
    private val colorOrange = "#FF9800".toColorInt()
    private val colorActive = "#00E676".toColorInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)

        wireView = findViewById(R.id.wireAnnotationView)
        val btnCalc = findViewById<Button>(R.id.btnCalculateFinal)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val btnRotate = findViewById<Button>(R.id.btnRotate)
        val txtResult = findViewById<TextView>(R.id.txtFinalResult)

        btnSetP1 = findViewById(R.id.btnSetP1)
        btnSetP2 = findViewById(R.id.btnSetP2)
        btnSetSag = findViewById(R.id.btnSetSag)

        realSpan = intent.getDoubleExtra("SPAN", 0.0)
        val imageUriString = intent.getStringExtra("IMG_URI")

        wireView.onPointPlaced = {
            updateButtonColors(0)
            Toast.makeText(this, "Point Updated", Toast.LENGTH_SHORT).show()
        }

        if (imageUriString != null) {
            try {
                val uri = imageUriString.toUri()
                currentBitmap = loadScaledBitmap(uri)
                loadImageToView()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }

        btnRotate.setOnClickListener {
            currentBitmap?.let { bmp ->
                val matrix = Matrix()
                matrix.postRotate(90f)
                try {
                    val rotated =
                        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                    currentBitmap = rotated
                    loadImageToView()
                } catch (_: OutOfMemoryError) {
                    Toast.makeText(this, "Image too large to rotate", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnReset.setOnClickListener {
            wireView.resetView()
            "Result: --".also { txtResult.text = it }
            updateButtonColors(0)
        }

        btnSetP1.setOnClickListener {
            wireView.placementMode = 1
            wireView.invalidate()
            updateButtonColors(1)
            Toast.makeText(this, "Tap image to place LEFT DOT", Toast.LENGTH_SHORT).show()
        }

        btnSetSag.setOnClickListener {
            wireView.placementMode = 3
            wireView.invalidate()
            updateButtonColors(3)
            Toast.makeText(this, "Tap image to place SAG DOT", Toast.LENGTH_SHORT).show()
        }

        btnSetP2.setOnClickListener {
            wireView.placementMode = 2
            wireView.invalidate()
            updateButtonColors(2)
            Toast.makeText(this, "Tap image to place RIGHT DOT", Toast.LENGTH_SHORT).show()
        }

        btnCalc.setOnClickListener {
            val p1 = CatenaryCalculator.PointD(wireView.p1.x.toDouble(), wireView.p1.y.toDouble())
            val p2 = CatenaryCalculator.PointD(wireView.p2.x.toDouble(), wireView.p2.y.toDouble())
            val pSag =
                CatenaryCalculator.PointD(wireView.pSag.x.toDouble(), wireView.pSag.y.toDouble())

            val totalLength = CatenaryCalculator.calculateArcLength(realSpan, p1, p2, pSag)

            val slope = (p2.y - p1.y) / (p2.x - p1.x)
            val yOnLine = p1.y + slope * (pSag.x - p1.x)
            val sagPixels = abs(pSag.y - yOnLine)

            val pixelSpan = hypot(p2.x - p1.x, p2.y - p1.y)
            if (pixelSpan == 0.0) {
                "Error: Dots are on top of each other".also { txtResult.text = it }
                return@setOnClickListener
            }

            val conversionRatio = realSpan / pixelSpan
            val sagMeters = sagPixels * conversionRatio

            "Length: %.3f m\nSag: %.3f m".format(totalLength, sagMeters)
                .also { txtResult.text = it }

            updateButtonColors(0)
        }
    }

    private fun updateButtonColors(activeMode: Int) {
        btnSetP1.backgroundTintList = ColorStateList.valueOf(colorBlue)
        btnSetP2.backgroundTintList = ColorStateList.valueOf(colorBlue)
        btnSetSag.backgroundTintList = ColorStateList.valueOf(colorOrange)

        when (activeMode) {
            1 -> btnSetP1.backgroundTintList = ColorStateList.valueOf(colorActive)
            2 -> btnSetP2.backgroundTintList = ColorStateList.valueOf(colorActive)
            3 -> btnSetSag.backgroundTintList = ColorStateList.valueOf(colorActive)
        }
    }

    private fun loadImageToView() {
        wireView.displayBitmap = currentBitmap
        wireView.resetView()

        currentBitmap?.let { bmp ->
            if (org.opencv.android.OpenCVLoader.initDebug()) {
                try {
                    val result = AdvancedWireDetector.analyze(bmp)
                    wireView.p1 = result.p1
                    wireView.p2 = result.p2
                    wireView.pSag = result.pSag
                    wireView.invalidate()
                    Toast.makeText(this, "Wire Detected!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadScaledBitmap(uri: Uri): Bitmap? {
        var inputStream: InputStream? = contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        var sampleSize = 1
        val width = options.outWidth
        val height = options.outHeight
        val targetSize = 1080

        if (height > targetSize || width > targetSize) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / sampleSize) >= targetSize && (halfWidth / sampleSize) >= targetSize) {
                sampleSize *= 2
            }
        }

        inputStream = contentResolver.openInputStream(uri)
        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize
        return BitmapFactory.decodeStream(inputStream, null, options)
    }

    override fun onDestroy() {
        super.onDestroy()
        currentBitmap?.recycle()
    }
}