package com.indev.videoplayer.Activity;

import static com.indev.videoplayer.Adapter.VideoAdapter.videoFolder;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.indev.videoplayer.R;

public class VideoPlayer extends AppCompatActivity {

    int position=-1;
    VideoView video_view;
    LinearLayout one,two,three,four,five,six;
    RelativeLayout zoomLayout;
    boolean isOpen=false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        AllIniClizeID();

        position=getIntent().getIntExtra("p",-1);

        String path=videoFolder.get(position).getPath();

        if (path !=null){
            video_view.setVideoPath(path);

            video_view.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    video_view.start();

                }
            });

        }else {
            Toast.makeText(this, "Path didn't exits ", Toast.LENGTH_SHORT).show();
        }


        zoomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOpen){
                    hideDefaultControls();
                    isOpen=false;
                }else {
                    ShowDefaultControls();
                    isOpen=true;
                }
            }
        });


    }

    private void AllIniClizeID() {
        video_view=findViewById(R.id.video_view);
        one=findViewById(R.id.videoView_one_layout);
        two=findViewById(R.id.videoView_two_layout);
        three=findViewById(R.id.videoView_three_layout);
        four=findViewById(R.id.videoView_four_layout);
        five=findViewById(R.id.video_five_layout);
        zoomLayout=findViewById(R.id.zoom_layout);
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
        final View decorview=window.getDecorView();

        if (decorview !=null){
            int uiOption=decorview.getSystemUiVisibility();

            if (Build.VERSION.SDK_INT>=14){
                uiOption |=View.SYSTEM_UI_FLAG_LOW_PROFILE;
            }

            if (Build.VERSION.SDK_INT >=16){
                uiOption |=View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            if (Build.VERSION.SDK_INT >=19){
                uiOption |=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
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
        final View decorview=window.getDecorView();

        if (decorview !=null){
            int uiOption=decorview.getSystemUiVisibility();

            if (Build.VERSION.SDK_INT>=14){
                uiOption &=View.SYSTEM_UI_FLAG_LOW_PROFILE;
            }

            if (Build.VERSION.SDK_INT >=16){
                uiOption &=View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            if (Build.VERSION.SDK_INT >=19){
                uiOption &=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            decorview.setSystemUiVisibility(uiOption);
        }
    }

}