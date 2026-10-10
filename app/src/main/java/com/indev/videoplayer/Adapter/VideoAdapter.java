package com.indev.videoplayer.Adapter;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
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
    private int lastAnimatedPosition = -1;
    public static final int DELETE_REQUEST_CODE = 1234;
    private int pendingDeletePosition = -1;
    private Uri pendingDeleteUri;
    private String pendingDeletePath;

    public static final int RENAME_REQUEST_CODE = 1235;
    private int pendingRenamePosition = -1;
    private Uri pendingRenameUri;
    private ContentValues pendingRenameValues;

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

        setAnimation(holder.itemView, position);
    }

    // Subtle fall-down + fade animation for newly bound rows only.
    private void setAnimation(View view, int position) {
        if (position > lastAnimatedPosition) {
            view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.item_anim_fall_down));
            lastAnimatedPosition = position;
        }
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
        // Use a MediaStore content URI so sharing works on Android 7+ (no FileUriExposedException).
        Uri uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                Long.parseLong(videoFolder.get(position).getId()));

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

    /** Public entry point so both the menu and the swipe-to-delete gesture can trigger a delete. */
    public void showDeleteDialog(int position) {
        DeleteFile(position);
    }

    private void DeleteFile(int position) {
        if (position < 0 || position >= videoFolder.size()) return;
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        bottomSheetDialog.setContentView(R.layout.delete_popup);

        TextView BtnYes = bottomSheetDialog.findViewById(R.id.BtnYes);
        TextView BtnNo = bottomSheetDialog.findViewById(R.id.BtnNo);

        if (BtnYes != null && BtnNo != null) {
            BtnYes.setOnClickListener(view -> {
                performDelete(position);
                bottomSheetDialog.dismiss();
            });
            BtnNo.setOnClickListener(view -> bottomSheetDialog.dismiss());
            bottomSheetDialog.show();
        } else {
            Snackbar.make(bottomSheetDialog.getWindow().getDecorView(), "Null Buttons", Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Scoped-storage-safe delete.
     *  - Android 11+ : the system shows its own confirmation dialog and deletes the file
     *                  before returning RESULT_OK (we just drop the row in onDeleteConfirmed).
     *  - Android 10  : a RecoverableSecurityException prompts for permission; on RESULT_OK
     *                  we RE-RUN the delete (the grant alone does not delete the file).
     *  - Older       : delete directly via the ContentResolver, with a raw File fallback.
     */
    private void performDelete(int position) {
        String videoPath = videoFolder.get(position).getPath();
        if (videoPath == null || videoPath.isEmpty()) {
            Toast.makeText(context, "Invalid File Path", Toast.LENGTH_LONG).show();
            return;
        }

        Uri uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                Long.parseLong(videoFolder.get(position).getId()));
        ContentResolver resolver = context.getContentResolver();

        // Stash what we are deleting so the result callback can finish the job.
        pendingDeletePosition = position;
        pendingDeleteUri = uri;
        pendingDeletePath = videoPath;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ArrayList<Uri> uris = new ArrayList<>();
                uris.add(uri);
                PendingIntent pi = MediaStore.createDeleteRequest(resolver, uris);
                launchDeleteRequest(pi.getIntentSender());
            } else {
                int rows = resolver.delete(uri, null, null);
                if (rows > 0 || deleteRawFile(videoPath)) {
                    removeAt(position);
                    Toast.makeText(context, "File Deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "File Deletion Failed", Toast.LENGTH_LONG).show();
                }
                clearPending();
            }
        } catch (SecurityException se) {
            // Android 10: ask the user for permission, then retry on result.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                handleRecoverableSecurity(se, position);
            } else {
                Toast.makeText(context, "Delete failed: " + se.getMessage(), Toast.LENGTH_LONG).show();
                clearPending();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error deleting file", Toast.LENGTH_LONG).show();
            clearPending();
        }
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.Q)
    private void handleRecoverableSecurity(SecurityException se, int position) {
        if (se instanceof RecoverableSecurityException) {
            IntentSender sender = ((RecoverableSecurityException) se)
                    .getUserAction().getActionIntent().getIntentSender();
            launchDeleteRequest(sender);
        } else {
            Toast.makeText(context, "Delete failed: " + se.getMessage(), Toast.LENGTH_LONG).show();
            clearPending();
        }
    }

    private void launchDeleteRequest(IntentSender sender) {
        try {
            ((Activity) context).startIntentSenderForResult(
                    sender, DELETE_REQUEST_CODE, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Toast.makeText(context, "Could not start delete request", Toast.LENGTH_LONG).show();
            clearPending();
        }
    }

    /** Called by the hosting activity from onActivityResult after the user approves. */
    public void onDeleteConfirmed() {
        // On Android 10 the grant does NOT delete the file — we must run the delete now.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                && pendingDeleteUri != null) {
            try {
                context.getContentResolver().delete(pendingDeleteUri, null, null);
            } catch (Exception e) {
                deleteRawFile(pendingDeletePath);
            }
        }
        Toast.makeText(context, "File Deleted", Toast.LENGTH_SHORT).show();
        clearPending();
        // The hosting activity reloads the list afterwards to reflect the real filesystem.
    }

    /** Replace the backing data with a fresh query result and redraw. */
    public void updateList(ArrayList<VideoModel> newList) {
        videoFolder = new ArrayList<>(newList);
        lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    /** Last-resort physical delete (works when the app holds all-files / write access). */
    private boolean deleteRawFile(String path) {
        try {
            File f = new File(path);
            return f.exists() && f.delete();
        } catch (Exception e) {
            return false;
        }
    }

    private void clearPending() {
        pendingDeletePosition = -1;
        pendingDeleteUri = null;
        pendingDeletePath = null;
    }

    private void removeAt(int position) {
        if (position < 0 || position >= videoFolder.size()) return;
        videoFolder.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, videoFolder.size());
    }



    private void RenameFileName(int position) {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.rename_file);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        final EditText editText = dialog.findViewById(R.id.EditTitle);
        Button cancel = dialog.findViewById(R.id.Cancel);
        Button rename = dialog.findViewById(R.id.Rename);

        String fullName = new File(videoFolder.get(position).getPath()).getName();
        final String ext = fullName.contains(".") ? fullName.substring(fullName.lastIndexOf(".")) : "";
        String nameOnly = fullName.contains(".") ? fullName.substring(0, fullName.lastIndexOf(".")) : fullName;

        editText.setText(nameOnly);
        editText.requestFocus();
        editText.setSelection(editText.getText().length());

        cancel.setOnClickListener(v -> dialog.dismiss());

        rename.setOnClickListener(v -> {
            String newBase = editText.getText().toString().trim();
            if (newBase.isEmpty()) {
                editText.setError("Name can't be empty");
                return;
            }
            performRename(position, newBase + ext);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Scoped-storage-safe rename via MediaStore DISPLAY_NAME.
     *  - Android 11+ : request write consent, then update on RESULT_OK.
     *  - Android 10  : update throws RecoverableSecurityException -> prompt -> retry.
     *  - Older       : direct ContentResolver update.
     */
    private void performRename(int position, String newDisplayName) {
        Uri uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                Long.parseLong(videoFolder.get(position).getId()));
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName);

        pendingRenamePosition = position;
        pendingRenameUri = uri;
        pendingRenameValues = values;

        ContentResolver resolver = context.getContentResolver();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ArrayList<Uri> uris = new ArrayList<>();
                uris.add(uri);
                PendingIntent pi = MediaStore.createWriteRequest(resolver, uris);
                ((Activity) context).startIntentSenderForResult(
                        pi.getIntentSender(), RENAME_REQUEST_CODE, null, 0, 0, 0);
            } else {
                int rows = resolver.update(uri, values, null, null);
                Toast.makeText(context, rows > 0 ? "Renamed" : "Rename failed",
                        Toast.LENGTH_SHORT).show();
                clearPendingRename();
            }
        } catch (SecurityException se) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                handleRecoverableRename(se);
            } else {
                Toast.makeText(context, "Rename failed: " + se.getMessage(), Toast.LENGTH_LONG).show();
                clearPendingRename();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error renaming file", Toast.LENGTH_LONG).show();
            clearPendingRename();
        }
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.Q)
    private void handleRecoverableRename(SecurityException se) {
        if (se instanceof RecoverableSecurityException) {
            IntentSender sender = ((RecoverableSecurityException) se)
                    .getUserAction().getActionIntent().getIntentSender();
            try {
                ((Activity) context).startIntentSenderForResult(
                        sender, RENAME_REQUEST_CODE, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                Toast.makeText(context, "Could not start rename request", Toast.LENGTH_LONG).show();
                clearPendingRename();
            }
        } else {
            Toast.makeText(context, "Rename failed: " + se.getMessage(), Toast.LENGTH_LONG).show();
            clearPendingRename();
        }
    }

    /** Called by the hosting activity from onActivityResult after write consent is granted. */
    public void onRenameConfirmed() {
        if (pendingRenameUri != null && pendingRenameValues != null) {
            try {
                int rows = context.getContentResolver().update(pendingRenameUri, pendingRenameValues, null, null);
                Toast.makeText(context, rows > 0 ? "Renamed" : "Rename failed", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, "Rename failed", Toast.LENGTH_LONG).show();
            }
        }
        clearPendingRename();
    }

    private void clearPendingRename() {
        pendingRenamePosition = -1;
        pendingRenameUri = null;
        pendingRenameValues = null;
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
