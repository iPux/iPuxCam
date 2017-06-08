package org.camera.viewer.android.ipux;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.camera.viewer.android.ipux.CameraInfo.CameraInfos;
import org.camera.viewer.android.ipux.R;

import java.net.URL;
import java.util.Hashtable;

public class CheckInfo extends Activity {
    private static final String[] media_type = {"H264", "MPEG4", "MJPEG"};
    private EditText name;
    private EditText host;
    private EditText port;
    private EditText username;
    private EditText password;
    private EditText model;
    private Button save;
    private Button contact;
    private Button cancel;
    private Spinner mType;
    private ArrayAdapter<String> mTypeAdapter;
    private Bundle bundle;
    private Intent ciIntent;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkinfo);

        ciIntent = this.getIntent();
        bundle = ciIntent.getExtras();

        name = (EditText) findViewById(R.id.name_i);
        host = (EditText) findViewById(R.id.ip_i);
        port = (EditText) findViewById(R.id.port_i);
        username = (EditText) findViewById(R.id.username_i);
        password = (EditText) findViewById(R.id.password_i);
        model = (EditText) findViewById(R.id.model_i);
        model.setEnabled(false);

        name.setText(bundle.getString("name"));
        host.setText(bundle.getString("host"));
        port.setText(bundle.getString("port"));
        username.setText(bundle.getString("username"));
        password.setText(bundle.getString("password"));
        model.setText(bundle.getString("model"));

        /*mType = (Spinner)findViewById(R.id.mediatype);
        mTypeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,media_type);
        mType.setAdapter(mTypeAdapter);*/

        save = (Button) findViewById(R.id.save_i);
        save.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                ContentValues values = new ContentValues();
                values.put(CameraInfos.NAME, name.getText().toString());
                values.put(CameraInfos.HOST, host.getText().toString());
                values.put(CameraInfos.PORT, port.getText().toString());

                String key = new String(Hex.encodeHex(DigestUtils.md5(PreferenceManager.getDefaultSharedPreferences(CheckInfo.this).getString("k", "HELLO") + "F")));
                String user = SecureUtility.encode(username.getText().toString(), key);
                String pw = SecureUtility.encode(password.getText().toString(), key);

                values.put(CameraInfos.USERNAME, user);
                values.put(CameraInfos.PASSWORD, pw);
                values.put(CameraInfos.MODEL, model.getText().toString());
                CheckInfo.this.getContentResolver().update(CameraProvider.CONTENT_URI, values, "_id=" + bundle.getString("id"), null);

                Intent abIntent = new Intent();
                abIntent.setClass(CheckInfo.this, AddressBook.class);
                startActivity(abIntent);
                finish();
            }

        });

        contact = (Button) findViewById(R.id.contact_i);
        contact.setOnClickListener(new Button.OnClickListener() {

            public void onClick(View arg0) {
                String modelName = getModelName();
                if (modelName.equals("")) {
                    new AlertDialog.Builder(CheckInfo.this)
                            .setTitle(R.string.error)
                            .setMessage(R.string.connectfail)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                } else
                    model.setText(modelName);
            }

        });

        cancel = (Button) findViewById(R.id.cancel_i);
        cancel.setOnClickListener(new Button.OnClickListener() {

            public void onClick(View v) {
                Intent abIntent = new Intent();
                abIntent.setClass(CheckInfo.this, AddressBook.class);
                startActivity(abIntent);
                finish();
            }

        });
        
        /*connect = (Button)findViewById(R.id.connect_i);
        connect.setOnClickListener(new Button.OnClickListener()
        {

			public void onClick(View v) {
				Intent vpIntent = new Intent();
				vpIntent.setClass(CheckInfo.this, ViewPanel.class);
				vpIntent.putExtras(bundle);
	        	startActivity(vpIntent);
	        	finish();
			}
        	
        });*/

        name.setEnabled(false);
        host.setEnabled(false);
        port.setEnabled(false);
        username.setEnabled(false);
        password.setEnabled(false);

        save.setVisibility(View.INVISIBLE);
        contact.setVisibility(View.INVISIBLE);
        cancel.setVisibility(View.INVISIBLE);
		
		/*name.setFocusable(false);
		host.setFocusable(false);
		port.setFocusable(false);
		username.setFocusable(false);
		password.setFocusable(false);
		model.setFocusable(false);*/

    }

    protected void onUserLeaveHint() {
        finish();
        getApplication().onTerminate();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.connect).setIcon(android.R.drawable.ic_menu_view);
        menu.add(0, 1, 1, R.string.modify).setIcon(android.R.drawable.ic_menu_edit);
        menu.add(0, 2, 2, R.string.delete).setIcon(android.R.drawable.ic_menu_delete);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case 0:
                String modelName = getModelName();
                if (modelName.equals("")) {
                    new AlertDialog.Builder(CheckInfo.this)
                            .setTitle(R.string.error)
                            .setMessage(R.string.connectfail)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                } else {
                    ContentValues values = new ContentValues();
                    values.put(CameraInfos.NAME, name.getText().toString());
                    values.put(CameraInfos.HOST, host.getText().toString());
                    values.put(CameraInfos.PORT, port.getText().toString());

                    //Update Encoding
                    String key = new String(Hex.encodeHex(DigestUtils.md5(PreferenceManager.getDefaultSharedPreferences(this).getString("k", "HELLO") + "F")));

                    String user = SecureUtility.encode(username.getText().toString(), key);
                    String pw = SecureUtility.encode(password.getText().toString(), key);

                    values.put(CameraInfos.USERNAME, user);
                    values.put(CameraInfos.PASSWORD, pw);
                    values.put(CameraInfos.MODEL, modelName);
                    CheckInfo.this.getContentResolver().update(CameraProvider.CONTENT_URI, values, "_id=" + bundle.getString("id"), null);

                    Bundle info = new Bundle();
                    info.putString("id", bundle.getString("id"));
                    info.putString("name", name.getText().toString());
                    info.putString("host", host.getText().toString());
                    info.putString("port", port.getText().toString());
                    info.putString("username", username.getText().toString());
                    info.putString("password", password.getText().toString());
                    info.putString("model", modelName);
                    info.putString("mediatype", "MJPEG");

                    Intent vpIntent = new Intent();
                    vpIntent.setClass(CheckInfo.this, ViewPanel.class);
                    vpIntent.putExtras(info);
                    startActivity(vpIntent);
                    finish();
                }
                break;
            case 1:
                name.setEnabled(true);
                host.setEnabled(true);
                port.setEnabled(true);
                username.setEnabled(true);
                password.setEnabled(true);

