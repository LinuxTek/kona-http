/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.http;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

/*
This code is public domain: you are free to use, link and/or modify it in any way you want, for all purposes including commercial applications. 
*/

/**
 * KHttpUtil.
 */

public class KHttpUtil {
    private static Logger logger = Logger.getLogger(KHttpUtil.class);
 
    @SuppressWarnings("deprecation")
	public static DefaultHttpClient wrapDevSSL(HttpClient base) {
        DefaultHttpClient httpclient = null;

        try {
            SSLContext ctx = SSLContext.getInstance("TLS");

            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, 
                        String string) throws CertificateException {
                }
 
                public void checkServerTrusted(X509Certificate[] xcs, 
                        String string) throws CertificateException {
                }
 
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            X509HostnameVerifier verifier = new X509HostnameVerifier() {
                @Override
                public void verify(String string, SSLSocket ssls) 
                        throws IOException {
                }
 
                @Override
                public void verify(String string, X509Certificate xc) 
                        throws SSLException {
                }
 
                @Override
                public void verify(String string, String[] strings, 
                        String[] strings1) throws SSLException {
                }
 
                @Override
                public boolean verify(String string, SSLSession ssls) {
                    return true;
                }
            };

            ctx.init(null, new TrustManager[]{tm}, null);

            SSLSocketFactory ssf = new SSLSocketFactory(ctx);

            ssf.setHostnameVerifier(verifier);

            ClientConnectionManager ccm = base.getConnectionManager();

            SchemeRegistry sr = ccm.getSchemeRegistry();

            sr.register(new Scheme("https", ssf, 443));

            httpclient = new DefaultHttpClient(ccm, base.getParams());
        } catch (Exception e) {
            logger.error(e);
        }

        return httpclient;
    }
}
