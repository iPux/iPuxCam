package org.camera.viewer.android.trendnet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import org.apache.commons.codec.binary.Base64;
import org.camera.viewer.android.trendnet.R;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Random;

public class ViewPanel extends Activity {
    private boolean snap_shot = false;
    private boolean app_exit = true;
    private int touchDownX = 0;
    private int touchDownY = 0;
    private int touchUpX = 0;
    private int touchUpY = 0;
    private int moveX = 0;
    private int moveY = 0;
    private int listenStatus = 0;//0=not listen 1=listening
    private int talkStatus = 0;//0=not talk 1=talking
    public int snapIdx = 1;
    private String RemoteHost;
    private String RemotePort;
    private String AccountCode = "";
    private String savePath = "/sdcard/Snapshot/";
    private Bundle bundle;
    private Intent vpIntent;
    private HttpVideoStream httpVideo = null;
    private HttpAudioInStream httpAideoIn = null;
    private HttpAudioOutStream httpAideoOut = null;
    private PowerManager pm;
    private PowerManager.WakeLock wl;
    private Bitmap bitmap = null;
    private Bitmap resizeBmp = null;
    private Bitmap snapshotBmp = null;
    private ImageView viewStream;
    private ImageView viewbg;
    private TextView debugmsg;
    private ImageButton backBtn;
    private ImageButton listenBtn;
    private ImageButton talkBtn;
    private ImageButton snapshotBtn;
    //	private ImageButton zoominBtn;
//	private ImageButton zoomoutBtn;
    private ImageButton upBtn;
    private ImageButton downBtn;
    private ImageButton leftBtn;
    private ImageButton rightBtn;
    private ImageButton homeBtn;
    private AudioTrack audioTrackIn = null;
    private AudioManager volumeCtrl;
    private AbsoluteLayout control_layout;

    private final String myboundary = "--myboundary\r\n";
    private final String contentType = "Content-Type: multipart/mixed;boundary=myboundary\r\n\r\n";
    private static final String[] pt_cmd = {"home", "up", "down", "left", "right"};

    protected static final int SHOWIMAGE = 0x101;
    protected static final int SAVEIMAGE = 0x102;

    Handler myHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ViewPanel.SHOWIMAGE:
                    updateImgView();
                    break;
                case ViewPanel.SAVEIMAGE:
//					debugmsg.setText("DebugMsg:save image");
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss_");
                    String picName = (new StringBuilder(String.valueOf(df.format(new Date())))).append(snapIdx).toString();
                    picName = (new StringBuilder(picName)).append(".jpg").toString();
                    capture((new StringBuilder(String.valueOf(savePath))).append(picName).toString());
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void updateImgView() {
//		viewStream.setImageBitmap(bitmap);
        viewStream.setImageBitmap(resizeBmp);
    }

    private void capture(String s) {
        try {

            MediaPlayer mp = MediaPlayer.create(this, R.raw.snapshot);
            mp.start();

            File spdir = new File(savePath);
            if (!spdir.exists()) {
                if (!spdir.mkdirs()) {
//					debugmsg.setText("DebugMsg:Create folder fail");
                    return;
                }
            }

            FileOutputStream fileOutputStream = null;
            int quality = 50;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 5;
            while (snapshotBmp == null) {
                Thread.sleep(1);
                snapshotBmp = Bitmap.createScaledBitmap(bitmap, 320, 240, false);
//				debugmsg.setText("DebugMsg:bitnmap null");
            }
            fileOutputStream = new FileOutputStream(s);
            BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);
            snapshotBmp.compress(CompressFormat.JPEG, quality, bos);
            bos.flush();
            bos.close();
            snapIdx++;
//			new MediaScannerNotifier(this.createPackageContext("org.camera.viewer.android", CONTEXT_INCLUDE_CODE ),s, "image/jpeg");
            new MediaScannerNotifier(this.getApplicationContext(), s, "image/jpeg");
//			debugmsg.setText("DebugMsg:save finished "+s);
        } catch (Exception e) {
            e.printStackTrace();
//			debugmsg.setText("DebugMsg:save error"+e.toString());
        }
        snapshotBtn.setEnabled(true);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpanel);

        snap_shot = false;

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "");
        wl.acquire();

        vpIntent = this.getIntent();
        bundle = vpIntent.getExtras();

        RemoteHost = bundle.getString("host");
        RemotePort = bundle.getString("port");
        String authCombination = bundle.getString("username") + ":" + bundle.getString("password");
        AccountCode = new String(Base64.encodeBase64(authCombination.getBytes()));

        control_layout = (AbsoluteLayout) this.findViewById(R.id.control_layout);

        debugmsg = (TextView) findViewById(R.id.debugmsg);
