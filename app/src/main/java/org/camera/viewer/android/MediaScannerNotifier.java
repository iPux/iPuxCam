package org.camera.viewer.android;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

public class MediaScannerNotifier implements
MediaScannerConnectionClient {
    private Context mContext;
    private MediaScannerConnection mConnection;
    private String mPath;
    private String mMimeType;

    public MediaScannerNotifier(Context context, String path, String mimeType) {
        mContext = context;
        mPath = path;
        mMimeType = mimeType;
        mConnection = new MediaScannerConnection(context, this);
        mConnection.connect();
    }

    public void onMediaScannerConnected() {
        mConnection.scanFile(mPath, mMimeType);
    }

    public void onScanCompleted(String path, Uri uri) {
        mConnection.disconnect();
        mContext = null;
    }

}

