package com.indev.videoplayer.Activity;

import static com.indev.videoplayer.Adapter.VideoAdapter.videoFolder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.indev.videoplayer.R;

public class VideoPlayer extends AppCompatActivity  implements View.OnTouchListener,
        ScaleGestureDetector.OnScaleGestureListener {

    int position = -1;
    VideoView video_view;
    LinearLayout one, two, three, four, five, six;
    RelativeLayout zoomLayout;
    boolean isOpen = false;
    TextView videoView_title;
    ImageButton videoView_go_back, videoView_play_pause_btn;
    LinearLayout videoView_rotation;
    private int currentRotation = 0;
    private SeekBar seekBar;
    private Handler handler;
    TextView videoView_endtime;
    private boolean isPlaying = true;

    MediaController mediaController;


    //////  For Zoom Video

    ScaleGestureDetector scaleDetector;
    GestureDetectorCompat gestureDetector;
    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 5.0f;
    boolean intLeft, intRight;
    private Display display;
    private Point size;
    private Mode mode = Mode.NONE;

    private enum Mode {
        NONE,
        DRAG,
        ZOOM
    }

    int device_width;
    private int sWidth;
    private float scale = 1.0f;
    private float lastScaleFactor = 0f;
    // Where the finger first  touches the screen
    private float startX = 0f;
    private float startY = 0f;
    // How much to translate the canvas
    private float dx = 0f;
    private float dy = 0f;
    private float prevDx = 0f;
    private float prevDy = 0f;

    //// For Plus 10minute and minus 10 minute video
    ImageButton videoView_forward,videoView_rewind;
    LinearLayout videoView_lock_screen;
    boolean isLockScreen = false;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                hideDefaultControls();
                if (scale > MIN_ZOOM) {
                    mode = Mode.DRAG;
                    startX = event.getX() - prevDx;
                    startY = event.getY() - prevDy;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                hideDefaultControls();
                isOpen = false;
                if (mode == Mode.DRAG) {
                    dx = event.getX() - startX;
                    dy = event.getY() - startY;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mode = Mode.ZOOM;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = Mode.DRAG;
                break;
            case MotionEvent.ACTION_UP:
                mode = Mode.NONE;
                prevDx = dx;
                prevDy = dy;
                break;
        }
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        if ((mode == Mode.DRAG && scale >= MIN_ZOOM) || mode == Mode.ZOOM) {
            zoomLayout.requestDisallowInterceptTouchEvent(true);
            float maxDx = (child().getWidth() - (child().getWidth() / scale)) / 2 * scale;
            float maxDy = (child().getHeight() - (child().getHeight() / scale)) / 2 * scale;
            dx = Math.min(Math.max(dx, -maxDx), maxDx);
            dy = Math.min(Math.max(dy, -maxDy), maxDy);
            applyScaleAndTranslation();
        }
        return true;
    }

    private void applyScaleAndTranslation() {
        child().setScaleX(scale);
        child().setScaleY(scale);
        child().setTranslationX(dx);
        child().setTranslationY(dy);
    }

    private View child() {
        return zoomLayout(0);
    }

    private View zoomLayout(int i) {
        return video_view;
    }


    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = scaleDetector.getScaleFactor();
        if (lastScaleFactor == 0 || (Math.signum(scaleFactor) == Math.signum(lastScaleFactor))) {
            scale *= scaleFactor;
            scale = Math.max(MIN_ZOOM, Math.min(scale, MAX_ZOOM));
            lastScaleFactor = scaleFactor;
        } else {
            lastScaleFactor = 0;
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        getSupportActionBar().hide();
        AllIniClizeID();

        position = getIntent().getIntExtra("p", -1);

        String path = videoFolder.get(position).getPath();
        String video_name = videoFolder.get(position).getTitle();

        videoView_title.setText(video_name);

        if (path != null) {
            video_view.setVideoPath(path);
            videoView_endtime.setText(videoFolder.get(position).getDuration());
            video_view.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    video_view.start();
                    int duration = video_view.getDuration();
                    seekBar.setMax(duration);
                    hideDefaultControls();
                    isOpen = false;

                }
            });

        } else {
            Toast.makeText(this, "Path didn't exits ", Toast.LENGTH_SHORT).show();
        }

        video_view.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                onBackPressed();
            }
        });


        zoomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOpen) {
                    hideDefaultControls();
                    isOpen = false;
                } else {
                    ShowDefaultControls();
                    isOpen = true;
                }
            }
        });

        GoBackClick();
        RotateScreen();
        SetSeekBarValue();
        StartSeekBar();
        TapToPlayPauseVideo();

        zoomLayout = findViewById(R.id.zoom_layout);
        display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        sWidth = size.x;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        device_width = displayMetrics.widthPixels;
        zoomLayout.setOnTouchListener(this);
        scaleDetector = new ScaleGestureDetector(getApplicationContext(), this);
        gestureDetector = new GestureDetectorCompat(getApplicationContext(), new GestureDetector());

        //// For Plus And minus 10 minute duretion

        SetOnClickPlusMinusDuretionButton();

        LockScreen();
    }



    private void StartSeekBar() {
        // SeekBar listener to seek the video
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    video_view.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void SetSeekBarValue() {
        // Set up the handler to update the seek bar
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (video_view.isPlaying()) {
                    int currentPosition = video_view.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                }
                // Update seek bar every 1 second
                handler.sendEmptyMessageDelayed(0, 1000);
                return true;
            }
        });

        // Start updating the seek bar
        handler.sendEmptyMessage(0);
    }

    private void RotateScreen() {
        videoView_rotation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentRotation += 90;
                video_view.setRotation(currentRotation);
            }
        });
    }

    private void GoBackClick() {
        videoView_go_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }


    private void AllIniClizeID() {
        video_view = findViewById(R.id.video_view);
        one = findViewById(R.id.videoView_one_layout);
        two = findViewById(R.id.videoView_two_layout);
        three = findViewById(R.id.videoView_three_layout);
        four = findViewById(R.id.videoView_four_layout);
        five = findViewById(R.id.video_five_layout);
        zoomLayout = findViewById(R.id.zoom_layout);
        videoView_title = findViewById(R.id.videoView_title);
        videoView_go_back = findViewById(R.id.videoView_go_back);
        videoView_rotation = findViewById(R.id.videoView_rotation);

        seekBar = findViewById(R.id.videoView_seekbar);
        videoView_endtime = findViewById(R.id.videoView_endtime);
        videoView_play_pause_btn = findViewById(R.id.videoView_play_pause_btn);
        mediaController = new MediaController(this);
        videoView_rewind=findViewById(R.id.videoView_rewind);
        videoView_forward=findViewById(R.id.videoView_forward);
        videoView_lock_screen=findViewById(R.id.videoView_lock_screen);
    }

    private void hideDefaultControls() {
        one.setVisibility(View.GONE);
        two.setVisibility(View.GONE);
        three.setVisibility(View.GONE);
        four.setVisibility(View.GONE);

        final Window window = this.getWindow();

        if (window == null) {
            return;
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        final View decorview = window.getDecorView();

        if (decorview != null) {
            int uiOption = decorview.getSystemUiVisibility();

            if (Build.VERSION.SDK_INT >= 14) {
                uiOption |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
            }

            if (Build.VERSION.SDK_INT >= 16) {
                uiOption |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            if (Build.VERSION.SDK_INT >= 19) {
                uiOption |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            decorview.setSystemUiVisibility(uiOption);
        }
    }

    private void ShowDefaultControls() {
        one.setVisibility(View.VISIBLE);
        two.setVisibility(View.VISIBLE);
        three.setVisibility(View.VISIBLE);
        four.setVisibility(View.VISIBLE);

        final Window window = this.getWindow();

        if (window == null) {
            return;
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        final View decorview = window.getDecorView();

        if (decorview != null) {
            int uiOption = decorview.getSystemUiVisibility();

            if (Build.VERSION.SDK_INT >= 14) {
                uiOption &= View.SYSTEM_UI_FLAG_LOW_PROFILE;
            }

            if (Build.VERSION.SDK_INT >= 16) {
                uiOption &= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            if (Build.VERSION.SDK_INT >= 19) {
                uiOption &= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            decorview.setSystemUiVisibility(uiOption);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    private void TapToPlayPauseVideo() {
        videoView_play_pause_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePlayPause();
            }
        });

    }

    private void togglePlayPause() {
        if (isPlaying) {
            isPlaying = false;
            videoView_play_pause_btn.setImageResource(R.drawable.ic_play); // Set the pause icon
            video_view.pause();
        } else {
            isPlaying = true;
            videoView_play_pause_btn.setImageResource(R.drawable.netflix_pause_button); // Set the play icon
            video_view.start();
        }
    }

    private class GestureDetector extends android.view.GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (isOpen) {
                hideDefaultControls();
                isOpen = false;
            } else {
                ShowDefaultControls();
                isOpen = true;
            }
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            if (event.getX() < (sWidth / 2)) {
                intLeft = true;
                intRight = false;
                video_view.seekTo(video_view.getCurrentPosition() - 20000);
                Toast.makeText(VideoPlayer.this, "-20sec", Toast.LENGTH_SHORT).show();
            } else if (event.getX() > (sWidth / 2)) {
                intLeft = false;
                intRight = true;
                video_view.seekTo(video_view.getCurrentPosition() + 20000);
                Toast.makeText(VideoPlayer.this, "+20sec", Toast.LENGTH_SHORT).show();
            }
            return super.onDoubleTap(event);
        }
    }


    private void SetOnClickPlusMinusDuretionButton() {

        videoView_forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                video_view.seekTo(video_view.getCurrentPosition() + 10000);
                Toast.makeText(VideoPlayer.this, "+10sec", Toast.LENGTH_SHORT).show();
            }
        });

        videoView_rewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                video_view.seekTo(video_view.getCurrentPosition() - 10000);
                Toast.makeText(VideoPlayer.this, "-10sec", Toast.LENGTH_SHORT).show();
            }
        });

//        videoView_forward.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//
//                int value = 0;
//
//                for (int i = 0; i < 10; i++) {
//                    value += 10;
//                    video_view.seekTo(video_view.getCurrentPosition() + value);
//                }
//                return true;
//            }
//        });

    }

    private void LockScreen() {
        videoView_lock_screen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isLockScreen) {
                    hideDefaultControls();
                    isLockScreen = false;
                } else {
                    ShowDefaultControls();
                    isLockScreen = true;
                }
            }
        });
    }



}