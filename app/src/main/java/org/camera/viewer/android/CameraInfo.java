package org.camera.viewer.android;

import android.net.Uri;
import android.provider.BaseColumns;

public class CameraInfo {
	public CameraInfo() {
	}
	public static final class CameraInfos implements BaseColumns {
		private CameraInfos() {
		}
		public static final Uri CONTENT_URI = Uri.parse("content://" + CameraProvider.AUTHORITY + "/camerainfos");
		public static final String CAMERA_ID = "_id";
		public static final String NAME = "name";
		public static final String HOST = "host";
		public static final String PORT = "port";
		public static final String USERNAME = "username";
		public static final String PASSWORD = "password";
		public static final String MODEL = "model";
	}
}
