package com.halilibo.sample;

import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.halilibo.bettervideoplayer.BetterVideoPlayer;

public class MainActivity extends AppCompatActivity {

    private BetterVideoPlayer bvp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bvp = (BetterVideoPlayer) findViewById(R.id.bvp);

        bvp.setAutoPlay(true);
        //bvp.setSource(Uri.parse("https://lh3.googleusercontent.com/2dj-HJDlNxdZbGpHyqlXkT5lsoeM1hYBMMk6Bd7ulcwJHqwQeHC5Edp1CLxJ_PpZU8Sq1pq0bk8=m18"));
        bvp.setSource(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video));
        bvp.setSubtitle(R.raw.en);
        bvp.setHideControlsOnPlay(true);
        bvp.setMenuCallback(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.action_settings){
                    Snackbar.make(bvp, "LOOOOOL", Snackbar.LENGTH_LONG).show();
                }
                return false;
            }
        });
    }
}
