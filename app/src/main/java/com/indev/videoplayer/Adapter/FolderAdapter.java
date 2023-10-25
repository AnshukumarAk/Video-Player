package com.indev.videoplayer.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.indev.videoplayer.Activity.VideoFolder;
import com.indev.videoplayer.Model.VideoModel;
import com.indev.videoplayer.R;

import java.util.ArrayList;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.MyViewholder> {

    private ArrayList<String> folderName;
    private ArrayList<VideoModel>videoModels;
    private Context context;

    public FolderAdapter(ArrayList<String> folderName, ArrayList<VideoModel> videoModels, Context context) {
        this.folderName = folderName;
        this.videoModels = videoModels;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.folder_view, parent, false);
        return new MyViewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewholder holder, @SuppressLint("RecyclerView") int position) {

        int index=folderName.get(position).lastIndexOf("/");
        String folderNames=folderName.get(position).substring(index+1);

        holder.tv_folder_name.setText(folderNames);
        holder.tv_folder_count.setText(String.valueOf(countVideos(folderName.get(position))));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent  intent=new Intent(context, VideoFolder.class);
                intent.putExtra("folderName",folderName.get(position));
                intent.putExtra("titleName",folderNames);
                context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return folderName.size();
    }

    public class MyViewholder extends RecyclerView.ViewHolder{
          TextView tv_folder_count,tv_folder_name;

        public MyViewholder(@NonNull View itemView) {
            super(itemView);
            tv_folder_count=itemView.findViewById(R.id.tv_folder_count);
            tv_folder_name=itemView.findViewById(R.id.tv_folder_name);
        }
    }

   int countVideos(String folders) {
        int count = 0;
        for (VideoModel model : videoModels) {
            if (model.getPath().substring(0, model.getPath().lastIndexOf("/")).endsWith(folders))
            {
                count++;
            }
        }
      return count;
  }
}
