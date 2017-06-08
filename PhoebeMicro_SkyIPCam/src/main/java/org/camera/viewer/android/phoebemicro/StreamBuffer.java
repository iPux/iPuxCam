package org.camera.viewer.android.phoebemicro;

public class StreamBuffer {
    public static final int BF_FREE = 0;
    public static final int BF_IN_USE = 1;
    public static final int BF_WRITE_OK = 2;
    int maxBufSlot = 8;
    int maxBufSize = 200 * 1024;
    int headerSize = 28;
    public byte[][] header; // buffer storing the content
    public byte[][] buf; // buffer storing the content
    public int[] state;// state of the buffer
    public int[] len;// length of the content size
    public int totalframes;
    public int cnt;
    public int sec;
    public int usec;
    public int idx;

    public void clearBuf() {
        int i;
        for (i = 0; i < maxBufSlot; i++) {
            state[i] = StreamBuffer.BF_FREE;
            len[i] = 0;
            idx = 0;

        }
    }

    public StreamBuffer() {
        buf = new byte[maxBufSlot][maxBufSize];
        clearBuf();
    }

    public StreamBuffer(int slot, int size) {
        maxBufSlot = slot;
        maxBufSize = size;
        state = new int[slot];
        len = new int[slot];
        header = new byte[maxBufSlot][headerSize];
        buf = new byte[maxBufSlot][maxBufSize];
        clearBuf();
    }
}
