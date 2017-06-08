package tw.com.wethink.ipcam.streaming;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class AudioOutOutputStream extends DataOutputStream {

    static String myHost;
    static int myPort = -1;
    static String myUsername;
    static String myPassword;
    static Socket audioOutSocket = null;
    static String header;
    private final static String CONTENT_LENGTH = "Content-Length";

    private static final String myboundary = "--myboundary\r\n";
    private static final String contentType = "Content-Type: multipart/mixed;boundary=myboundary\r\n\r\n";

    private AudioOutOutputStream(OutputStream out) {
        super(new BufferedOutputStream(out));
    }

    public static void initData(String host, int port, String username, String password) {
        myHost = host;
        myPort = port;
        myUsername = username;
        myPassword = password;
        prepareHeader();
    }

    public static AudioOutOutputStream initSocket() {
        if (myHost == null || myPort <= 0 || myUsername == null || myPassword == null)
            return null;

        try {
            if (audioOutSocket != null) {
                if (!audioOutSocket.isClosed())
                    audioOutSocket.close();
                audioOutSocket = null;
            }
            audioOutSocket = new Socket(myHost, myPort);

            return new AudioOutOutputStream(audioOutSocket.getOutputStream());
        } catch (UnknownHostException e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void closeSocket() {
        if (audioOutSocket != null) {
            try {
                if (!audioOutSocket.isClosed())
                    audioOutSocket.close();
            } catch (IOException e) {
            }
            audioOutSocket = null;
        }
    }

    private static void prepareHeader() {
        String authCombination = myUsername + ":" + myPassword;
        String accountCode = Base64.encodeToString(authCombination.getBytes(), Base64.DEFAULT);

        if (header == null) {
            header = new String();
            header = header + "POST " + "/cgi/audio/audio.cgi?type=PCM" + " HTTP/1.1\r\n";
            header = header + "Authorization: Basic " + accountCode + "\r\n";
            header = header + contentType;
        }
    }

    public void writeHeader() {
        try {
            write(header.getBytes());
        } catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    public void writeAudioStream(byte[] audioStream, int size) throws IOException {
        write(myboundary.getBytes(), 0, myboundary.length());

        byte[] payLoadHeader = new byte[28];
        payLoadHeader[0] = -17;
        payLoadHeader[1] = 41;
        payLoadHeader[2] = (byte) 0x00;
        payLoadHeader[3] = (byte) 0x00;
        payLoadHeader[4] = (byte) 0x00;
        payLoadHeader[5] = (byte) 0x0f;
        payLoadHeader[6] = (byte) 0x00;
        payLoadHeader[7] = (byte) 0x00;
        payLoadHeader[8] = (byte) 'A';

        String contentLength = CONTENT_LENGTH + ": " + (payLoadHeader.length + size) + "\r\n\r\n";
        write(contentLength.getBytes(), 0, contentLength.length());
        write(payLoadHeader, 0, payLoadHeader.length);
        write(audioStream, 0, size);
    }

}