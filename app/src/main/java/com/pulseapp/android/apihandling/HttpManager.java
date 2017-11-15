package com.pulseapp.android.apihandling;

import com.pulseapp.android.util.AppLibrary;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

/**
 * Created by ratan on 1/13/2015.
 */
class HttpManager {
    private static final String TAG;
    private static final DefaultHttpClient sClient;
    static {
        TAG = "HttpManager";
        // Set basic data
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpProtocolParams.setUserAgent(params, "androidv4");

        // Make pool
        ConnPerRoute connPerRoute = new ConnPerRouteBean(12);
        ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
        ConnManagerParams.setMaxTotalConnections(params, 12);

        // Set timeout
        HttpConnectionParams.setStaleCheckingEnabled(params, false);
        HttpConnectionParams.setConnectionTimeout(params, 3 * 1000);
        HttpConnectionParams.setSoTimeout(params, 3 * 1000);
        HttpConnectionParams.setSocketBufferSize(params, 8192);

        // Some client params
        HttpClientParams.setRedirecting(params, true);

        // Register http/s schemas!
        SchemeRegistry schReg = new SchemeRegistry();
        schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

        ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);
        sClient = new DefaultHttpClient(conMgr, params);
    }

    private HttpManager() {
    }

    public static InputStream postRequest(String urlString, List<NameValuePair> pairs) {
        try {
            //  1. modify url if required.
            HttpPost httpPost = new HttpPost(urlString);

            //  2. Add or Modify any headers
            //  Ex: httpPost.addHeader(new BasicHeader("X-Access-Token", prefs.getString("access_token", "")));

            //  3. Add any more post params if required
            //  pairs.add(new BasicNameValuePair("key", "value"));
            httpPost.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8"));

            long timeBeforeApiCall = System.currentTimeMillis();
            HttpResponse response = HttpManager.execute(httpPost);
            AppLibrary.log_i(TAG, "request url: " + urlString);
            AppLibrary.log_i(TAG, "request time: " + (System.currentTimeMillis() - timeBeforeApiCall));

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return AppLibrary.getStream(response);

            } else {
                AppLibrary.log_i(TAG, "Response Code: " + responseCode);
            }
        } catch (IOException e) {
            AppLibrary.error_log(TAG, "fetch failed, url: " + urlString, e);
        }
        return null;
    }

    public static InputStream getRequest(String urlString) {
        try {
            //  1. modify url if required.
            HttpGet httpGet = new HttpGet(urlString);

            //  2. Add or Modify any headers
            //  Ex: httpPost.addHeader(new BasicHeader("X-Access-Token", prefs.getString("access_token", "")));

            long timeBeforeApiCall = System.currentTimeMillis();
            HttpResponse response = HttpManager.execute(httpGet);
            AppLibrary.log_i(TAG, "request url: " + urlString);
            AppLibrary.log_i(TAG, "request time: " + (System.currentTimeMillis() - timeBeforeApiCall));

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = AppLibrary.getStream(response);
                return in;

            } else {
                AppLibrary.log_i(TAG, "Response Code: " + responseCode);
            }
        } catch (IOException e) {
            AppLibrary.error_log(TAG, "fetch failed, url: " + urlString, e);
        }
        return null;
    }

    private static HttpResponse execute(HttpHead head) throws IOException {
        return sClient.execute(head);
    }

    private static HttpResponse execute(HttpHost host, HttpGet get) throws IOException {
        return sClient.execute(host, get);
    }

    private static HttpResponse execute(HttpGet get) throws IOException {
        if (AppLibrary.USE_COMPRESSION)
            get.addHeader(new BasicHeader("Accept-Encoding", "gzip"));
        return sClient.execute(get);
    }

    private static HttpResponse execute(HttpPost post) throws IOException {
        if (AppLibrary.USE_COMPRESSION)
            post.addHeader(new BasicHeader("Accept-Encoding", "gzip"));
        return sClient.execute(post);
    }

    public static synchronized CookieStore getCookieStore() {
        return sClient.getCookieStore();
    }
}
