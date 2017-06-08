package org.camera.viewer.android.phoebemicro;

import android.os.Bundle;

import org.apache.commons.codec.binary.Base64;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class HttpVideoStream implements Runnable {

    public HttpParser httpparser;
    private Thread thdVideoGet;
    private boolean bVideoGetRunning;
    public boolean bVideoShowRunning;
    public boolean imgOK = false;
    private int vbufIdx = 0;

    private String codebase = "/cgi/";
    private String AccountCode = "";
    private String mType;

    public String RemoteHost;
    public String RemotePort;

    public StreamBuffer vbuf;

    private final int MAX_BUFFER_SIZE = 200 * 1024;

    public HttpVideoStream(Bundle connInfo) {
        init(connInfo);
        start();
    }

    private void init(Bundle connInfo) {

        RemoteHost = connInfo.getString("host");
        RemotePort = connInfo.getString("port");
        String authCombination = connInfo.getString("username") + ":" + connInfo.getString("password");
        AccountCode = new String(Base64.encodeBase64(authCombination.getBytes()));
        String mType = connInfo.getString("mediatype");
        if (mType.equals("H264"))
            codebase = codebase + "h264/h264.cgi";
        else if (mType.equals("MPEG4"))
            codebase = codebase + "mpeg4/mpeg4.cgi";
        else if (mType.equals("MJPEG"))
            codebase = codebase + "mjpg/mjpeg.cgi";
        else {
            mType = "MJPEG";
            codebase = codebase + "mjpg/mjpeg.cgi";
        }
        vbuf = new StreamBuffer(8, MAX_BUFFER_SIZE);// 8 slots, 200k each
        httpparser = new HttpParser();
        bVideoGetRunning = false;
        bVideoShowRunning = false;
    }

    private void VideoGetThread() {
        HttpURLConnection httpconn = null;
        Random ran = new Random(System.currentTimeMillis());
        while (bVideoGetRunning == true) {

            //System.err.println("VideoGetThread start 2");

            URL myurl;

            int size = 0;
            int cnt = 0;
            //long tmp = 0;
            httpparser.bufAvailable = 0;
            httpparser.bufRead = 0;

            try {
                myurl = new URL("http", RemoteHost, Integer.parseInt(RemotePort), codebase);
                //System.err.println("myurl = " + myurl.toString());

                httpconn = (HttpURLConnection) myurl.openConnection();
                httpconn.setRequestProperty("Authorization", "Basic " + AccountCode);
//				httpconn.addRequestProperty("", "\r\nAuthorization: Basic "+ AccountCode);
                httpconn.connect();
                BufferedInputStream in = new BufferedInputStream(httpconn
                        .getInputStream());


                if (httpconn.getResponseCode() != HttpURLConnection.HTTP_OK) {
//					System.err.println("response  = "+ httpconn.getResponseCode());
                    continue;
                    // return;
                }
                String contentType = httpconn.getContentType();
                //System.err.println("contentType2 = " + contentType);
                String boundary = httpparser.parseBoundary(contentType);
                if (boundary.length() == 0) {
//					System.err.println("no boundary, exit");
                    continue;
                    // return;
                }
                boundary = "--" + boundary;
                //System.err.println("Boundary: " + boundary);
                httpparser.seekToPattern(in, boundary);
                //httpparser.seekToPattern(in, "\r\n\r\n");
                //e1 = System.currentTimeMillis();
                while (bVideoGetRunning == true) {
                    while (vbuf.state[cnt] == StreamBuffer.BF_IN_USE) {
                        cnt = (cnt + 1) % vbuf.maxBufSlot;
                    }
                    // mark this buf as "in use"
                    vbuf.state[cnt] = StreamBuffer.BF_IN_USE;
                    size = httpparser.getOneBoundary(in, boundary, vbuf.buf[cnt], MAX_BUFFER_SIZE, vbuf.header[cnt]);
                    //System.err.println("a4. size = " + size);


                    if (size <= 0) {
                        throw new IOException("connection timeout");
                    } else {
                        // mark buf as "write OK"
                        vbuf.state[cnt] = StreamBuffer.BF_WRITE_OK;
                        vbufIdx = cnt;
//						System.out.println("a5. vbuf.state[" + cnt + "][WRIT]: "
//						s	+ vbuf.state[cnt]);
                        vbuf.len[cnt] = size;
                        cnt = (cnt + 1) % vbuf.maxBufSlot;
                    }
                }
            } catch (Exception e) {
//				System.err.println("a7. error IO");
                httpconn.disconnect();
                httpconn = null;
                vbuf.clearBuf();
                cnt = 0;
                httpparser.nonsafeSleep(3000 + ran.nextInt() % 2000);
                continue;
            }

        }
        if (httpconn != null)
            httpconn.disconnect();
        vbuf.clearBuf();
    }

    public void run() {
        Thread me = Thread.currentThread();
        if (me == thdVideoGet) {
            VideoGetThread();
        }
    }

    public void start() {
        Play();
    }

    private void Play() {
        if (bVideoGetRunning == false) {
            bVideoGetRunning = true;
            bVideoShowRunning = true;
            thdVideoGet = new Thread(this);
            thdVideoGet.start();
        }
    }

    public void stop() {

        Stop();
    }

    private void Stop() {
        bVideoGetRunning = false;
        bVideoShowRunning = false;
    }

}
