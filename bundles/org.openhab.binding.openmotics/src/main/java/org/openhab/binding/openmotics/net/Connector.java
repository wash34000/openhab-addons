package org.openhab.binding.openmotics.net;

import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Connector {
    private static final Logger logger;
    private static final Properties HTTP_HEADERS;
    private static final int DEFAULT_HTTP_REQUEST_TIMEOUT = 10000;
    protected static int httpRequestTimeout;
    private SSLSocketFactory sslSocketFactory;
    private HostnameVerifier hostNameVerifier;
    private final String baseUrl;

    static {
        logger = LoggerFactory.getLogger(Connector.class);
        Connector.httpRequestTimeout = DEFAULT_HTTP_REQUEST_TIMEOUT;
        (HTTP_HEADERS = new Properties()).put("Accept", "application/json");
    }

    public Connector(final String address) {
        this.baseUrl = "https://" + address;
        try {
            final SSLContext context = SSLContext.getInstance("TLS");
            context.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
            this.sslSocketFactory = context.getSocketFactory();
        } catch (Exception e) {
            // System.out.println("Cannot initialize SSLSocketFactory");
            return;
        }
        this.hostNameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(final String arg0, final SSLSession arg1) {
                return true;
            }
        };
    }

    public Boolean authenficationOk(final String message) {
        Boolean canConnect;
        try {
            final URL url = new URL(String.valueOf(this.baseUrl) + message);
            final HttpsURLConnection connection = this.openConnection(url);

            if (connection.getResponseCode() == 401) {
                canConnect = false;
            } else {
                canConnect = true;
            }

        } catch (Exception e) {
            canConnect = false;
        }
        return canConnect;
    }

    public synchronized JsonObject postAndGetResponse(final String message) {
        JsonObject result = null;
        try {
            final URL url = new URL(String.valueOf(this.baseUrl) + message);
            final HttpsURLConnection connection = this.openConnection(url);

            Boolean canConnect = true;

            // Teste si la connexion avec le token est authorisé
            if (connection.getResponseCode() == 401) {
                canConnect = false;

                result = new JsonObject();
                result.addProperty("success", false);
                result.addProperty("error", "token");
                Connector.logger.warn("Erreur d'authetification (" + url + ") : " + connection.getResponseCode() + " / " + connection.getResponseMessage());

            } else if (connection.getResponseCode() != 200 || connection.getInputStream() == null) {
                canConnect = false;

                result = new JsonObject();
                result.addProperty("success", false);
                result.addProperty("error", "server");
                Connector.logger.warn("Le serveur ne repond pas (" + url + ") : " + connection.getResponseCode() + " / " + connection.getResponseMessage());
            }

            // Si tout est OK
            if (canConnect) {
                final JsonParser parser = new JsonParser();
                final JsonElement je = parser.parse(new InputStreamReader(connection.getInputStream()));
                final JsonObject obj = je.getAsJsonObject();
                if (obj.has("success") && obj.get("success").getAsBoolean()) {
                    // obj.remove("success");
                    result = obj;
                } else {
                    // System.out.println(obj.get("msg").getAsString());
                }
            }

        } catch (Exception e) {
            // System.out.print("Method postAndGetResponse Exception : ");
            // System.out.println(e);
        }
        return result;
    }

    public synchronized int getResponseCode(final String message) {
        int result = -1;
        try {
            final URL url = new URL(String.valueOf(this.baseUrl) + message);
            final HttpsURLConnection connection = this.openConnection(url);
            result = connection.getResponseCode();
        } catch (Exception e) {
            // System.out.println(this.baseUrl + " : getResponseCode Exception" + e);
        }
        return result;
    }

    private HttpsURLConnection openConnection(final URL url) {
        HttpsURLConnection conn = null;
        try {
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setSSLSocketFactory(this.sslSocketFactory);
            conn.setHostnameVerifier(this.hostNameVerifier);
            conn.setConnectTimeout(Connector.httpRequestTimeout);
            for (final String key : Connector.HTTP_HEADERS.stringPropertyNames()) {
                conn.setRequestProperty(key, Connector.HTTP_HEADERS.getProperty(key));
            }
        } catch (Exception e) {
            // System.out.println("Method HttpsURLConnection Exception" + e);
        }
        return conn;
    }

    private static class DefaultTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // TODO Auto-generated method stub

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // TODO Auto-generated method stub

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] myTrustedAnchors = new X509Certificate[0];
            return myTrustedAnchors;
            // TODO Auto-generated method stub
            // return null;
        }
    }

}
