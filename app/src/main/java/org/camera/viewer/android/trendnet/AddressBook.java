package org.camera.viewer.android.trendnet;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.camera.viewer.android.trendnet.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddressBook extends ListActivity {
    private Cursor cur;
    public static int g_variable;
    public static final String AUTHORITY = "org.camera.viewer.android.trendnet.cameraprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/camerainfos");
    private static final String[] PROJECTION = new String[]{"_id", "name", "host", "port", "username", "password", "model"};

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent.getData() == null)
            intent.setData(CONTENT_URI);
        cur = getContentResolver().query(getIntent().getData(), PROJECTION, null, null, null);
        ArrayList<Map<String, Object>> coll = new ArrayList<Map<String, Object>>();
        Map<String, Object> item;
        cur.moveToFirst();
        while (!cur.isAfterLast()) {
            item = new HashMap<String, Object>();
            item.put("name_item", cur.getString(1));
            item.put("host_item", cur.getString(2));
            coll.add(item);
            cur.moveToNext();
        }
        this.setListAdapter(new SimpleAdapter(this,
                coll,
                R.layout.addressbook,
                new String[]{"name_item", "host_item"},
                new int[]{R.id.name_item, R.id.host_item}));

        if (coll.size() < 1) {
            new Handler().postDelayed(openMenu, 1000);
        }

        System.gc();
    }

    private Runnable openMenu = new Runnable() {
        @Override
        public void run() {
            openOptionsMenu();
        }
    };

    protected void onUserLeaveHint() {
        finish();
        getApplication().onTerminate();
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.add).setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, 1, 1, R.string.info).setIcon(android.R.drawable.ic_menu_info_details);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case 0:
                Intent cfgIntent = new Intent();
                cfgIntent.setClass(AddressBook.this, CameraCfg.class);
                startActivity(cfgIntent);
                finish();
                break;
            case 1:
                Intent ifIntent = new Intent();
                ifIntent.setClass(AddressBook.this, InfoPage.class);
                startActivity(ifIntent);
                finish();
                break;
        }
        return true;
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        cur.moveToPosition(position);
        Intent ciIntent = new Intent();
        ciIntent.setClass(AddressBook.this, CheckInfo.class);
        Bundle info = new Bundle();
        info.putString("id", cur.getString(0));
        info.putString("name", cur.getString(1));
        info.putString("host", cur.getString(2));
        info.putString("port", cur.getString(3));

        //Query Encoding
        String u = cur.getString(4);
        String p = cur.getString(5);
        String key = new String(Hex.encodeHex(DigestUtils.md5(PreferenceManager.getDefaultSharedPreferences(this).getString("k", "HELLO") + "F")));

        String user = SecureUtility.decode(u, key);
        String pw = SecureUtility.decode(p, key);

        info.putString("username", user);
        info.putString("password", pw);
        info.putString("model", cur.getString(6));
        info.putString("mediatype", "MJPEG");
        ciIntent.putExtras(info);
        startActivity(ciIntent);
        finish();
    }
}