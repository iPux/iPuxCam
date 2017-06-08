package org.camera.viewer.android;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;

import android.os.Bundle;

public class HttpAudioInStream implements Runnable {
	
	public HttpParser httpparser;
	private Thread thdAudioGet;
	private String AccountCode = "";
	private boolean bAudioGetRunning;
	public boolean bAudioPlayRunning;
	
	public String RemoteHost;
	public String RemotePort;
	
	public StreamBuffer abuf;
	
	private final int MAX_BUFFER_SIZE = 8 * 1024;

	public HttpAudioInStream(Bundle connInfo) {
		init(connInfo);
		//start();
	}
	
	private void init(Bundle connInfo) {
		RemoteHost = connInfo.getString("host");
		RemotePort = connInfo.getString("port");
		String authCombination = connInfo.getString("username") + ":" + connInfo.getString("password");
		AccountCode = new String(Base64.encodeBase64(authCombination.getBytes()));
		abuf = new StreamBuffer(8, MAX_BUFFER_SIZE);// 8 slots, 8k each
		httpparser = new HttpParser();
		bAudioGetRunning = false;
		bAudioPlayRunning = false;
		
	}
	
	public void AudioGetThread() {
		HttpURLConnection httpconn = null;
		Random ran = new Random(System.currentTimeMillis());
		while (bAudioGetRunning == true) {

			URL myurl;

			int size = 0;
			int cnt = 0;
			//long tmp = 0;
			httpparser.bufAvailable = 0;
			httpparser.bufRead = 0;
			
			try {
				myurl = new URL("http",RemoteHost, Integer.parseInt(RemotePort), "/cgi/audio/audio.cgi?type=PCM");

				httpconn = (HttpURLConnection) myurl.openConnection();
				httpconn.setRequestProperty("Authorization", "Basic "+ AccountCode);
//				httpconn.addRequestProperty("", "\r\nAuthorization: Basic "+ AccountCode);
				httpconn.connect();
				BufferedInputStream in = new BufferedInputStream(httpconn
						.getInputStream());
				
				
				if (httpconn.getResponseCode() != HttpURLConnection.HTTP_OK) {
					System.err.println("response  = "
							+ httpconn.getResponseCode());
					continue;
				}
				String contentType = httpconn.getContentType();
				String boundary = httpparser.parseBoundary(contentType);
				if (boundary.length() == 0) {
					System.err.println("no boundary, exit");
					continue;
				}
				boundary = "--" + boundary;
				httpparser.seekToPattern(in, boundary);
				httpparser.seekToPattern(in, "\r\n\r\n");
				while (bAudioGetRunning == true) {
					while (abuf.state[cnt] == StreamBuffer.BF_IN_USE) {
						cnt = (cnt + 1) % abuf.maxBufSlot;
					}
					abuf.state[cnt] = StreamBuffer.BF_IN_USE;
					size = httpparser.getOneBoundary(in, boundary, abuf.buf[cnt], MAX_BUFFER_SIZE, abuf.header[cnt]);
					//System.err.println("a4. size = " + size);

					if (size <= 0) {
						throw new IOException("connection timeout");
					}else{
						// mark buf as "write OK"
						abuf.state[cnt] = StreamBuffer.BF_WRITE_OK;
						
						abuf.len[cnt] = size;
						cnt = (cnt + 1) % abuf.maxBufSlot;
					}
				}
			} catch (Exception e) {
				httpconn.disconnect();
				httpconn = null;
				abuf.clearBuf();
				cnt = 0;
				httpparser.nonsafeSleep(3000 + ran.nextInt() % 2000);
				continue;
			}

		}
		//System.err.println("Quit");
		if(httpconn!=null)
			httpconn.disconnect();
		abuf.clearBuf();
	}
	
	public void run() {
		Thread me = Thread.currentThread();
		if (me == thdAudioGet) {
			AudioGetThread();
		}
		
	}
	
	public int Play() {
		if (bAudioGetRunning == false) {
			bAudioGetRunning = true;
			bAudioPlayRunning = true;
			thdAudioGet = new Thread(this);
			thdAudioGet.start();
		}
		return 0;
	}
	
	private int Stop() {
		bAudioGetRunning = false;
		bAudioPlayRunning = false;
		return 0;
	}
	
	public void start() {
		Play();
	}
	
	public void stop() {
		Stop();
	}

}
