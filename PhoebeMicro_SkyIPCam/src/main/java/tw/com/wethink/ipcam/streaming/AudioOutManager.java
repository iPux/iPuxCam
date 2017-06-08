package tw.com.wethink.ipcam.streaming;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import java.io.IOException;

public class AudioOutManager extends AudioRecord {
    private AudioOutThread thread;
    private AudioOutOutputStream audioOutputStream = null;

    AudioTrack testAudio;
    static final int SAMPLE_INTERVAL = 20; // milliseconds
    static final int SAMPLE_SIZE = 2; // bytes per sample
    static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2;

    byte[] tempBuffer;

    private boolean isRunning = false;

    public class AudioOutThread extends Thread {

        public AudioOutThread() {
        }

        public void run() {
            int bytesRead;

            while (isRunning) {
                try {
                    synchronized (this) {
                        try {
                            bytesRead = read(tempBuffer, 0, tempBuffer.length);
                            System.out.println("[AudioOutManager] bytesRead: " + bytesRead);

                            if (audioOutputStream != null)
                                audioOutputStream.writeAudioStream(tempBuffer, bytesRead);
                        } catch (IOException e) {
                            System.out.println("E: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } finally {
                }
            }
        }
    }

    void calc1(byte[] lin, int off, int len) {
        int i, j;
        for (i = 0; i < len; i++) {
            j = lin[i + off];
            lin[i + off] = (byte) (j >> 2);
        }
    }

    public void init() {
        if (tempBuffer == null)
            tempBuffer = new byte[getMinBuffer()];
    }

    public void startRecording() {
        super.startRecording();

        audioOutputStream = AudioOutOutputStream.initSocket();
        audioOutputStream.writeHeader();

        if (thread != null)
            thread = null;

        thread = new AudioOutThread();
        thread.start();

        isRunning = true;
    }


    public void stopRecording() {
        super.stop();

        isRunning = false;

        if (audioOutputStream != null) {
            audioOutputStream.closeSocket();
            try {
                audioOutputStream.close();
                audioOutputStream = null;
            } catch (IOException e) {
            }
        }
    }

    public AudioOutManager() {
        super(MediaRecorder.AudioSource.DEFAULT, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, getMinBuffer());
        init();
    }

    public static int getMinBuffer() {
        int iMinBufSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        iMinBufSize = iMinBufSize > 3840 ? iMinBufSize : 3840;
        return iMinBufSize;
    }

}