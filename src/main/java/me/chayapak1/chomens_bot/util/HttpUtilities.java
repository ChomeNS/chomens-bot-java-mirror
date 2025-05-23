package me.chayapak1.chomens_bot.util;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

// Author: _ChipMC_ (well I added some stuff)
public class HttpUtilities {
    // ig duplicate codes yup real

    public static String getRequest (final URL url) throws NoSuchAlgorithmException, KeyManagementException, IOException {
        final SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[] { new DownloadUtilities.DefaultTrustManager() }, new SecureRandom());
        SSLContext.setDefault(ctx);

        final URLConnection conn = url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        // https://www.whatismybrowser.com/guides/the-latest-user-agent/windows
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36");

        final BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        final StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        return content.toString();
    }

    public static String postRequest (final URL url, final String contentType, final String requestBody) throws Exception {
        final SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[] { new DownloadUtilities.DefaultTrustManager() }, new SecureRandom());
        SSLContext.setDefault(ctx);

        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", contentType);

        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        // https://www.whatismybrowser.com/guides/the-latest-user-agent/windows
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36");

        final OutputStream os = conn.getOutputStream();
        os.write(requestBody.getBytes());
        os.flush();

        final BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        final StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        return content.toString();
    }
}
