package com.pulseapp.android.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.pulseapp.android.R;

/**
 * Created by abc on 2/11/2016.
 */
public class VideoEditorActivity extends FragmentActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_editor);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void loadVideoEditorFragment(){

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
