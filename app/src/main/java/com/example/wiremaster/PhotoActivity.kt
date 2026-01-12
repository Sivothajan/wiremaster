package com.example.wiremaster

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

class PhotoActivity : AppCompatActivity(), SensorEventListener {

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var btnCapture: Button
    private lateinit var btnGallery: Button
    private lateinit var txtLevel: TextView
    private var realSpan: Double = 0.0

    private lateinit var sensorManager: SensorManager
    private var isPhoneVertical = false

    private val selectImageFromGalleryResult =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedUri ->
                val intent = Intent(this, AnalysisActivity::class.java)
                intent.putExtra("IMG_URI", selectedUri.toString())
                intent.putExtra("SPAN", realSpan)
                startActivity(intent)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        realSpan = intent.getDoubleExtra("SPAN", 0.0)

        btnCapture = findViewById(R.id.camera_capture_button)
        btnGallery = findViewById(R.id.btnGallery)
        txtLevel = findViewById(R.id.txtLevel)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        outputDirectory = getOutputDirectory()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }

        btnCapture.setOnClickListener { takePhoto() }

        btnGallery.setOnClickListener {
            selectImageFromGalleryResult.launch("image/*")
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("PhotoActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        if (!isPhoneVertical) {
            Toast.makeText(this, "Hold phone VERTICAL first!", Toast.LENGTH_SHORT).show()
            return
        }

        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS",
                Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("PhotoActivity", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val intent = Intent(this@PhotoActivity, AnalysisActivity::class.java)
                    intent.putExtra("IMG_URI", savedUri.toString())
                    intent.putExtra("SPAN", realSpan)
                    startActivity(intent)
                    finish()
                }
            })
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
            SensorManager.SENSOR_DELAY_UI
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {
            val z = event.values[2]

            if (abs(z) < 3.0) {
                isPhoneVertical = true
                btnCapture.backgroundTintList =
                    android.content.res.ColorStateList.valueOf("#00E676".toColorInt())

                "PERFECT LEVEL".also { txtLevel.text = it }
                txtLevel.setTextColor(Color.GREEN)
            } else {
                isPhoneVertical = false
                btnCapture.backgroundTintList =
                    android.content.res.ColorStateList.valueOf("#FF5252".toColorInt())

                "HOLD UPRIGHT".also { txtLevel.text = it }
                txtLevel.setTextColor(Color.RED)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()
            ?.let { File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }
}