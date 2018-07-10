package discomics.application;

import javafx.scene.image.Image;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jure on 4.9.2016.
 */
public class DownloadFileHTTP {

    private static final int DOWNLOAD_RETRIES_TIMEOUT = 10;
    private static final int DOWNLOAD_RETRY_DELAY_MS = 2000;

    private static final String CLASS_NAME = DownloadFileHTTP.class.getName();

    public static String downloadOnlineFileNoHeader(String urlInput) throws SocketException {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        MyLogger.entering(CLASS_NAME, methodName);

        HttpClient client = HttpClients.custom().build();
        HttpUriRequest request = RequestBuilder.get().setUri(urlInput).build();

        InputStream is = null;

        for (int i = 0; i < DOWNLOAD_RETRIES_TIMEOUT; i++) { // retry 30 times, 2 second interval in between
            try {
                is = client.execute(request).getEntity().getContent();
                return download(is);
            } catch (IOException e) {
                try {
                    System.out.println("RETRY " + i);
                    Thread.sleep(DOWNLOAD_RETRY_DELAY_MS);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        throw new SocketException(); // if info cant be retrieved after 30 retries, throw SocketException
    }

    public static Image downloadOnlineImage(String url) throws SocketException {
        HttpClient client = HttpClients.custom().build();
        HttpUriRequest request = RequestBuilder.get().setUri(url).build();

        InputStream is = null;

        for (int i = 0; i < DOWNLOAD_RETRIES_TIMEOUT; i++) {
            try {
                is = client.execute(request).getEntity().getContent();
                Image image = new Image(is);
                is.close();
                return image;
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    System.out.println("RETRY: " + i);
                    Thread.sleep(DOWNLOAD_RETRY_DELAY_MS);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        throw new SocketException();
    }

    public static String downloadOnlineFileWithHeader(String urlInput) throws SocketException {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        MyLogger.entering(CLASS_NAME, methodName);

        HttpClient client = HttpClients.custom().setConnectionTimeToLive(30, TimeUnit.SECONDS).build();
        HttpUriRequest request = RequestBuilder.get().setUri(urlInput)
                .setHeader("Accept", "application/json").build();

        InputStream is = null;
        for (int i = 0; i < DOWNLOAD_RETRIES_TIMEOUT; i++) {
            try {
                is = client.execute(request).getEntity().getContent();
                return download(is);
            } catch (IOException e) {
                try {
                    System.out.println("RETRY: " + i);
                    Thread.sleep(DOWNLOAD_RETRY_DELAY_MS);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        throw new SocketException();
    }

    private static String download(InputStream is) throws IOException {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        MyLogger.entering(CLASS_NAME, methodName);

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        int cp;

        while ((cp = br.read()) != -1) {
            sb.append((char) cp);
        }
        br.close();
        is.close();
        return sb.toString();
    }
}
