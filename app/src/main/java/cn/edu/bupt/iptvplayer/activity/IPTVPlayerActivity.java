/*
 * Copyright (C) 2013 yixia.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.edu.bupt.iptvplayer.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;

import java.io.IOException;

import cn.edu.bupt.iptvplayer.R;
import cn.edu.bupt.iptvplayer.utils.Global;
import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.MediaPlayer.OnVideoSizeChangedListener;

public class IPTVPlayerActivity extends Activity implements OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener, SurfaceHolder.Callback {

    private static final String TAG = IPTVPlayerActivity.class.getName();
    private String path="";
    private int videoWidth;
    private int videoHeight;
    private long currentPosition = 0;
    private View controlView;
    private SeekBar seekPosition;
    private MediaPlayer mediaPlayer;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Intent intent;
    private boolean isVideoSizeKnown = false;
    private boolean isVideoReadyToBePlayed = false;
    private int visibleFlag = 1;

    private GestureDetector mGestureDetector;
    private AudioManager mAudioManager;   //获取音频管理器
    private int mMaxVolume;
    private int mVolume = -1;
    private float mBrightness = -1f;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==0x123) {
                if (visibleFlag == 1) {
                    controlView.setVisibility(View.INVISIBLE);
                    visibleFlag = 0;
                } else {
                    controlView.setVisibility(View.VISIBLE);
                    visibleFlag = 1;
                }
            }
        }
    };
    private Runnable r = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "setProgress");
            if (getIntent().getIntExtra(Global.MEDIA, 0) == Global.LOCAL_VIDEO) {
                seekPosition.setProgress((int) mediaPlayer.getCurrentPosition() / 1000);
                seekPosition.postDelayed(r, 1000);
            }
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!LibsChecker.checkVitamioLibs(this))
            return;
        setContentView(R.layout.activity_iptv_player);
        controlView = findViewById(R.id.control);
        seekPosition = (SeekBar) findViewById(R.id.seekPosition);
        seekPosition.setOnSeekBarChangeListener(new SeekCurrentPosition());
        surfaceView = (SurfaceView) findViewById(R.id.surface);
        surfaceView.setOnTouchListener(new SurfaceViewScrollListen());
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setFormat(PixelFormat.RGBA_8888);
        intent = getIntent();
        path=intent.getStringExtra(Global.EXTRA_LINK);

        mGestureDetector = new GestureDetector(this, new MyGestureListener());
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentPosition = mediaPlayer.getCurrentPosition();
        controlView.setVisibility(View.VISIBLE);
        startVideoPlayback();
    }

    private void playVideo(String path) {
        doCleanUp();
        try {
            Log.v(TAG, path);
            // Create a new media player and set the listeners
            mediaPlayer = new MediaPlayer(this);
            mediaPlayer.setDataSource(path);
            mediaPlayer.setDisplay(surfaceHolder);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            handler.post(r);
        } catch (IOException e) {
            Log.e(TAG, "error: " + e.getMessage(), e);
        }
    }

    public void stop(View v) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
    }

    @Override
    public void onCompletion(MediaPlayer arg0) {
        Log.d(TAG, "onCompletion called");
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.v(TAG, "onVideoSizeChanged called");
        if (width == 0 || height == 0) {
            Log.e(TAG, "invalid video width(" + width + ") or height(" + height + ")");
            return;
        }
        isVideoSizeKnown = true;
        videoWidth = width;
        videoHeight = height;
        if (isVideoReadyToBePlayed && isVideoSizeKnown) {
            startVideoPlayback();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaplayer) {
        Log.d(TAG, "onPrepared called");
        isVideoReadyToBePlayed = true;
        if (isVideoReadyToBePlayed && isVideoSizeKnown) {
            startVideoPlayback();
        }
    }

    public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
        Log.d(TAG, "surfaceChanged called");

    }

    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
        Log.d(TAG, "surfaceDestroyed called");
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated called");
        playVideo(intent.getStringExtra(Global.EXTRA_LINK));

    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaPlayer();
        doCleanUp();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        doCleanUp();
    }

    private void releaseMediaPlayer() {
        Log.d(TAG,"releaseMediaPlayer called");
        if (mediaPlayer != null) {
            if (intent.getIntExtra(Global.MEDIA, 0) == Global.LOCAL_VIDEO) {
                seekPosition.removeCallbacks(r);
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void doCleanUp() {
        videoWidth = 0;
        videoHeight = 0;
        isVideoReadyToBePlayed = false;
        isVideoSizeKnown = false;
    }

    private void startVideoPlayback() {
        seekPosition.setMax((int) mediaPlayer.getDuration() / 1000);
        Log.v(TAG, "startVideoPlayback");
        WindowManager windowManager = getWindowManager();
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        surfaceHolder.setFixedSize(metrics.widthPixels, videoHeight * metrics.widthPixels / videoWidth); //保持长宽比使其全屏
        if (intent.getIntExtra(Global.MEDIA, 0) == Global.LOCAL_VIDEO)
            if (currentPosition > 0) {
                Log.d(TAG, "continue");
                mediaPlayer.seekTo(currentPosition);
            }
        mediaPlayer.start();
    }


    private class SeekCurrentPosition implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mediaPlayer.seekTo(progress * 1000);
                mediaPlayer.start();
            }
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    class SurfaceViewScrollListen implements View.OnTouchListener{
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                    endGesture();
                    break;
            }
            return true;
        }

    }
    private void endGesture() {
        mVolume = -1;
        mBrightness = -1f;
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener{
            //单击
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                handler.sendEmptyMessage(0x123);
                return super.onSingleTapUp(e);
            }

            //滑动
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {
                float mOldX = e1.getX(), mOldY = e1.getY();
                int y = (int) e2.getRawY();
                WindowManager windowManager=getWindowManager();
                DisplayMetrics metrics=new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(metrics);
                int windowWidth = metrics.widthPixels;
                int windowHeight =metrics.heightPixels;


                if (mOldX > windowWidth * 4.0 / 5)// 右边滑动
                    onVolumeSlide((mOldY - y) / windowHeight);  //得到滑动的百分比
                if (mOldX < windowWidth / 5.0)// 左边滑动
                    onBrightnessSlide((mOldY - y) / windowHeight);

                return super.onScroll(e1, e2, distanceX, distanceY);
            }
        }
        private void onBrightnessSlide(float percent) {
            if (mBrightness < 0) {
                mBrightness = getWindow().getAttributes().screenBrightness;
                if (mBrightness <= 0.00f)
                    mBrightness = 0.50f;
                if (mBrightness < 0.01f)
                    mBrightness = 0.01f;
            }
            WindowManager.LayoutParams lpa = getWindow().getAttributes();
            lpa.screenBrightness = mBrightness + percent;
            if (lpa.screenBrightness > 1.0f)
                lpa.screenBrightness = 1.0f;
            else if (lpa.screenBrightness < 0.01f)
                lpa.screenBrightness = 0.01f;
            getWindow().setAttributes(lpa);    //设置改变之后的亮度，使用百分比来表示亮度
        }
        private void onVolumeSlide(float percent) {
            if (mVolume == -1) {
                mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);  //
                if (mVolume < 0)
                    mVolume = 0;
            }
            int index = (int) (percent * mMaxVolume) + mVolume;
            if (index > mMaxVolume)
                index = mMaxVolume;
            else if (index < 0)
                index = 0;
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
        }

}
