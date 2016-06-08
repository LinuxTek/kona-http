/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;


/**
 * KHttpClientRequest.
 */

public class KHttpClientRequest {
    private static Logger logger = Logger.getLogger(KHttpClientRequest.class);

    private String baseUrl = null;
    private DefaultHttpClient httpclient = null;
    private Map<String, String> headers = null;

    public KHttpClientRequest(String baseUrl) throws KHttpClientException {
        this(baseUrl, null, null, false);
    }

    public KHttpClientRequest(String baseUrl, 
            String username, String password) throws KHttpClientException {
        this(baseUrl, username, password, false);
    }

    public KHttpClientRequest(String baseUrl, String username, 
            String password, boolean ignoreSSLCertWarning) 
            throws KHttpClientException {
        this.baseUrl = baseUrl;

        httpclient = new DefaultHttpClient();
        headers = new HashMap<String,String>();

        if (username != null && password != null) {
            setCredentials(username, password, ignoreSSLCertWarning);
        }
    }
    
    public DefaultHttpClient getHttpClient() {
        return httpclient;
    }
    
    public void addHeader(String name, String value) {
    	headers.put(name,  value);
    }
    
    public void removeHeader(String name) {
    	headers.remove(name);
    }
    
    public void clearHeaders() {
        headers = new HashMap<String,String>();
    }

    private void setCredentials(String username, String password, 
            boolean ignoreSSLCertWarning) throws KHttpClientException {
        try {
            logger.debug("Setting credentials: " 
                    + "\nusername: " + username
                    + "\npassword: " + password);

            UsernamePasswordCredentials defaultcreds = 
                new UsernamePasswordCredentials(username, password);

            URL url = new URL(baseUrl);

            String protocol = url.getProtocol();
            logger.debug("protocol: " + protocol);

            String host = url.getHost();

            int port = url.getPort(); // will return -1 if not set
            if (port == -1) {
                port = url.getDefaultPort(); // will return -1 if unknown
            }

            if (protocol != null && protocol.equals("https") 
                    && ignoreSSLCertWarning) {
                logger.debug("setting wrapDevSSL ...");
                httpclient = KHttpUtil.wrapDevSSL(httpclient);
            }

            AuthScope authScope = new AuthScope(host,port,AuthScope.ANY_REALM);

            logger.debug("AuthScope: " + authScope);
            logger.debug("Credentials: " + defaultcreds);

            httpclient.getCredentialsProvider().setCredentials(
                    authScope, defaultcreds);

        } catch (Exception e) {
            throw new KHttpClientException(e);
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    protected String encode(String s) {
        String result = s;
        try {
            result = URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            logger.error(e);
        }
        return result;
    }
    
    public String get(String path, Map<String,Object> params) 
            throws KHttpClientException {
        String serviceParams = null;
        
        if (params != null && params.size() > 0) {
        	StringBuffer sb = new StringBuffer();
        	Iterator<String> it = params.keySet().iterator();
        	while (it.hasNext()) {
        		String key = it.next();
        		Object value = params.get(key);
        		sb.append(key);
                sb.append("=");
                if (value == null) {
                	sb.append("null");
                } else {
                	sb.append(encode(value.toString()));
                }
                sb.append("&");
        	}
            serviceParams = sb.substring(0, (sb.length()-1));
        }
        return get(path, serviceParams);
    }
    
    public String doRequest(String servicePath, 
            String serviceParams) throws KHttpClientException {
        return get(servicePath, serviceParams);        
    }

    public String get (String servicePath, 
            String serviceParams) throws KHttpClientException {
        logger.debug("get called ...");

        // NOTE: If you want to use SSL-specific features,change to:
        //HttpsURLConnection conn = 
        //      (HttpsURLConnection) url.openConnection();

        String url = baseUrl + servicePath;
        if (serviceParams != null) {
        	url += "?" + serviceParams;
        }

        logger.debug("request URL: " + url);

        try {
            HttpGet httpget = new HttpGet(url);
            if (headers != null) {
                for (String key : headers.keySet()) {
                	httpget.addHeader(key, headers.get(key));
                }
            }
            
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StringBuffer content = null;

            if (entity != null) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(entity.getContent()));
        
                content = new StringBuffer();
                String line = in.readLine();
                while (line != null) { 
                    content.append(line + "\n");
                    line = in.readLine();
                }
        
                in.close();
            }

            String result = (content == null ? null : content.toString());

            Integer statusCode = response.getStatusLine().getStatusCode();
            logger.debug("response code: " + statusCode);
            logger.debug("response content: " + result);

            if (statusCode != 200) {
                throw new KHttpClientException(statusCode, result);
            }

            return (result);
        } catch (IOException e) {
            throw new KHttpClientException(e);
        }
    }

    public String post(String servicePath, 
            Map<String,Object> serviceParams) throws KHttpClientException {
        logger.debug("post called ...");

        // NOTE: If you want to use SSL-specific features,change to:
        //HttpsURLConnection conn = 
        //      (HttpsURLConnection) url.openConnection();

        String url = baseUrl + servicePath;

        try {
            HttpPost httppost = new HttpPost(url);
            
            HttpEntity postEntity = createEntity(serviceParams);
            httppost.setEntity(postEntity);

            if (headers != null) {
                for (String key : headers.keySet()) {
                	httppost.addHeader(key, headers.get(key));
                }
            }
            
            
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            
            StringBuffer content = null;

            if (entity != null) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(entity.getContent()));

                content = new StringBuffer();
                String line = in.readLine();
                while (line != null) {
                    content.append(line + "\n");
                    line = in.readLine();
                }

                in.close();
            }

            String result = (content == null ? null : content.toString());

            Integer statusCode = response.getStatusLine().getStatusCode();
            logger.debug("response code: " + statusCode);
            logger.debug("response content: " + result);

            if (statusCode != 200) {
                throw new KHttpClientException(statusCode, result);
            }

            return (result);
        } catch (IOException e) {
            throw new KHttpClientException(e);
        }
    }

    protected HttpEntity createEntity(Map<String,Object> map) 
            throws KHttpClientException {

        try {
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            
            if (map != null && map.size() > 0) {
            	Iterator<String> it = map.keySet().iterator();

            	while (it.hasNext()) {
            		String key = it.next();
            		Object value = map.get(key);
            		if (value != null) {
            			formparams.add(
            					new BasicNameValuePair(key, value.toString()));
            		}
            	}
            }

            UrlEncodedFormEntity entity = 
                new UrlEncodedFormEntity(formparams, "UTF-8");
         
            return entity;
        } catch (UnsupportedEncodingException e) {
            throw new KHttpClientException(e);
        }
    }
}