//        debugmsg.setText("DebugMsg:");

        viewStream = (ImageView) findViewById(R.id.viewstream);
        viewbg = (ImageView) findViewById(R.id.viewbg);

        backBtn = (ImageButton) findViewById(R.id.back);
        backBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                backEvent();
            }

        });

        listenBtn = (ImageButton) findViewById(R.id.listen);
        listenBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                listenEvent();
            }

        });

        talkBtn = (ImageButton) findViewById(R.id.talk);
        talkBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                talkEvent();
            }

        });

        snapshotBtn = (ImageButton) findViewById(R.id.snapshot);
        snapshotBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                snapshotBtn.setEnabled(false);
                snapshotEvent();
            }

        });

        listenBtn = (ImageButton) findViewById(R.id.listen);
        listenBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                listenEvent();
            }

        });
        
       /*zoominBtn = (ImageButton)findViewById(R.id.zoomin);
        zoominBtn.setOnClickListener(new Button.OnClickListener()
        {
        	@Override
			public void onClick(View v) {
//        		debugmsg.setText("DebugMsg:zoom in");
			}
        	
        });
        
        zoomoutBtn = (ImageButton)findViewById(R.id.zoomout);
        zoomoutBtn.setOnClickListener(new Button.OnClickListener()
        {
        	@Override
			public void onClick(View v) {
//        		debugmsg.setText("DebugMsg:zoom out");
			}
        	
        });*/

        upBtn = (ImageButton) findViewById(R.id.up);
        upBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ptMoveAction(pt_cmd[1]);//up
//				debugmsg.setText("DebugMsg:up");
            }

        });

        downBtn = (ImageButton) findViewById(R.id.down);
        downBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ptMoveAction(pt_cmd[2]);//down
//				debugmsg.setText("DebugMsg:down");
            }

        });

        leftBtn = (ImageButton) findViewById(R.id.left);
        leftBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ptMoveAction(pt_cmd[3]);//left
//				debugmsg.setText("DebugMsg:left");
            }

        });

        rightBtn = (ImageButton) findViewById(R.id.right);
        rightBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ptMoveAction(pt_cmd[4]);//right
//				debugmsg.setText("DebugMsg:right");
            }

        });

        homeBtn = (ImageButton) findViewById(R.id.home);
        homeBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ptMoveAction(pt_cmd[0]);//home
//				debugmsg.setText("DebugMsg:home");
            }

        });

        //check hardware support
        int talk = checkTalk();
        if (talk == 0) {
            talkBtn.setEnabled(false);
            talkBtn.setImageResource(R.drawable.d7);
        }

        int listen = checkListen();
        if (listen == 0) {
            listenBtn.setEnabled(false);
            listenBtn.setImageResource(R.drawable.d9);
        }

        int pt = checkPanTilt();
        if (pt == 0) {
            upBtn.setEnabled(false);
            upBtn.setImageResource(R.drawable.d2);
            downBtn.setEnabled(false);
            downBtn.setImageResource(R.drawable.d8);
            homeBtn.setEnabled(false);
            homeBtn.setImageResource(R.drawable.home_d);
            leftBtn.setEnabled(false);
            leftBtn.setImageResource(R.drawable.d4);
            rightBtn.setEnabled(false);
            rightBtn.setImageResource(R.drawable.d6);

        }

        httpVideo = new HttpVideoStream(bundle);
        httpAideoIn = new HttpAudioInStream(bundle);
        httpAideoOut = new HttpAudioOutStream();

        new Thread(showVideo).start();
