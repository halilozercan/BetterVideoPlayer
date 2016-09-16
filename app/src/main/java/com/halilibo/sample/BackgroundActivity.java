package com.halilibo.sample;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.halilibo.bettervideoplayer.BetterVideoPlayer;

public class BackgroundActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background);

        BetterVideoPlayer bvp = (BetterVideoPlayer) findViewById(R.id.bvp);
        bvp.setSource(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video));
    }
}
