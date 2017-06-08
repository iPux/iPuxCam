package org.camera.viewer.android.trendnet;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.IOException;

public class HttpAudioOutStream implements Runnable {

    private AudioRecord audioRecord = null;
    private int minRecordBufferSizeInBytes = 0;
    private Thread thdMicGet;
    private boolean bMicGetRunning;
    public boolean bMicSendRunning;
    public StreamBuffer tbuf;
    public byte[] hbuf;
    public HttpParser httpparser;

    private final int MAX_BUFFER_SIZE = 8 * 1024;
    private final int bufSize = 4000;


    public HttpAudioOutStream() {
        init();
        //start();
    }

    private void init() {
        tbuf = new StreamBuffer(8, MAX_BUFFER_SIZE);// 8 slots, 8k each
        hbuf = new byte[28];
        hbuf[4] = (byte) 0xa0;
        hbuf[5] = (byte) 0x0f;

        minRecordBufferSizeInBytes = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minRecordBufferSizeInBytes);

        httpparser = new HttpParser();
        bMicGetRunning = false;
        bMicSendRunning = false;

    }

    public void MicGetThread() {
        int cnt = 0;
        int size = 0;
        try {
            audioRecord.startRecording();
            while (bMicGetRunning == true) {
                while (tbuf.state[cnt] == StreamBuffer.BF_IN_USE) {
                    cnt = (cnt + 1) % tbuf.maxBufSlot;
                }
                tbuf.state[cnt] = StreamBuffer.BF_IN_USE;
                size = audioRecord.read(tbuf.buf[cnt], 0, bufSize);
                if (size <= 0) {
                    throw new IOException("connection timeout");
                } else {
                    tbuf.state[cnt] = StreamBuffer.BF_WRITE_OK;
                    tbuf.len[cnt] = size;
                    cnt = (cnt + 1) % tbuf.maxBufSlot;
                }
            }
            audioRecord.stop();
        } catch (Exception e) {
            e.printStackTrace();
            tbuf.clearBuf();
            cnt = 0;
        }
        tbuf.clearBuf();
    }

    public void start() {
        Play();
    }

    public void run() {
        Thread me = Thread.currentThread();
        if (me == thdMicGet) {
            MicGetThread();
        }
    }

    public int Play() {
        if (bMicGetRunning == false) {
            bMicGetRunning = true;
            bMicSendRunning = true;
            thdMicGet = new Thread(this);
            thdMicGet.start();
        }
        return 0;
    }

    public int Stop() {
        bMicGetRunning = false;
        bMicSendRunning = false;
        return 0;
    }

    public void stop() {
        Stop();
    }

    public void release() {
        audioRecord.release();
        audioRecord = null;
    }

}
