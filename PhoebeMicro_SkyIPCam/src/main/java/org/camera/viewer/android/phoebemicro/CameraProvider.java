package org.camera.viewer.android.phoebemicro;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import org.camera.viewer.android.phoebemicro.CameraInfo.CameraInfos;

import java.util.HashMap;

public class CameraProvider extends ContentProvider {

    private static final String TAG = "CameraProvider";
    private static final String DATABASE_NAME = "cameraprovider.db";
    private static final int DATABASE_VERSION = 1;
    private static final String CAMINFO_TABLE_NAME = "camerainfos";
    public static final String AUTHORITY = "org.camera.viewer.android.phoebemicro.cameraprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/camerainfos");

    private static final UriMatcher sUriMatcher;
    private static final int NOTES = 1;
    private static HashMap<String, String> cameraInfoMap;

    public class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + CAMINFO_TABLE_NAME + " (" + CameraInfos.CAMERA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + CameraInfos.NAME + " VARCHAR(255)," + CameraInfos.HOST + " LONGTEXT," + CameraInfos.PORT + " LONGTEXT,"
                    + CameraInfos.USERNAME + " LONGTEXT," + CameraInfos.PASSWORD + " LONGTEXT," + CameraInfos.MODEL + " LONGTEXT" + ");");

            PreferenceManager.getDefaultSharedPreferences(CameraProvider.this.getContext()).edit().putString("k", String.valueOf(new java.util.Date().getTime())).commit();
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + CAMINFO_TABLE_NAME);
            onCreate(db);
        }
    }

    private DatabaseHelper dbHelper;

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.delete(CAMINFO_TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    public String getType(Uri uri) {
        return null;
    }

    public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(CAMINFO_TABLE_NAME, CameraInfos.MODEL, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(CameraProvider.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                qb.setTables(CAMINFO_TABLE_NAME);
                qb.setProjectionMap(cameraInfoMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.update(CAMINFO_TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, CAMINFO_TABLE_NAME, NOTES);
        cameraInfoMap = new HashMap<String, String>();
        cameraInfoMap.put(CameraInfos.CAMERA_ID, CameraInfos.CAMERA_ID);
        cameraInfoMap.put(CameraInfos.NAME, CameraInfos.NAME);
        cameraInfoMap.put(CameraInfos.HOST, CameraInfos.HOST);
        cameraInfoMap.put(CameraInfos.PORT, CameraInfos.PORT);
        cameraInfoMap.put(CameraInfos.USERNAME, CameraInfos.USERNAME);
        cameraInfoMap.put(CameraInfos.PASSWORD, CameraInfos.PASSWORD);
        cameraInfoMap.put(CameraInfos.MODEL, CameraInfos.MODEL);
    }

}
