package com.halilibo.sample;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.halilibo.bvpkotlin.BetterVideoPlayer;
import com.halilibo.bvpkotlin.VideoCallback;

import org.jetbrains.annotations.NotNull;

import androidx.appcompat.app.AppCompatActivity;

public class BackgroundActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background);

        BetterVideoPlayer bvp = findViewById(R.id.bvp);
        bvp.setSource(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video));
        bvp.setCallback(new VideoCallback() {
            @Override
            public void onStarted(@NotNull BetterVideoPlayer player) {

            }

            @Override
            public void onPaused(@NotNull BetterVideoPlayer player) {

            }

            @Override
            public void onPreparing(@NotNull BetterVideoPlayer player) {

            }

            @Override
            public void onPrepared(@NotNull BetterVideoPlayer player) {
                player.setVolume(0,0);
            }

            @Override
            public void onBuffering(int percent) {

            }

            @Override
            public void onError(@NotNull BetterVideoPlayer player, Exception e) {

            }

            @Override
            public void onCompletion(@NotNull BetterVideoPlayer player) {

            }

            @Override
            public void onToggleControls(@NotNull BetterVideoPlayer player, boolean isShowing) {

            }
        });

        findViewById(R.id.example_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),
                    "You can put a background video on your login page ;)",
                    Toast.LENGTH_LONG)
                    .show();
            }
        });
    }
}
