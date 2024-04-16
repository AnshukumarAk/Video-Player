package com.indev.videoplayer.Adapter;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.indev.videoplayer.Activity.VideoPlayer;
import com.indev.videoplayer.Model.VideoModel;
import com.indev.videoplayer.R;

import java.io.File;
import java.util.ArrayList;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.Myholder> {

    public static ArrayList<VideoModel>videoFolder=new ArrayList<>();
    private Context context;
   BottomSheetDialog bottomSheetDialog;

    public VideoAdapter(ArrayList<VideoModel> videoFolder, Context context) {
        this.videoFolder = videoFolder;
        this.context = context;
        this.bottomSheetDialog = bottomSheetDialog;
    }

    public void updateSearchList(ArrayList<VideoModel> searchList) {
        videoFolder=new ArrayList<>();
        videoFolder.addAll(searchList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoAdapter.Myholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.files_view, parent, false);
        return new VideoAdapter.Myholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoAdapter.Myholder holder, @SuppressLint("RecyclerView") int position) {


        Glide.with(context).load(videoFolder.get(position).getPath()).into(holder.thumbnail);
        holder.title.setText(videoFolder.get(position).getTitle());
        holder.duretion.setText(videoFolder.get(position).getDuration());
        holder.size.setText(videoFolder.get(position).getSize());
        holder.resolution.setText(videoFolder.get(position).getResolution());

        holder.video_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ShowPopUp(position);
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent=new Intent(context, VideoPlayer.class);
                intent.putExtra("p",position);
                context.startActivity(intent);
            }
        });


    }


    @Override
    public int getItemCount() {
        return videoFolder.size();
    }


    public class Myholder extends RecyclerView.ViewHolder{

        ImageView thumbnail,video_menu;
        TextView title,size,duretion,resolution;

        public Myholder(@NonNull View itemView) {
            super(itemView);

            thumbnail=itemView.findViewById(R.id.video_thumbnail);
            video_menu=itemView.findViewById(R.id.video_menu);
            title=itemView.findViewById(R.id.video_title);
            duretion=itemView.findViewById(R.id.vide_duretion);
            resolution=itemView.findViewById(R.id.video_quality);
            size=itemView.findViewById(R.id.video_size);
        }
    }

    private void ShowPopUp(int position) {
        bottomSheetDialog=new BottomSheetDialog(context);
        bottomSheetDialog.setContentView(R.layout.show_diloge);

        LinearLayout share = bottomSheetDialog.findViewById(R.id.share);
        LinearLayout delete = bottomSheetDialog.findViewById(R.id.delete);
        LinearLayout rename = bottomSheetDialog.findViewById(R.id.rename);
        LinearLayout properties = bottomSheetDialog.findViewById(R.id.properties);

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                Sharefile(position);
            }
        });


        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                DeleteFile(position);
            }
        });

        rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
               RenameFileName(position);
            }
        });

        properties.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
               ShowVideoProperties(position);
            }
        });

        bottomSheetDialog.show();
    }


    private void Sharefile(int position) {
        Uri uri = Uri.parse(videoFolder.get(position).getPath());

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            context.startActivity(Intent.createChooser(intent, "Share"));
        } catch (ActivityNotFoundException e) {
            // Handle the case where there are no apps to handle the share action
            Toast.makeText(context, "No apps can perform this action", Toast.LENGTH_SHORT).show();
        }
    }

    private void DeleteFile(int position) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        bottomSheetDialog.setContentView(R.layout.delete_popup);

        TextView BtnYes = bottomSheetDialog.findViewById(R.id.BtnYes);
        TextView BtnNo = bottomSheetDialog.findViewById(R.id.BtnNo);

        // Null check for BtnYes and BtnNo TextViews
        if (BtnYes != null && BtnNo != null) {
            BtnYes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Get the video file path
                    String videoPath = videoFolder.get(position).getPath();

                    // Check if the video file path is valid
                    if (videoPath != null && !videoPath.isEmpty()) {
                        // Create a File object for the video
                        File videoFile = new File(videoPath);

                        // Check if the file exists
                        if (videoFile.exists()) {
                            try {
                                // Attempt to delete the file
                                boolean deleted = videoFile.delete();

                                // Check if the file was successfully deleted
                                if (deleted) {
                                    // Remove the video from the list
                                    videoFolder.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, videoFolder.size());
                                    Toast.makeText(context, "File Deleted Successfully", Toast.LENGTH_LONG).show();
                                } else {
                                    // Show an error message if the file deletion failed
                                    Toast.makeText(context, "File Deletion Failed", Toast.LENGTH_LONG).show();
                                }
                            } catch (SecurityException e) {
                                // Handle security exceptions
                                e.printStackTrace();
                                Toast.makeText(context, "Security Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                // Handle other exceptions
                                e.printStackTrace();
                                Toast.makeText(context, "Error Deleting File: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // Show an error message if the video file does not exist
                            Toast.makeText(context, "File Not Found", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Show an error message if the video path is invalid
                        Toast.makeText(context, "Invalid File Path", Toast.LENGTH_LONG).show();
                    }

                    // Dismiss the bottom sheet dialog
                    bottomSheetDialog.dismiss();
                }
            });

            BtnNo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Dismiss the bottom sheet dialog
                    bottomSheetDialog.dismiss();
                }
            });

            bottomSheetDialog.show();
        } else {
            // Show an error message if the buttons are not found
            Snackbar.make(bottomSheetDialog.getWindow().getDecorView(), "Null Buttons", Snackbar.LENGTH_LONG).show();
        }
    }



    private void RenameFileName(int position) {
        final Dialog dialog=new Dialog(context);
        dialog.setContentView(R.layout.rename_file);
        final EditText editText=dialog.findViewById(R.id.EditTitle);

        Button cancel=dialog.findViewById(R.id.Cancel);
        Button rename=dialog.findViewById(R.id.Rename);

        final File renameFile=new File(videoFolder.get(position).getPath());
        String nameText=renameFile.getName();

        nameText=nameText.substring(0,nameText.lastIndexOf("."));
        editText.setText(nameText);
        editText.clearFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);


        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String onlyPath=renameFile.getParentFile().getAbsolutePath();
                String ext=renameFile.getAbsolutePath();

                ext=ext.substring(ext.lastIndexOf("."));

                String newPath=onlyPath + "/" + editText.getText() + ext;
                File newFile=new File(newPath);

                boolean rename=renameFile.renameTo(newFile);

                if (rename){
                    context.getApplicationContext().getContentResolver()
                            .delete(MediaStore.Files.getContentUri("external"),
                            MediaStore.MediaColumns.DATA + "=?",
                    new String[]{renameFile.getAbsolutePath()});

                    Intent intent=new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(newFile));
                    context.getApplicationContext().sendBroadcast(intent);
                    Snackbar.make(v,"Rename Successfully",Snackbar.LENGTH_LONG).show();


                }else {
                    Snackbar.make(v,"Rename Failed",Snackbar.LENGTH_LONG).show();
                }
            }
        });

        dialog.show();
    }

    private void ShowVideoProperties(int position){
       BottomSheetDialog bottomSheetDialog=new BottomSheetDialog(context);
        bottomSheetDialog.setContentView(R.layout.show_video_properties);


        String name=videoFolder.get(position).getTitle();
        String path=videoFolder.get(position).getPath();
        String size=videoFolder.get(position).getSize();
        String duration=videoFolder.get(position).getDuration();
        String resolution=videoFolder.get(position).getResolution();


        TextView title_name = bottomSheetDialog.findViewById(R.id.name);
        TextView video_path = bottomSheetDialog.findViewById(R.id.path);
        TextView video_size = bottomSheetDialog.findViewById(R.id.size);
        TextView video_duration = bottomSheetDialog.findViewById(R.id.duration);
        TextView video_resolution = bottomSheetDialog.findViewById(R.id.resolution);


        title_name.setText(name);
        video_path.setText(path);
        video_size.setText(size);
        video_duration.setText(duration);
        video_resolution.setText(resolution+"p");

       bottomSheetDialog.show();
    }


}
