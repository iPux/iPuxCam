package org.camera.viewer.android.phoebemicro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.camera.viewer.android.phoebemicro.CameraInfo.CameraInfos;
import org.camera.viewer.android.phoebemicro.R;

import java.net.URL;
import java.util.Hashtable;

public class CameraCfg extends Activity {
    private static final String[] media_type = {"H264", "MPEG4", "MJPEG"};
    private EditText name;
    private EditText host;
    private EditText port;
    private EditText username;
    private EditText password;
    private EditText model;
    private Spinner mType;
    private ArrayAdapter<String> mTypeAdapter;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cameracfg);

        name = (EditText) findViewById(R.id.name);
        host = (EditText) findViewById(R.id.ip);
        port = (EditText) findViewById(R.id.port);
        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        model = (EditText) findViewById(R.id.model);
        model.setEnabled(false);

        /*mType = (Spinner)findViewById(R.id.mediatype);
        mTypeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,media_type);
        mType.setAdapter(mTypeAdapter);*/

        Button save = (Button) findViewById(R.id.save);
        save.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                ContentValues values = new ContentValues();
                values.put(CameraInfos.NAME, name.getText().toString());
                values.put(CameraInfos.HOST, host.getText().toString());
                values.put(CameraInfos.PORT, port.getText().toString());

                //insert Encoding
                String key = new String(Hex.encodeHex(DigestUtils.md5(PreferenceManager.getDefaultSharedPreferences(CameraCfg.this).getString("k", "HELLO") + "F")));

                String user = SecureUtility.encode(username.getText().toString(), key);
                String pw = SecureUtility.encode(password.getText().toString(), key);

                values.put(CameraInfos.USERNAME, user);
                values.put(CameraInfos.PASSWORD, pw);

                values.put(CameraInfos.MODEL, model.getText().toString());
                Uri newAddUri = CameraCfg.this.getContentResolver().insert(CameraProvider.CONTENT_URI, values);

                Intent abIntent = new Intent();
                abIntent.setClass(CameraCfg.this, AddressBook.class);
                startActivity(abIntent);
                finish();
            }

        });

        Button contact = (Button) findViewById(R.id.contact);
        contact.setOnClickListener(new Button.OnClickListener() {

            public void onClick(View arg0) {
                String modelName = getModelName();
                if (modelName.equals("")) {
                    new AlertDialog.Builder(CameraCfg.this)
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

        Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new Button.OnClickListener() {

            public void onClick(View v) {
                Intent abIntent = new Intent();
                abIntent.setClass(CameraCfg.this, AddressBook.class);
                startActivity(abIntent);
                finish();
            }

        });
    }

    protected void onUserLeaveHint() {
        finish();
        getApplication().onTerminate();
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
                abIntent.setClass(CameraCfg.this, AddressBook.class);
                startActivity(abIntent);
                finish();
                break;
        }
        return true;
    }
}