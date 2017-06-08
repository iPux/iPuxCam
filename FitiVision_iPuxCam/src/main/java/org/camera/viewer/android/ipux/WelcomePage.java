package org.camera.viewer.android.ipux;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import org.camera.viewer.android.ipux.R;

public class WelcomePage extends Activity {
    private ImageView welcomeBg;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcomepage);
        new Thread(showWelcome).start();
    }

    protected void onUserLeaveHint() {
        finish();
        getApplication().onTerminate();
    }

    private Runnable showWelcome = new Runnable() {

        public void run() {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Intent abIntent = new Intent();
            abIntent.setClass(WelcomePage.this, AddressBook.class);
            startActivity(abIntent);
            finish();
        }
    };
}
