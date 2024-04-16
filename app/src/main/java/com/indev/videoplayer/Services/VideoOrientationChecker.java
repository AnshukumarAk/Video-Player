package com.indev.videoplayer.Services;

import android.media.MediaMetadataRetriever;

public class VideoOrientationChecker {

    public static int getVideoOrientation(String videoPath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoPath);

        String rotationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (rotationString != null) {
            int rotation = Integer.parseInt(rotationString);
            if (rotation == 90 || rotation == 270) {
                // Video is in portrait orientation
                return 1; // Use your custom constant for portrait
            } else if (rotation == 0 || rotation == 180) {
                // Video is in landscape orientation
                return 2; // Use your custom constant for landscape
            }
        }

        // Default to unknown orientation
        return 0; // Use your custom constant for undefined
    }

}
