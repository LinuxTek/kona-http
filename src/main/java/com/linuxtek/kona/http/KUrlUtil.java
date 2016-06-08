/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.http;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.log4j.Logger;

/**
 * KUrlUtil.
 */

public class KUrlUtil {
    private static Logger logger = Logger.getLogger(KUrlUtil.class);
 
    public static String encode(String url) 
    		throws MalformedURLException, URISyntaxException {
        URL u = new URL(url);
        String protocol = u.getProtocol();
        String host = u.getHost();
        Integer port = u.getPort();
        String path = u.getPath();
        String query = u.getQuery();
        String fragment = u.getRef();
        
        String authority = host;
        if (port >=0) {
        	authority += ":" + port;
        }
        
        // since we don't know if this url is already encoded or not
        // decode the path, query, and fragment first
		try {
            if (path != null) {
            	path = URLDecoder.decode(path, "UTF-8");
            }
            
            if (query != null) {
            	query = URLDecoder.decode(query, "UTF-8");
            }
            
            if (fragment != null) {
            	fragment = URLDecoder.decode(fragment, "UTF-8");
            }
		} catch (UnsupportedEncodingException e) {
            logger.error(e);
		}

        URI uri = new URI(protocol, authority, path, query, fragment);
        String s = uri.toASCIIString();
        logger.debug("encoded url: " + s);
        return s;
    }
}