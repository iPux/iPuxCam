package org.camera.viewer.android;

import org.camera.viewer.android.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

public class InfoPage extends Activity{
	private WebView info;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.infopage);
        
        info = (WebView)findViewById(R.id.info);
        info.loadUrl("file:///android_asset/Info.html");
	}
	
	protected void  onUserLeaveHint  (){
		finish();
		getApplication().onTerminate();
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, 0, 0, R.string.addbook).setIcon(android.R.drawable.ic_menu_agenda);
	    return super.onCreateOptionsMenu(menu);
    }
	
	public boolean onOptionsItemSelected(MenuItem item)
    {
      super.onOptionsItemSelected(item);
      switch(item.getItemId())
      {
        case 0:
        	Intent abIntent = new Intent();
			abIntent.setClass(InfoPage.this, AddressBook.class);
			startActivity(abIntent);
        	finish();
          break;
      }
      return true;
    }
	
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
			Intent abIntent = new Intent();
			abIntent.setClass(InfoPage.this, AddressBook.class);
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