//        new Thread(inAudio).start();
//        new Thread(outAudio).start();
    }

    private void backEvent() {
        app_exit = false;
        if (httpVideo != null)
            httpVideo.stop();
        if (httpAideoIn != null)
            httpAideoIn.stop();
        if (httpAideoOut != null)
            httpAideoOut.stop();
        if (wl != null)
            wl.release();
        if (httpAideoOut != null)
            httpAideoOut.release();

        Intent abIntent = new Intent();
        abIntent.setClass(ViewPanel.this, AddressBook.class);
        startActivity(abIntent);
        finish();
    }

    private void listenEvent() {
        if (listenStatus == 0) {
            listenStatus = 1;
            listenBtn.setImageResource(R.drawable.o9);
            httpAideoIn.start();
            new Thread(inAudio).start();
//			debugmsg.setText("DebugMsg:Listen start");
        } else {
            listenStatus = 0;
            listenBtn.setImageResource(R.drawable.a9);
            httpAideoIn.stop();
//			debugmsg.setText("DebugMsg:Listen stop");
        }
    }

    private void talkEvent() {
        if (talkStatus == 0) {
            talkStatus = 1;
            talkBtn.setImageResource(R.drawable.o7);
            httpAideoOut.start();
            new Thread(outAudio).start();
//			debugmsg.setText("DebugMsg:Talk start");

        } else {
            talkStatus = 0;
            talkBtn.setImageResource(R.drawable.a7);
            httpAideoOut.stop();
//			debugmsg.setText("DebugMsg:Talk stop");
        }
    }

    private void snapshotEvent() {
        if (snap_shot == false) {
            snap_shot = true;
        }
    }

    private void ptMoveAction(String cmd) {
        HttpURLConnection httpconn = null;
        URL myurl;
        int returnCode = 0;
        try {
            String authCombination = bundle.getString("username") + ":" + bundle.getString("password");
            myurl = new URL("http", bundle.getString("host"), Integer.parseInt(bundle.getString("port")), "/cgi/admin/ptctrl.cgi?action=move&Cmd=" + cmd);
            httpconn = (HttpURLConnection) myurl.openConnection();
            httpconn.setRequestProperty("Authorization", "Basic " + new String(Base64.encodeBase64(authCombination.getBytes())));
//			httpconn.addRequestProperty("", "\r\nAuthorization: Basic "+ new String(Base64.encodeBase64(authCombination.getBytes())));
            httpconn.connect();
            returnCode = httpconn.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (httpconn != null)
            httpconn.disconnect();
    }

    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    public boolean CreateImage(byte[] imgbuf, int len) {
        if (bitmap != null)
            bitmap = null;
        if (resizeBmp != null)
            resizeBmp = null;
        bitmap = BitmapFactory.decodeByteArray(imgbuf, 0, len);
        resizeBmp = Bitmap.createScaledBitmap(bitmap, 320, 240, false);
        if (bitmap == null || resizeBmp == null) {
//			System.err.println("<err>:Fail to create imgPic");
            return false;
        }
        return true;
    }

    private boolean checkJPG(byte[] buf, int len) {
        boolean ret = false;
        if (len < 1)
            return false;

        if ((buf[0] == (byte) 0xff) && (buf[1] == (byte) 0xd8)) {
//			 System.err.println("error JPG header");
            ret = true;
        } else {
//			System.err.println("error JPG header");
            return false;
        }

        if ((buf[len - 2] == (byte) 0xff) && (buf[len - 1] == (byte) 0xd9)) {
//			 System.err.println("error JPG tailer");
            ret = true;
        } else {
//			System.err.println("error JPG tailer");
            return false;
        }
        return ret;

    }

    private Runnable showVideo = new Runnable() {

        public void run() {
            boolean chk = true;
            while (httpVideo.bVideoShowRunning == true) {
                if (httpVideo.vbuf == null)
                    continue;
                if (httpVideo.vbuf.state[httpVideo.vbuf.idx] != StreamBuffer.BF_WRITE_OK) { // Not OK for Read
                    httpVideo.httpparser.nonsafeSleep(1);
                    continue;
                }
                httpVideo.vbuf.state[httpVideo.vbuf.idx] = StreamBuffer.BF_IN_USE;

                chk = checkJPG(httpVideo.vbuf.buf[httpVideo.vbuf.idx], httpVideo.vbuf.len[httpVideo.vbuf.idx]);
                if (chk)// check is ok
                {
                    if (CreateImage(httpVideo.vbuf.buf[httpVideo.vbuf.idx], httpVideo.vbuf.len[httpVideo.vbuf.idx]) == false) {
                        httpVideo.vbuf.state[httpVideo.vbuf.idx] = StreamBuffer.BF_FREE;
                        httpVideo.vbuf.idx = (httpVideo.vbuf.idx + 1) % httpVideo.vbuf.maxBufSlot;
//						System.err.println("create fail?");
                        continue;
                    } else {
                        httpVideo.imgOK = true;

//						show image
                        Message message = new Message();
                        message.what = ViewPanel.SHOWIMAGE;
                        myHandler.sendMessage(message);

                        if (snap_shot == true) {
                            snap_shot = false;
                            (new Thread() {

                                public void run() {
                                    snapshotBmp = Bitmap.createScaledBitmap(bitmap, 320, 240, false);
                                    Message saveMsg = new Message();
                                    saveMsg.what = ViewPanel.SAVEIMAGE;
                                    myHandler.sendMessage(saveMsg);
                                }
                            }).start();
                        }
                    }

                }
                httpVideo.httpparser.nonsafeSleep(1);

                httpVideo.vbuf.state[httpVideo.vbuf.idx] = StreamBuffer.BF_FREE;
//				System.err.println("vbuf.state[" + vbuf.idx + "][FREE]: "
//						+ vbuf.state[vbuf.idx]);
                httpVideo.vbuf.idx = (httpVideo.vbuf.idx + 1) % httpVideo.vbuf.maxBufSlot;

            }

        }

    };

    private Runnable inAudio = new Runnable() {

        public void run() {
            try {
                if (audioTrackIn != null) {
                    audioTrackIn.release();
                    audioTrackIn = null;
                }

                int iMinBufSize = AudioTrack.getMinBufferSize(8000,
                        AudioFormat.CHANNEL_CONFIGURATION_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
                audioTrackIn = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
                        AudioFormat.CHANNEL_CONFIGURATION_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        iMinBufSize,
                        AudioTrack.MODE_STREAM);
                audioTrackIn.play();
                if (audioTrackIn.setStereoVolume(1.0f, 1.0f) == AudioTrack.SUCCESS) {
//					debugmsg.setText("setStereo");
                }
                while (httpAideoIn.bAudioPlayRunning == true) {
                    if (httpAideoIn.abuf.state[httpAideoIn.abuf.idx] != StreamBuffer.BF_WRITE_OK) { // Not OK for Read
                        httpAideoIn.httpparser.nonsafeSleep(1);
                        continue;
                    }
                    httpAideoIn.abuf.state[httpAideoIn.abuf.idx] = StreamBuffer.BF_IN_USE;
                    audioTrackIn.write(httpAideoIn.abuf.buf[httpAideoIn.abuf.idx], 0, httpAideoIn.abuf.len[httpAideoIn.abuf.idx]);
                    httpAideoIn.httpparser.nonsafeSleep(1);

                    httpAideoIn.abuf.state[httpAideoIn.abuf.idx] = StreamBuffer.BF_FREE;
                    httpAideoIn.abuf.idx = (httpAideoIn.abuf.idx + 1) % httpAideoIn.abuf.maxBufSlot;

                }
                audioTrackIn.stop();
                audioTrackIn.release();
                audioTrackIn = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };
    private Runnable outAudio = new Runnable() {

        public void run() {
            Random ran = new Random(System.currentTimeMillis());
            while (httpAideoOut.bMicSendRunning == true) {
                String tk_sRequest;
                try {
                    Socket tk_sktClient;
                    BufferedReader tk_Input;
                    DataOutputStream tk_Output;

                    tk_sRequest = "";
                    tk_sRequest = tk_sRequest + "POST " + "/cgi/audio/audio.cgi?type=PCM" + " HTTP/1.1\r\n";
                    tk_sRequest = tk_sRequest + "Authorization: Basic " + AccountCode + "\r\n";
                    tk_sRequest = tk_sRequest + contentType;
                    tk_sRequest = tk_sRequest + "\r\n";

                    tk_sktClient = new Socket(RemoteHost, Integer.parseInt(RemotePort));

                    tk_Output = new DataOutputStream(tk_sktClient.getOutputStream());
                    tk_Input = new BufferedReader(new InputStreamReader(tk_sktClient.getInputStream()));
                    tk_Output.write(tk_sRequest.getBytes());

                    while (httpAideoOut.bMicSendRunning == true) {
                        if (httpAideoOut.tbuf.state[httpAideoOut.tbuf.idx] != StreamBuffer.BF_WRITE_OK) { // Not OK for Read
                            httpAideoOut.httpparser.nonsafeSleep(1);
                            continue;
                        }
                        httpAideoOut.tbuf.state[httpAideoOut.tbuf.idx] = StreamBuffer.BF_IN_USE;
                        //-send http packet
                        tk_Output.write(myboundary.getBytes(), 0, myboundary.length());
                        String contentLength = "Content-Length: " + (httpAideoOut.hbuf.length + httpAideoOut.tbuf.len[httpAideoOut.tbuf.idx]) + "\r\n\r\n";
                        tk_Output.write(contentLength.getBytes(), 0, contentLength.length());
                        tk_Output.write(httpAideoOut.hbuf, 0, httpAideoOut.hbuf.length);
                        tk_Output.write(httpAideoOut.tbuf.buf[httpAideoOut.tbuf.idx], 0, httpAideoOut.tbuf.len[httpAideoOut.tbuf.idx]);
                        httpAideoOut.tbuf.state[httpAideoOut.tbuf.idx] = StreamBuffer.BF_FREE;
                        httpAideoOut.tbuf.idx = (httpAideoOut.tbuf.idx + 1) % httpAideoOut.tbuf.maxBufSlot;
                    }
                    tk_Output.close();
                    tk_sktClient.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    httpAideoOut.httpparser.nonsafeSleep(3000 + ran.nextInt() % 2000);
                    continue;
                }
            }
        }

    };

    protected void onUserLeaveHint() {
        if (app_exit) {
            if (httpVideo != null)
                httpVideo.stop();
            if (httpAideoIn != null)
                httpAideoIn.stop();
            if (httpAideoOut != null)
                httpAideoOut.stop();
            if (wl != null)
                wl.release();
            if (httpAideoOut != null)
                httpAideoOut.release();
            finish();
            getApplication().onTerminate();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                backEvent();
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                volumeCtrl = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                volumeCtrl.adjustVolume(AudioManager.ADJUST_RAISE, 0);
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                volumeCtrl = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                volumeCtrl.adjustVolume(AudioManager.ADJUST_LOWER, 0);
                break;
        }
        return true;
    }

    private int checkListen() {
        try {
            String cfg = "/cgi/param.cgi?action=list&group=Hardware&name=MicIn";
            SendHttpRequest gt = new SendHttpRequest(new URL("http", RemoteHost, Integer.parseInt(RemotePort), cfg.toString()), AccountCode);
            int i = 0;
            int ret = gt.getConfig("MicIn");
            if (ret == 200) {    //----200 OK
                for (i = 0; i < gt.param.size(); i++) {
                    Hashtable t = new Hashtable();
                    t = (Hashtable) (gt.param.get(i));
                    if (t.containsKey("MicIn"))
                        return Integer.parseInt((String) t.get("MicIn"));
                    else
                        return 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int checkTalk() {
        try {
            String cfg = "/cgi/param.cgi?action=list&group=Hardware&name=AudioOut";
            SendHttpRequest gt = new SendHttpRequest(new URL("http", RemoteHost, Integer.parseInt(RemotePort), cfg.toString()), AccountCode);
            int i = 0;
            int ret = gt.getConfig("AudioOut");
            if (ret == 200) {    //----200 OK
                for (i = 0; i < gt.param.size(); i++) {
                    Hashtable t = new Hashtable();
                    t = (Hashtable) (gt.param.get(i));
                    if (t.containsKey("AudioOut"))
                        return Integer.parseInt((String) t.get("AudioOut"));
                    else
                        return 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int checkPanTilt() {
        try {
            String cfg = "/cgi/param.cgi?action=list&group=Hardware&name=PT";
            SendHttpRequest gt = new SendHttpRequest(new URL("http", RemoteHost, Integer.parseInt(RemotePort), cfg.toString()), AccountCode);
            int i = 0;
            int ret = gt.getConfig("PT");
            if (ret == 200) {    //----200 OK
                for (i = 0; i < gt.param.size(); i++) {
                    Hashtable t = new Hashtable();
                    t = (Hashtable) (gt.param.get(i));
                    if (t.containsKey("PT"))
                        return Integer.parseInt((String) t.get("PT"));
                    else
                        return 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setView();
        super.onConfigurationChanged(newConfig);
    }

    private void setView() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            control_layout.setVisibility(View.VISIBLE);
            viewStream.setLayoutParams(new AbsoluteLayout.LayoutParams((int) (320 * getResources().getDisplayMetrics().density), (int) (240 * getResources().getDisplayMetrics().density), 0, 0));
            viewStream.setScaleType(ScaleType.FIT_XY);
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            control_layout.setVisibility(View.GONE);
            viewStream.setLayoutParams(new AbsoluteLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 0, 0));
            viewStream.setScaleType(ScaleType.FIT_XY);
        }
    }
}
