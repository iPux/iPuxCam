package org.camera.viewer.android.phoebemicro;

import java.io.BufferedInputStream;
import java.io.IOException;

public class HttpParser {

    private static final String BOUNDARY = "boundary=";
    public int bufAvailable;
    public int bufRead;

    public HttpParser() {
        bufAvailable = 0;
        bufRead = 0;
    }

    public void nonsafeSleep(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            System.err.println("interrupted error");
        }
    }

    private void checkBufIn(BufferedInputStream in) throws IOException {
        long now = System.currentTimeMillis();
        ;
        long last = System.currentTimeMillis();
        long delayTime = 0;
        while ((bufAvailable = in.available()) == 0) {
            nonsafeSleep(1);
            now = System.currentTimeMillis();
            delayTime = now - last;
            if (delayTime > 10000) {
                throw new IOException("timeout");
            }
        }
        last = now;
        bufRead = 0;
    }

    private int readBufIn(BufferedInputStream in) throws IOException {
        int got = -1;
        if (bufRead == bufAvailable) {
            checkBufIn(in);
        }
        got = in.read();
        bufRead = bufRead + 1;
        return got;
    }

    public String parseBoundary(String contentType) {
        if (contentType.indexOf("multipart/mixed") < 0)
            return new String("");
        int indexBoundary = contentType.indexOf(BOUNDARY);
        if (indexBoundary < 0)
            return new String("");

        return contentType.substring(indexBoundary + BOUNDARY.length());
    }

    public void seekToPattern(BufferedInputStream in, String pattern) throws IOException {
        int got;
        got = readBufIn(in);
        int match = 0;
        int count = 0;
        while (got != -1) {
            if (pattern.charAt(match) == got) {
                match++;
            } else {
                match = 0;
            }
            count++;
            if (match == pattern.length()) {
                //System.out.println("seek to pattern=" + count);
                return;
            }
            got = readBufIn(in);
        }
        throw new IOException("Boundary not complete");
    }

    public int getOneBoundary(BufferedInputStream in, String boundary, byte[] buffer, int maxBufferSize, byte[] hdrbuf) throws IOException {
        int index = 0;
        int count = 0;
        int match = 0;
        int got = 0;
        int tick = 0;
        int i = 0;

        seekToPattern(in, "\r\n\r\n");
        got = readBufIn(in);
        while (i < 28) {
            hdrbuf[i] = (byte) got;
            got = readBufIn(in);
            if (got == -1)
                return -1;
            i++;
        }
        while (got != -1) {
            buffer[index] = (byte) got;
            if (index >= maxBufferSize - 1) {
                System.err.println("buffer size overflow");
                index = 0;
                continue;
                // return -1;
            }
            if (boundary.charAt(match) == got) {
                match++;
                tick++;
            } else {
                match = 0;
                tick = 0;
            }
            count++;
            if (match == boundary.length()) {
                //System.out.println("a3.seek to boundary" + count);
                return (count - boundary.length());
            }
            index++;
            got = readBufIn(in);

        }

        if (got == -1)
            return -1;

        return index;
    }

}
