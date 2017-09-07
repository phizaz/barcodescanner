package me.dm7.barcodescanner.zxing.sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private var mClss: Class<*>? = null

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_main)
        launchActivity(FullScannerActivity::class.java)
    }

    fun launchActivity(clss: Class<*>) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mClss = clss
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA), ZXING_CAMERA_PERMISSION)
        } else {
            val intent = Intent(this, clss)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            ZXING_CAMERA_PERMISSION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mClss != null) {
                        val intent = Intent(this, mClss)
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this, "Please grant camera permission to use the QR Scanner", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    companion object {
        private val ZXING_CAMERA_PERMISSION = 1
    }
}