//			connect.setVisibility(View.INVISIBLE);
                save.setVisibility(View.VISIBLE);
                contact.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.VISIBLE);
                break;
            case 2:
                CheckInfo.this.getContentResolver().delete(CameraProvider.CONTENT_URI, "_id=" + bundle.getString("id"), null);

                Intent abIntent = new Intent();
                abIntent.setClass(CheckInfo.this, AddressBook.class);
                startActivity(abIntent);
                finish();
                break;
        }
        return true;
    }

    private String getModelName() {
        try {
            String cfg = "/cgi/param.cgi?action=list&group=System.Info&name=ModelName";
            String authCombination = username.getText().toString() + ":" + password.getText().toString();
            String AccountCode = new String(Base64.encodeBase64(authCombination.getBytes()));
            SendHttpRequest gt = new SendHttpRequest(new URL("http", host.getText().toString(), Integer.parseInt(port.getText().toString()), cfg.toString()), AccountCode);
            int i = 0;
            int ret = gt.getConfig("ModelName");
            if (ret == 200) {    //----200 OK
                for (i = 0; i < gt.param.size(); i++) {
                    Hashtable t = new Hashtable();
                    t = (Hashtable) (gt.param.get(i));
                    if (t.containsKey("ModelName"))
                        return String.valueOf(t.get("ModelName"));
                    else
                        return "";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                Intent abIntent = new Intent();
                abIntent.setClass(CheckInfo.this, AddressBook.class);
                startActivity(abIntent);
                finish();
                break;
            case KeyEvent.KEYCODE_MENU:
                openOptionsMenu();
                break;
        }

        return true;
    }
}
