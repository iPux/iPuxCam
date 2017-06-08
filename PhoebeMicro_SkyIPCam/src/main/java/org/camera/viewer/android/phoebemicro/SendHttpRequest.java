package org.camera.viewer.android.phoebemicro;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;


public class SendHttpRequest {

    private URL url;
    private String accountcode;
    public Vector param;

    public SendHttpRequest(URL myurl, String account) {
        url = myurl;
        accountcode = account;
        param = new Vector();
    }

    public int getConfig(String seekItem) {
        return sendRequest("list", seekItem);
    }

    public int getGroupConfig() {
        return sendRequest("listAll", "");
    }

    public int getUnicodeConfig() {
        return sendRequest("Unicode", "");
    }

    public int setConfig() {
        return sendRequest("update", "");
    }

    public int deleteImage() {
        return sendRequest("delete", "");
    }

    public int sendRequest(String action, String setItem) {
        HttpURLConnection httpconn = null;
        int returnCode = 0;
        String line = "";
        try {
            httpconn = (HttpURLConnection) url.openConnection();
            httpconn.setRequestProperty("Authorization", "Basic " + accountcode);
//			httpconn.addRequestProperty("", "\r\nAuthorization: Basic "+ accountcode);
            httpconn.setConnectTimeout(20000);
            httpconn.connect();

            BufferedReader rd = new BufferedReader(new InputStreamReader(httpconn.getInputStream()));
            returnCode = httpconn.getResponseCode();

            if (action.equals("list")) {
                while ((line = rd.readLine()) != null) {
                    if (line.startsWith(setItem)) {
                        String p[];
                        p = line.split("=");
                        Hashtable ht = new Hashtable();
                        if (line.endsWith("="))
                            ht.put(p[0], "");
                        else
                            ht.put(p[0], p[1]);
                        param.add(ht);
                    }
                }
            } else if (action.equals("listAll")) {
                while ((line = rd.readLine()) != null) {
                    if (line.indexOf("=") > 0) {
                        String p[];
                        p = line.split("=");
                        Hashtable ht = new Hashtable();
                        if (line.endsWith("="))
                            ht.put(p[0], "");
                        else
                            ht.put(p[0], p[1]);
                        param.add(ht);
                    }
                }
            } else if (action.equals("Unicode")) {
                while ((line = rd.readLine()) != null) {
                    if (line.indexOf("=") > 0) {
                        String p[];
                        p = line.split("=");
                        Hashtable ht = new Hashtable();
                        if (line.endsWith("="))
                            ht.put(p[0], "");
                        else
                            ht.put(p[0], p[1]);
                        param.add(ht);
                    }
                }
            } else if (action.equals("update")) {

            } else if (action.equals("delete")) {
                while ((line = rd.readLine()) != null) {
                    if (line.indexOf("200") > 0) {
                        returnCode = 200;
                        break;
                    }
                }
            }
            rd.close();
            httpconn.disconnect();
            httpconn = null;
        } catch (Exception e) {
            httpconn.disconnect();
            httpconn = null;
        }
        return returnCode;
    }

    public int sendMultipartRequest(String boundary, byte[] mContent) {
        HttpURLConnection httpconn = null;
        DataOutputStream dos = null;
        int returnCode = 0;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        try {
            httpconn = (HttpURLConnection) url.openConnection();
            httpconn.setRequestProperty("Authorization", "Basic " + accountcode);
            httpconn.setDoInput(true);
            httpconn.setDoOutput(true);
            httpconn.setRequestMethod("POST");
            httpconn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            httpconn.setRequestProperty("Content-Length", String.valueOf(mContent.length));
            dos = new DataOutputStream(httpconn.getOutputStream());
            httpconn.connect();

            dos.writeBytes(lineEnd + twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"yuvfile\";" + "filename=\"osd.yuv\"" + lineEnd);
            dos.writeBytes("Content-Type: application/octet-stream" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.write(mContent, 0, mContent.length);
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.flush();
            dos.close();
            BufferedReader rd = new BufferedReader(new InputStreamReader(httpconn.getInputStream()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                if (line.indexOf("200") > 0) {
                    returnCode = 200;
                    break;
                }
            }
            httpconn.disconnect();
            httpconn = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnCode;
    }
}
