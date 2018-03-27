package com.yashoid.viewswitcher.sample;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.yashoid.viewswitcher.ViewSwitcher;

public class MainActivity extends AppCompatActivity {

    private Handler mHandler;

    private ViewSwitcher mViewSwitcher;

    private boolean mGoingForward = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        mViewSwitcher = findViewById(R.id.viewswitcher);
        mViewSwitcher.setStackAlpha(0.8f);

        mHandler.postDelayed(mSwitcher, 1000);
    }

    private Runnable mSwitcher = new Runnable() {

        @Override
        public void run() {
            if (mGoingForward) {
                if (!mViewSwitcher.switchForward()) {
                    mGoingForward = false;
                }
            }
            else {
                if (!mViewSwitcher.switchBack()) {
                    mGoingForward = true;
                }
            }

            mHandler.postDelayed(this, 1000);
        }

    };

}
