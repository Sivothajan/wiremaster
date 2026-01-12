package com.example.wiremaster

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import kotlin.math.sqrt

class ArActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private var anchorNode1: AnchorNode? = null
    private var anchorNode2: AnchorNode? = null
    private var sphereRenderable: ModelRenderable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)

        setSupportActionBar(findViewById(R.id.my_toolbar))

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment

        MaterialFactory.makeOpaqueWithColor(this, Color(android.graphics.Color.RED))
            .thenAccept { material ->
                sphereRenderable = ShapeFactory.makeSphere(0.05f, Vector3(0f, 0f, 0f), material)
            }

        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            handleAddAnchor(hitResult)
        }

        findViewById<Button>(R.id.btnResetAr).setOnClickListener {
            clearAnchors()
            Toast.makeText(this, "Points cleared", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnNext).setOnClickListener {
            if (anchorNode1 != null && anchorNode2 != null) {
                val dist = calculateDistance()
                val intent = Intent(this, PhotoActivity::class.java)
                intent.putExtra("SPAN", dist)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please mark 2 points first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                showAboutDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleAddAnchor(hitResult: HitResult) {
        if (sphereRenderable == null) return

        if (anchorNode1 == null) {
            anchorNode1 = AnchorNode(hitResult.createAnchor()).apply {
                setParent(arFragment.arSceneView.scene)
                renderable = sphereRenderable
            }
            Toast.makeText(this, "Point A Set. Walk to Point B.", Toast.LENGTH_SHORT).show()
        } else if (anchorNode2 == null) {
            anchorNode2 = AnchorNode(hitResult.createAnchor()).apply {
                setParent(arFragment.arSceneView.scene)
                renderable = sphereRenderable
            }
            val dist = calculateDistance()
            "Distance: %.2f m".format(dist)
                .also { findViewById<TextView>(R.id.txtDistance).text = it }
        } else {
            Toast.makeText(
                this,
                "Points already set. Click Reset to try again.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun clearAnchors() {
        anchorNode1?.let { arFragment.arSceneView.scene.removeChild(it); it.anchor?.detach() }
        anchorNode2?.let { arFragment.arSceneView.scene.removeChild(it); it.anchor?.detach() }

        anchorNode1 = null
        anchorNode2 = null

        "Mark 2 Points".also { findViewById<TextView>(R.id.txtDistance).text = it }
    }

    private fun calculateDistance(): Double {
        val p1 = anchorNode1!!.anchor!!.pose
        val p2 = anchorNode2!!.anchor!!.pose
        val dx = p1.tx() - p2.tx()
        val dy = p1.ty() - p2.ty()
        val dz = p1.tz() - p2.tz()
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble())
    }

    private fun showAboutDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("About WireMaster")
        builder.setMessage("Wire Length Measurement App\n\nUsing ARCore and Catenary Physics.\n\nCreated by: Sivothayan Sivasiva.")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }
}