package com.indev.videoplayer.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.indev.videoplayer.Adapter.VideoAdapter;
import com.indev.videoplayer.Model.VideoModel;
import com.indev.videoplayer.R;

import java.util.ArrayList;
import java.util.Locale;

public class VideoFolder extends AppCompatActivity implements SearchView.OnQueryTextListener{

    private RecyclerView recyclerView;
    private String name;
    private ArrayList<VideoModel>videoModelArrayList=new ArrayList<>();
    private VideoAdapter videoAdapter;
    Context context=this;
    String title_name="";
    Toolbar toolbar;
    private Paint mClearPaint;
    LinearLayout parentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_folder);

        name=getIntent().getStringExtra("folderName");
        title_name=getIntent().getStringExtra("titleName");
        AllIniclizeID();

//          getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        getSupportActionBar().hide();
////        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setHomeAsUpIndicator(getResources().getDrawable(R.drawable.ic_baseline_back_24));
//        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_700));



        if (!title_name.equals("")){
          setTitle(title_name+" videos");
        }

        LoadVideos();

    }

    private void AllIniclizeID() {
        recyclerView=findViewById(R.id.recycleview_video);
//        toolbar=findViewById(R.id.toolbar);
    }

    private void LoadVideos() {
        videoModelArrayList=getAllVideoFromFolder(this,name);
        if (name!=null && videoModelArrayList.size()>0){
            videoAdapter=new VideoAdapter(videoModelArrayList,this);


            //// if your recycle lagging then just add this line
            recyclerView.setHasFixedSize(true);
            recyclerView.setItemViewCacheSize(20);
            recyclerView.setDrawingCacheEnabled(true);
            recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
            recyclerView.setNestedScrollingEnabled(false);

            /////

            recyclerView.setAdapter(videoAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this,RecyclerView.VERTICAL,false));

        }else {
              Toast.makeText(this, "can't find any videos", Toast.LENGTH_LONG).show();
        }

    }

    private ArrayList<VideoModel> getAllVideoFromFolder(VideoFolder videoFolder, String name) {
            ArrayList<VideoModel>list=new ArrayList<>();

            Uri uri= MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            String orderBy=MediaStore.Video.Media.DATE_ADDED+ " DESC ";

            String [] projection={
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.TITLE,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.HEIGHT,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Video.Media.RESOLUTION,

            };

             String selection=MediaStore.Video.Media.DATA +" like?";
             String[] selectionArgs=new String[]{"%" + name + "%"};

           Cursor cursor=context.getContentResolver().query(uri,projection,selection,selectionArgs,orderBy);

          if (cursor!=null) {
              while (cursor.moveToNext()) {
                  String id = cursor.getString(0);
                  String path = cursor.getString(1);
                  String title = cursor.getString(2);
                  int size = cursor.getInt(3);
                  String resolution = cursor.getString(4);
                  int duration = cursor.getInt(5);
                  String disName = cursor.getString(6);
                  String bucket_display_name = cursor.getString(7);
                  String width_height = cursor.getString(8);


                  ///// this Method convert 1204 in 1mb
                  String human_can_read = null;
                  if (size < 1024) {
                      human_can_read = String.format(context.getString(R.string.size_in_b), (double) size);
                  } else if (size < Math.pow(1024, 2)) {
                      human_can_read = String.format(context.getString(R.string.size_in_gb), (double) size / 1024);
                  } else if (size < Math.pow(1024, 3)) {
                      human_can_read = String.format(context.getString(R.string.size_in_mb), (double) size / Math.pow(1024, 2));

                  } else {
                      human_can_read = String.format(context.getString(R.string.size_in_gb), (double) size / Math.pow(1024, 3));

                  }


                  ///// this method convert any random video duretion like 672365256231 into 1:21:12
                  String duration_formatted;
                  int sec = (duration / 1000) % 60;
                  int min = (duration / (1000 * 60)) % 60;
                  int hrs = (duration / (1000 * 60 * 60));

                  if (hrs == 0) {
                      duration_formatted = String.valueOf(min).concat(":".concat(String.format(Locale.UK, "%02d", sec)));
                  } else {
                      duration_formatted = String.valueOf(hrs).concat(":".concat(String.format(Locale.UK, "%02d", min).concat(":".concat(String.format(Locale.UK, "%02d", sec)))));
                  }


                  VideoModel files = new VideoModel(id, path, title, human_can_read, resolution, duration_formatted, disName, width_height);

                  if (name.endsWith(bucket_display_name))
                      list.add(files);


              }
              cursor.close();

          }
        return list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_toolbar, menu);
        MenuItem menuItem=menu.findItem(R.id.search);
        SearchView searchView=(SearchView) menuItem.getActionView();
//        ImageView ivClose=searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
//        ivClose.setColorFilter(ContextCompat.getColor(getApplicationContext(),R.color.white),
//                android.graphics.PorterDuff.Mode.SRC_IN);
        searchView.setQueryHint("Search file name");

        searchView.setOnQueryTextListener(this);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String input=newText.toLowerCase();
        ArrayList<VideoModel> searchList=new ArrayList<>();
        for (VideoModel model : videoModelArrayList){
            if (model.getTitle().toLowerCase().contains(input)){
                searchList.add(model);
            }
        }
        videoAdapter.updateSearchList(searchList);
        return true;
    }
}