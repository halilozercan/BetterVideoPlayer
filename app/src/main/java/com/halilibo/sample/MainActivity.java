package com.halilibo.sample;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.halilibo.bettervideoplayer.BetterVideoCallback;
import com.halilibo.bettervideoplayer.BetterVideoPlayer;
import com.halilibo.bettervideoplayer.subtitle.CaptionsView;

public class MainActivity extends AppCompatActivity {

    private BetterVideoPlayer bvp;
    private String TAG = "BetterSample";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.background_activity_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, BackgroundActivity.class));
            }
        });

        findViewById(R.id.fulscreen_activity_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, FullscreenActivity.class));
            }
        });

        bvp = findViewById(R.id.bvp);

        if(savedInstanceState == null) {
            bvp.setAutoPlay(true);
            bvp.setSource(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video));
            bvp.setCaptions(R.raw.sub, CaptionsView.CMime.SUBRIP);
        }

        bvp.setHideControlsOnPlay(true);

        bvp.getToolbar().inflateMenu(R.menu.menu_dizi);
        bvp.getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.action_enable_swipe:
                        bvp.enableSwipeGestures(getWindow());
                        break;
                    case R.id.action_enable_double_tap:
                        bvp.enableDoubleTapGestures(5000);
                        break;
                    case R.id.action_disable_swipe:
                        bvp.disableGestures();
                        break;
                    case R.id.action_show_bottombar:
                        bvp.setBottomProgressBarVisibility(true);
                        break;
                    case R.id.action_hide_bottombar:
                        bvp.setBottomProgressBarVisibility(false);
                        break;
                    case R.id.action_show_captions:
                        bvp.setCaptions(R.raw.sub, CaptionsView.CMime.SUBRIP);
                        break;
                    case R.id.action_hide_captions:
                        bvp.removeCaptions();
                }
                return false;
            }
        });

        bvp.enableSwipeGestures(getWindow());

        bvp.setCallback(new BetterVideoCallback() {
            @Override
            public void onStarted(BetterVideoPlayer player) {
                //Log.i(TAG, "Started");
            }

            @Override
            public void onPaused(BetterVideoPlayer player) {
                //Log.i(TAG, "Paused");
            }

            @Override
            public void onPreparing(BetterVideoPlayer player) {
                //Log.i(TAG, "Preparing");
            }

            @Override
            public void onPrepared(BetterVideoPlayer player) {
                //Log.i(TAG, "Prepared");
            }

            @Override
            public void onBuffering(int percent) {
                //Log.i(TAG, "Buffering " + percent);
            }

            @Override
            public void onError(BetterVideoPlayer player, Exception e) {
                //Log.i(TAG, "Error " +e.getMessage());
            }

            @Override
            public void onCompletion(BetterVideoPlayer player) {
                //Log.i(TAG, "Completed");
            }

            @Override
            public void onToggleControls(BetterVideoPlayer player, boolean isShowing) {

            }
        });
    }

    @Override
    public void onPause(){
        bvp.pause();
        super.onPause();
    }
}
