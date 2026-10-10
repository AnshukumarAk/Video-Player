package com.indev.videoplayer.Activity;

import static com.indev.videoplayer.Adapter.VideoAdapter.videoFolder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.indev.videoplayer.R;

import java.util.ArrayList;

public class VideoPlayer extends AppCompatActivity  implements View.OnTouchListener,
        ScaleGestureDetector.OnScaleGestureListener {

    int position = -1;
    VideoView video_view;
    LinearLayout one, two, three, four, five, six;
    RelativeLayout zoomLayout;
    boolean isOpen = false;
    TextView videoView_title;
    ImageButton videoView_go_back, videoView_play_pause_btn, videoView_more;
    private SeekBar seekBar;
    private Handler handler;
    TextView videoView_endtime;
    private boolean isPlaying = true;

    MediaController mediaController;

    // Captured once the video is prepared so we can change speed / audio tracks.
    private MediaPlayer currentMediaPlayer;

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
    int device_height;
    private int sWidth;
    private float scale = 1.0f;
    private float lastScaleFactor = 0f;
    // Base scale comes from the screen-size mode (Fit/Stretch/Crop/100%); pinch zoom multiplies on top.
    private float baseScaleX = 1.0f;
    private float baseScaleY = 1.0f;
    private int videoWidth = 0;
    private int videoHeight = 0;
    private int resizeMode = 0; // 0 Fit, 1 Stretch, 2 Crop, 3 Original
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
    SeekBar videoView_brightness;
    String path="";

    LinearLayout lockControls, unlockControls, rotate, audioTrack,videoView_one_layout;
    TextView title, endTime, lockTextOne, lockTextTwo;

    ImageView img_lock,img_audio_and_subtitle,img_rotate_screen;
    TextView tv_rotate_screen,tv_audio_and_subtitle,tv_lock;

    Context context=this;
    int videosize=0;

    // ------- Swipe gestures (brightness / volume / seek) -------
    private AudioManager audioManager;
    private int maxVolume;
    private float currentBrightness = 0.5f;
    private TextView gestureInfo;
    private boolean swiping = false;
    private int gestureType = 0; // 1 = brightness, 2 = volume, 3 = seek
    private float gStartX, gStartY;
    private float startBrightness;
    private int startVolume;
    private int startSeekPos;
    private int pendingSeek = -1;
    private static final int SWIPE_THRESHOLD = 40;

    // ------- Controls auto-hide -------
    private final Handler controlsHandler = new Handler();
    private final Runnable hideControlsRunnable = this::hideDefaultControls;
    private static final long CONTROLS_TIMEOUT = 4000;

    // ------- Lock & speed -------
    private boolean controlsLocked = false;
    private float playbackSpeed = 1.0f;

    // ------- Resume playback -------
    private SharedPreferences resumePrefs;
    private static final String RESUME_PREF = "resume_positions";

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // When the screen is locked, ignore everything except a tap to reveal the unlock button.
        if (controlsLocked) {
            if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                toggleUnlockHint();
            }
            return true;
        }

        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                gStartX = event.getX();
                gStartY = event.getY();
                swiping = false;
                gestureType = 0;
                pendingSeek = -1;
                startBrightness = currentBrightness;
                startVolume = audioManager != null ? audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) : 0;
                startSeekPos = video_view.getCurrentPosition();
                if (scale > MIN_ZOOM) {
                    mode = Mode.DRAG;
                    startX = event.getX() - prevDx;
                    startY = event.getY() - prevDy;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // Pinch-zoom drag takes priority once the video is zoomed in.
                if (scale > MIN_ZOOM && mode == Mode.DRAG) {
                    dx = event.getX() - startX;
                    dy = event.getY() - startY;
                    break;
                }

                float ddx = event.getX() - gStartX;
                float ddy = event.getY() - gStartY;

                if (!swiping && (Math.abs(ddx) > SWIPE_THRESHOLD || Math.abs(ddy) > SWIPE_THRESHOLD)) {
                    swiping = true;
                    if (Math.abs(ddx) > Math.abs(ddy)) {
                        gestureType = 3; // horizontal -> seek
                    } else if (gStartX < device_width / 2f) {
                        gestureType = 1; // left vertical -> brightness
                    } else {
                        gestureType = 2; // right vertical -> volume
                    }
                }

                if (swiping) {
                    if (gestureType == 1) handleBrightnessSwipe(ddy);
                    else if (gestureType == 2) handleVolumeSwipe(ddy);
                    else if (gestureType == 3) handleSeekSwipe(ddx);
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
                if (swiping && gestureType == 3 && pendingSeek >= 0) {
                    video_view.seekTo(pendingSeek);
                }
                hideGestureInfo();
                swiping = false;
                gestureType = 0;
                break;
        }

        if (scale > MIN_ZOOM && ((mode == Mode.DRAG) || mode == Mode.ZOOM)) {
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
        child().setScaleX(baseScaleX * scale);
        child().setScaleY(baseScaleY * scale);
        child().setTranslationX(dx);
        child().setTranslationY(dy);
    }

    private View child() {
        return video_view;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float factor = detector.getScaleFactor();
        float oldScale = scale;
        float newScale = Math.max(MIN_ZOOM, Math.min(oldScale * factor, MAX_ZOOM));
        float r = newScale / oldScale;
        // Keep the point under the user's fingers fixed (zoom toward the pinch focus, MX-style).
        float fx = (detector.getFocusX() - child().getLeft()) - child().getWidth() / 2f;
        float fy = (detector.getFocusY() - child().getTop()) - child().getHeight() / 2f;
        dx = fx * (1 - r) + dx * r;
        dy = fy * (1 - r) + dy * r;
        scale = newScale;
        if (scale <= MIN_ZOOM) {
            dx = 0f;
            dy = 0f;
        }
        applyScaleAndTranslation();
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

        resumePrefs = getSharedPreferences(RESUME_PREF, MODE_PRIVATE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        AllIniClizeID();

        position = getIntent().getIntExtra("p", -1);

        path = videoFolder.get(position).getPath();
        String video_name = videoFolder.get(position).getTitle();

        videoView_title.setText(video_name);

        videosize = videoFolder.size();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        device_width = displayMetrics.widthPixels;
        device_height = displayMetrics.heightPixels;

        if (path != null) {
            video_view.setVideoPath(path);
            video_view.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    currentMediaPlayer = mp;
                    videoWidth = mp.getVideoWidth();
                    videoHeight = mp.getVideoHeight();
                    int duration = video_view.getDuration();
                    seekBar.setMax(duration);

                    // Resume from the last saved position if we have one.
                    int resumePos = resumePrefs.getInt(path, 0);
                    if (resumePos > 0 && resumePos < duration - 3000) {
                        video_view.seekTo(resumePos);
                        Toast.makeText(VideoPlayer.this, "Resumed", Toast.LENGTH_SHORT).show();
                    }

                    applyPlaybackSpeed(mp);
                    video_view.start();
                    isPlaying = true;
                    videoView_play_pause_btn.setImageResource(R.drawable.netflix_pause_button);

                    hideDefaultControls();
                    isOpen = false;

                    audioTrack.setOnClickListener(v -> checkMultiAudioTrack(mp));
                }
            });

        } else {
            Toast.makeText(this, "Path didn't exits ", Toast.LENGTH_SHORT).show();
        }

        video_view.setOnCompletionListener(mediaPlayer -> {
            resumePrefs.edit().remove(path).apply();
            playNextVideo();
        });


        zoomLayout.setOnClickListener(v -> toggleControls());

        GoBackClick();
        SetSeekBarValue();
        StartSeekBar();
        TapToPlayPauseVideo();

        display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        sWidth = size.x;
        zoomLayout.setOnTouchListener(this);
        scaleDetector = new ScaleGestureDetector(getApplicationContext(), this);
        gestureDetector = new GestureDetectorCompat(getApplicationContext(), new GestureDetector());

        SetOnClickPlusMinusDuretionButton();
        SetDisplayBrightness();
        SetupMoreMenu();

        lockControls.setOnClickListener(this::onClick);
        five.setOnClickListener(this::onClick);
        unlockControls.setOnClickListener(this::onClick);
        rotate.setOnClickListener(this::onClick);
    }

    @SuppressLint("NonConstantResourceId")
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.videoView_rotation:
                // Controls now stay white-on-scrim in both orientations, so just flip orientation.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT){
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    hideDefaultControls();
                    isOpen = false;
                }else if (orientation == Configuration.ORIENTATION_LANDSCAPE){
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                break;

            case R.id.videoView_lock_screen:
                // Lock the screen: hide controls and show the unlock pill.
                controlsLocked = true;
                hideDefaultControls();
                five.setVisibility(View.VISIBLE);
                animateFadeIn(five);
                showUnlockHint(true);
                break;

            case R.id.video_five_layout:
                toggleUnlockHint();
                break;

            case R.id.video_five_child_layout:
                // Unlock pressed.
                controlsLocked = false;
                five.setVisibility(View.GONE);
                ShowDefaultControls();
                break;
        }
    }

    private void toggleUnlockHint() {
        if (isOpen){
            showUnlockHint(false);
            isOpen = false;
        }else {
            showUnlockHint(true);
            isOpen = true;
        }
    }

    private void showUnlockHint(boolean show) {
        int vis = show ? View.VISIBLE : View.INVISIBLE;
        unlockControls.setVisibility(vis);
        lockTextOne.setVisibility(vis);
        lockTextTwo.setVisibility(vis);
    }

    private void StartSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    video_view.seekTo(progress);
                    int currentPosition = video_view.getCurrentPosition();
                    videoView_endtime.setText("" + convertIntoTime(video_view.getDuration() - currentPosition));
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
        // Single, well-behaved updater (replaces the old zero-delay busy loop).
        handler = new Handler(new Handler.Callback() {
            @SuppressLint("SetTextI18n")
            @Override
            public boolean handleMessage(Message msg) {
                if (video_view.getDuration() > 0) {
                    int currentPosition = video_view.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    videoView_endtime.setText("" + convertIntoTime(video_view.getDuration() - currentPosition));
                }
                handler.sendEmptyMessageDelayed(0, 500);
                return true;
            }
        });
        handler.sendEmptyMessage(0);
    }


    private void GoBackClick() {
        videoView_go_back.setOnClickListener(view -> onBackPressed());
    }


    @SuppressLint("CutPasteId")
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
        videoView_more = findViewById(R.id.videoView_more);
        gestureInfo = findViewById(R.id.gesture_info);

        seekBar = findViewById(R.id.videoView_seekbar);
        videoView_endtime = findViewById(R.id.videoView_endtime);
        videoView_play_pause_btn = findViewById(R.id.videoView_play_pause_btn);
        mediaController = new MediaController(this);
        videoView_rewind=findViewById(R.id.videoView_rewind);
        videoView_forward=findViewById(R.id.videoView_forward);
        videoView_brightness=findViewById(R.id.videoView_brightness);

        lockControls = findViewById(R.id.videoView_lock_screen);
        unlockControls = findViewById(R.id.video_five_child_layout);
        lockTextOne = findViewById(R.id.videoView_lock_text);
        lockTextTwo = findViewById(R.id.videoView_lock_text_two);
        rotate = findViewById(R.id.videoView_rotation);
        audioTrack = findViewById(R.id.videoView_track);

        ///// For Changes color after rotate screen
        tv_lock=findViewById(R.id.tv_lock);
        img_lock=findViewById(R.id.img_lock);
        tv_audio_and_subtitle=findViewById(R.id.tv_audio_and_subtitle);
        img_audio_and_subtitle=findViewById(R.id.img_audio_and_subtitle);
        tv_rotate_screen=findViewById(R.id.tv_rotate_screen);
        img_rotate_screen=findViewById(R.id.img_rotate_screen);
        videoView_one_layout=findViewById(R.id.videoView_one_layout);
    }

    private void toggleControls() {
        if (controlsLocked) return;
        if (isOpen) {
            hideDefaultControls();
            isOpen = false;
        } else {
            ShowDefaultControls();
            isOpen = true;
        }
    }

    private void hideDefaultControls() {
        controlsHandler.removeCallbacks(hideControlsRunnable);
        animateFadeOut(one);
        animateFadeOut(two);
        animateFadeOut(three);
        animateFadeOut(four);
        isOpen = false;

        final Window window = this.getWindow();
        if (window == null) {
            return;
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        final View decorview = window.getDecorView();
        if (decorview != null) {
            int uiOption = decorview.getSystemUiVisibility();
            uiOption |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
            uiOption |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiOption |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorview.setSystemUiVisibility(uiOption);
        }
    }

    private void ShowDefaultControls() {
        animateFadeIn(one);
        animateFadeIn(two);
        animateFadeIn(three);
        animateFadeIn(four);
        isOpen = true;

        // Auto-hide again after a few seconds of inactivity.
        controlsHandler.removeCallbacks(hideControlsRunnable);
        controlsHandler.postDelayed(hideControlsRunnable, CONTROLS_TIMEOUT);

        final Window window = this.getWindow();
        if (window == null) {
            return;
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        final View decorview = window.getDecorView();
        if (decorview != null) {
            int uiOption = decorview.getSystemUiVisibility();
            uiOption &= ~View.SYSTEM_UI_FLAG_LOW_PROFILE;
            uiOption &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiOption &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorview.setSystemUiVisibility(uiOption);
        }
    }

    // ------- Smooth fade helpers -------
    private void animateFadeIn(final View v) {
        if (v.getVisibility() == View.VISIBLE && v.getAlpha() == 1f) return;
        v.clearAnimation();
        v.setVisibility(View.VISIBLE);
        v.animate().alpha(1f).setDuration(220).start();
    }

    private void animateFadeOut(final View v) {
        if (v.getVisibility() != View.VISIBLE) return;
        v.animate().alpha(0f).setDuration(220).withEndAction(() -> v.setVisibility(View.GONE)).start();
    }

    @Override
    public void onBackPressed() {
        saveResumePosition();
        super.onBackPressed();
    }


    private void TapToPlayPauseVideo() {
        videoView_play_pause_btn.setOnClickListener(view -> togglePlayPause());
    }

    private void togglePlayPause() {
        if (isPlaying) {
            isPlaying = false;
            videoView_play_pause_btn.setImageResource(R.drawable.ic_play); // show "play" so user can resume
            video_view.pause();
        } else {
            isPlaying = true;
            videoView_play_pause_btn.setImageResource(R.drawable.netflix_pause_button);
            video_view.start();
        }
    }

    private class GestureDetector extends android.view.GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            toggleControls();
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            if (controlsLocked) return super.onDoubleTap(event);
            int duration = video_view.getDuration();
            if (event.getX() < (device_width / 2f)) {
                intLeft = true;
                intRight = false;
                int target = Math.max(0, video_view.getCurrentPosition() - 10000);
                video_view.seekTo(target);
                showGestureInfo("⏪  -10s");
                gestureInfo.postDelayed(VideoPlayer.this::hideGestureInfo, 600);
            } else {
                intLeft = false;
                intRight = true;
                int target = Math.min(duration, video_view.getCurrentPosition() + 10000);
                video_view.seekTo(target);
                showGestureInfo("+10s  ⏩");
                gestureInfo.postDelayed(VideoPlayer.this::hideGestureInfo, 600);
            }
            return super.onDoubleTap(event);
        }
    }


    private void SetOnClickPlusMinusDuretionButton() {
        videoView_forward.setOnClickListener(view -> {
            int target = Math.min(video_view.getDuration(), video_view.getCurrentPosition() + 10000);
            video_view.seekTo(target);
            showGestureInfo("+10s  ⏩");
            gestureInfo.postDelayed(this::hideGestureInfo, 600);
        });

        videoView_rewind.setOnClickListener(view -> {
            int target = Math.max(0, video_view.getCurrentPosition() - 10000);
            video_view.seekTo(target);
            showGestureInfo("⏪  -10s");
            gestureInfo.postDelayed(this::hideGestureInfo, 600);
        });
    }

    // ------- Swipe gesture handlers -------
    @SuppressLint("SetTextI18n")
    private void handleBrightnessSwipe(float ddy) {
        float delta = -ddy / device_height;
        float b = startBrightness + delta;
        b = Math.max(0.02f, Math.min(1f, b));
        currentBrightness = b;
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = b;
        getWindow().setAttributes(lp);
        showGestureInfo("☀  " + Math.round(b * 100) + "%");
    }

    @SuppressLint("SetTextI18n")
    private void handleVolumeSwipe(float ddy) {
        if (audioManager == null) return;
        float delta = -ddy / device_height;
        int v = startVolume + Math.round(delta * maxVolume);
        v = Math.max(0, Math.min(maxVolume, v));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0);
        showGestureInfo("🔊  " + Math.round((v * 100f) / maxVolume) + "%");
    }

    @SuppressLint("SetTextI18n")
    private void handleSeekSwipe(float ddx) {
        int duration = video_view.getDuration();
        if (duration <= 0) return;
        int delta = (int) ((ddx / device_width) * duration);
        int target = Math.max(0, Math.min(duration, startSeekPos + delta));
        pendingSeek = target;
        String sign = delta >= 0 ? "+" : "-";
        showGestureInfo(convertIntoTime(target) + " / " + convertIntoTime(duration)
                + "\n" + sign + convertIntoTime(Math.abs(delta)));
    }

    private void showGestureInfo(String text) {
        gestureInfo.setText(text);
        if (gestureInfo.getVisibility() != View.VISIBLE) {
            gestureInfo.setAlpha(0f);
            gestureInfo.setVisibility(View.VISIBLE);
            gestureInfo.animate().alpha(1f).setDuration(120).start();
        }
    }

    private void hideGestureInfo() {
        if (gestureInfo.getVisibility() == View.VISIBLE) {
            gestureInfo.animate().alpha(0f).setDuration(150)
                    .withEndAction(() -> gestureInfo.setVisibility(View.GONE)).start();
        }
    }

    private void playNextVideo() {
        if (position < videosize - 1) {
            position++;
            playVideo(position);
        } else {
            onBackPressed();
        }
    }

    private void playVideo(int index) {
        if (index < 0 || index >= videoFolder.size()) return;
        path = videoFolder.get(index).getPath();
        videoView_title.setText(videoFolder.get(index).getTitle());
        // setVideoPath re-triggers onPrepared (which starts playback, applies speed & resume).
        video_view.setVideoPath(path);
        isPlaying = true;
        videoView_play_pause_btn.setImageResource(R.drawable.netflix_pause_button);
    }

    private void SetDisplayBrightness() {
        // Initialise the slider to the current brightness.
        videoView_brightness.setProgress(Math.round(currentBrightness * videoView_brightness.getMax()));
        videoView_brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) updateBrightness(progress, seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void updateBrightness(int brightness, int max) {
        float value = Math.max(0.02f, (float) brightness / max);
        currentBrightness = value;
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = value;
        getWindow().setAttributes(layoutParams);
    }

    // ------- Screen size + playback speed (via the top-right "more" button) -------
    private void SetupMoreMenu() {
        videoView_more.setOnClickListener(this::showMoreMenu);
    }

    private void showMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        final float[] speeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

        SubMenu sizeMenu = popup.getMenu().addSubMenu("Screen size");
        sizeMenu.add(0, 200, 0, "Fit");
        sizeMenu.add(0, 201, 1, "Stretch");
        sizeMenu.add(0, 202, 2, "Crop");
        sizeMenu.add(0, 203, 3, "100% (Original)");

        SubMenu speedMenu = popup.getMenu().addSubMenu("Playback speed");
        for (int i = 0; i < speeds.length; i++) {
            speedMenu.add(0, 100 + i, i, speeds[i] + "x");
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id >= 200 && id <= 203) {
                applyResizeMode(id - 200);
                String[] names = {"Fit", "Stretch", "Crop", "100%"};
                Toast.makeText(VideoPlayer.this, names[id - 200], Toast.LENGTH_SHORT).show();
                return true;
            }
            if (id >= 100 && id <= 105) {
                playbackSpeed = speeds[id - 100];
                if (currentMediaPlayer != null) {
                    applyPlaybackSpeed(currentMediaPlayer);
                }
                Toast.makeText(VideoPlayer.this, "Speed " + playbackSpeed + "x", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    /** Apply an MX-style screen-size mode by scaling the video surface. Resets any pinch zoom. */
    private void applyResizeMode(int mode) {
        resizeMode = mode;
        scale = 1f;
        dx = 0f;
        dy = 0f;
        prevDx = 0f;
        prevDy = 0f;
        lastScaleFactor = 0f;

        int vw = video_view.getWidth();
        int vh = video_view.getHeight();
        if (vw == 0 || vh == 0 || videoWidth == 0 || videoHeight == 0) {
            baseScaleX = baseScaleY = 1f;
            applyScaleAndTranslation();
            return;
        }

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int sw = dm.widthPixels, sh = dm.heightPixels;

        switch (mode) {
            case 1: // Stretch to fill, ignoring aspect ratio
                baseScaleX = (float) sw / vw;
                baseScaleY = (float) sh / vh;
                break;
            case 2: // Crop to fill, preserving aspect ratio
                float c = Math.max((float) sw / vw, (float) sh / vh);
                baseScaleX = baseScaleY = c;
                break;
            case 3: // 100% original pixels
                float o = (float) videoWidth / vw;
                baseScaleX = baseScaleY = o;
                break;
            case 0: // Fit (default letterbox)
            default:
                baseScaleX = baseScaleY = 1f;
                break;
        }
        baseScaleX = Math.max(0.25f, Math.min(baseScaleX, 5f));
        baseScaleY = Math.max(0.25f, Math.min(baseScaleY, 5f));
        applyScaleAndTranslation();
    }

    private void resetZoom() {
        scale = 1f;
        dx = 0f;
        dy = 0f;
        prevDx = 0f;
        prevDy = 0f;
        lastScaleFactor = 0f;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Re-fit the video to the new orientation. Without this, a leftover zoom/pan
        // transform (computed for the old size) leaves the frame stuck/offset after rotating.
        if (video_view != null) {
            video_view.post(() -> {
                resetZoom();
                applyResizeMode(resizeMode);
                video_view.requestLayout();
            });
        }
    }

    private void applyPlaybackSpeed(MediaPlayer mp) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                boolean wasPlaying = video_view.isPlaying();
                PlaybackParams params = mp.getPlaybackParams();
                params.setSpeed(playbackSpeed);
                mp.setPlaybackParams(params);
                // setPlaybackParams resumes playback; honour the paused state.
                if (!wasPlaying && !isPlaying) {
                    mp.pause();
                }
            } catch (Exception ignored) {
                // Some codecs/devices don't support speed changes.
            }
        }
    }

    private String convertIntoTime(int ms){
        String time;
        int x, seconds, minutes, hours;
        x = ms / 1000;
        seconds = x % 60;
        x /= 60;
        minutes = x % 60;
        x /= 60;
        hours = x % 24;
        if (hours != 0)
            time = String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
        else time = String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
        return time;
    }

    private void checkMultiAudioTrack(MediaPlayer mediaPlayer) {
        MediaPlayer.TrackInfo[] trackInfos = mediaPlayer.getTrackInfo();

        final ArrayList<Integer> audioTracksIndex = new ArrayList<>();
        for (int i = 0; i < trackInfos.length; i++) {
            if (trackInfos[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                audioTracksIndex.add(i);
            }
        }

        if (audioTracksIndex.isEmpty()) {
            Toast.makeText(this, "No selectable audio tracks", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(VideoPlayer.this);
        builder.setTitle("Select Audio Track");

        String[] values = new String[audioTracksIndex.size()];
        for (int i = 0; i < audioTracksIndex.size(); i++) {
            values[i] = "Track " + (i + 1);
        }
        builder.setSingleChoiceItems(values, 0, (dialog, which) -> {
            try {
                mediaPlayer.selectTrack(audioTracksIndex.get(which));
                Toast.makeText(VideoPlayer.this, "Track " + (which + 1) + " selected", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(VideoPlayer.this, "This track can't be switched", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // ------- Picture-in-Picture -------
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        enterPipIfPossible();
    }

    private void enterPipIfPossible() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
                && video_view.isPlaying()) {
            int w = video_view.getWidth();
            int h = video_view.getHeight();
            if (w <= 0 || h <= 0) { w = 16; h = 9; }
            Rational ratio = new Rational(w, h);
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(ratio)
                    .build();
            try {
                enterPictureInPictureMode(params);
            } catch (IllegalStateException ignored) {
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            hideDefaultControls();
        }
    }

    private void saveResumePosition() {
        if (path != null && video_view != null && video_view.getDuration() > 0) {
            int pos = video_view.getCurrentPosition();
            if (pos > 3000) {
                resumePrefs.edit().putInt(path, pos).apply();
            } else {
                resumePrefs.edit().remove(path).apply();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveResumePosition();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        controlsHandler.removeCallbacks(hideControlsRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (handler != null) handler.sendEmptyMessage(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        controlsHandler.removeCallbacks(hideControlsRunnable);
    }
}
