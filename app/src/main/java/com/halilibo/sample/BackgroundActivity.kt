package com.halilibo.sample

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.halilibo.bvpkotlin.BetterVideoPlayer

class BackgroundActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_background)

        val bvp = findViewById<BetterVideoPlayer>(R.id.bvp)
        bvp.setSource(Uri.parse("android.resource://" + packageName + "/" + R.raw.video))
        bvp.setVolume(0f, 0f)

        findViewById<View>(R.id.example_button).setOnClickListener {
            Toast.makeText(applicationContext,
                    "You can put a background video on your login page ;)",
                    Toast.LENGTH_LONG)
                    .show()
        }
    }
}
