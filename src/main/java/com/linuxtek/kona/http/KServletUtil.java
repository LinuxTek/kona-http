/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import com.linuxtek.kona.encryption.KEncryptUtil;
import com.linuxtek.kona.templates.KTemplate;
import com.linuxtek.kona.templates.KTemplateException;
import com.linuxtek.kona.util.KDateUtil;

/*
import com.linuxtek.kona.entity.KToken;
import com.linuxtek.kona.service.KAuthService;
import com.linuxtek.kona.service.KServiceClient;
*/



public class KServletUtil {
    private static Logger logger = Logger.getLogger(KServletUtil.class);
    
    public static String DEFAULT_ACCESS_TOKEN_KEY =
            "com.linuxtek.kona.security.ACCESS_TOKEN";
    
    public static String DEFAULT_CLIENT_ID_HEADER_KEY = "X-CLIENT-ID";
    
    private static final String PIXEL_1x1_GIF_B64  = "R0lGODlhAQABAPAAAAAAAAAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==";

    private static String accessTokenKey = null;
    
    public static void serveTransparentPixel(HttpServletResponse resp) throws IOException {
    	byte[] data = Base64.decodeBase64(PIXEL_1x1_GIF_B64.getBytes());
        String contentType = "image/gif";
    	writeObject(resp, contentType, data, null, false);
    }

    public static String getAccessTokenKey() {
        if (accessTokenKey == null) {
            return DEFAULT_ACCESS_TOKEN_KEY;
        }
        return accessTokenKey;
    }
    
    public static void setAccessTokenKey(String s) {
        accessTokenKey = s;
    }
    
    /*
    public static KToken getToken(KAuthService<KToken> authService,
            HttpServletRequest req) {

        if (authService == null) {
            logger.info("KAuthService is null. Returning null token");
            return null;
        }

        KToken token = null;
        String accessToken = getAccessToken(req);
        //logger.debug("accessToken: " + accessToken);
        if (accessToken != null) {
            token = authService.getToken(accessToken);
            //logger.debug("token:\n" + KClassUtil.toString(token));
            if (token != null && !authService.isTokenValid(token)) {
                token = null;
                //logger.debug("token set to null after isTokenValid check");
            }
        }
        return (token);
    }
    */

    public static String getAccessToken(HttpServletRequest req) {
        return getAccessToken(req, null);
    }
    
    public static String getAccessToken(HttpServletRequest req, String tokenKey) {
        String accessToken = null;
        
        if (req == null) {
            logger.info("HttpServletRequest is null. Returning null AccessToken");
            return null;
        }
        
        KForm form = new KForm(req);
        
        if (tokenKey == null) {
        	tokenKey = getAccessTokenKey();
        }
        
        // check if acccess_token provided as param value
        accessToken = form.getString("access_token");

        // first see if we have a token stored in a session
        //HttpSession session = req.getSession(true);
        if (accessToken == null) {
        	HttpSession session = getSession(req, false);
        	if (session != null) {
        		accessToken = (String) session.getAttribute(tokenKey);
        	}
        }

        // if not, check for a cookie
        if (accessToken == null) {
            accessToken = getCookie(req, tokenKey);
        }
        
        // check auth header (basic)
        if (accessToken == null) {
            String[] creds = getBasicAuthCreds(req);
            if (creds != null) {
            	accessToken = creds[0];
            }
        }
        
        // check auth header (bearer)
        if (accessToken == null) {
        	try {
        		accessToken = getBearerToken(req);
        	} catch (Exception e) {
        		logger.error("Error fetching bearer token", e);
        	}
        }

        return accessToken;
    }
    
    public static String getClientId(HttpServletRequest req) {
    	return getClientId(req, null);
    }
    
    public static String getClientId(HttpServletRequest req, String clientIdHeaderKey) {
        KForm form = new KForm(req);
        
        if (clientIdHeaderKey == null) {
        	clientIdHeaderKey = DEFAULT_CLIENT_ID_HEADER_KEY;
        }
        
        String clientId = form.getString("client_id");
        
        if (clientId == null) {
        	clientId = req.getHeader(clientIdHeaderKey);
        }
        
        return clientId;
    }
    
    
    public static String[] getBasicAuthCreds(HttpServletRequest req) {
        //logger.debug("getBasicAuthCreds called");
        String[] creds = null;
    	final String authorization = req.getHeader("Authorization");
    	if (authorization != null && authorization.startsWith("Basic")) {
    		// Authorization: Basic base64credentials
    		String base64Credentials = authorization.substring("Basic".length()).trim();
    		//logger.debug("found Basic Auth in header: " + base64Credentials);
    		String credentials = new String(Base64.decodeBase64(base64Credentials.getBytes()),
    					Charset.forName("UTF-8"));
    		// credentials = username:password
    		creds = credentials.split(":",2);
    		//logger.debug("decoded Basic Auth username: " + creds[0]);
    	}
        return creds;
    }
    
    public static String getBearerToken(HttpServletRequest request) 
    		throws IOException, ServletException {
        //logger.debug("Looking for bearer token in Authorization header, query string or Form-Encoded body parameter");
        String tokenValue = null;
        
        String auth = request.getHeader("Authorization");
        String query = request.getQueryString();
        Map<String,String[]> params = request.getParameterMap();
        if (auth == null) return null;

        if (auth.startsWith("Bearer")) {
            //logger.debug("Found bearer token in Authorization header");
            tokenValue = auth.substring(7);
        } else if (isFormEncoded(request) && request.getParts().size() <= 1 && request.getMethod() != "GET") {
            //logger.debug("Found bearer token in request body");
            tokenValue = params.get("access_token")[0]; 
        } else if (query != null && query.contains("access_token")) {
            //logger.debug("Found bearer token in query string");
            tokenValue = request.getParameter("access_token");
        } else {
            //logger.debug("No token found");
        }
        return tokenValue;
    }


    public static String dumpRequestHeaders(HttpServletRequest req) {
        StringBuffer sb = new StringBuffer();
        Enumeration<?> e = req.getHeaderNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            sb.append(name + ": " + req.getHeader(name) + "\n");
        }

        return sb.toString();
    }

    public static String dumpSessionParams(HttpSession session) {
        StringBuffer sb = new StringBuffer();
        Enumeration<?> e = session.getAttributeNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            sb.append(name + ": " + session.getAttribute(name) + "\n");
        }
        return sb.toString();
    }
    
    public static void writeHtml(HttpServletResponse resp, String html) throws IOException {
    	resp.setContentType("text/html");  
        resp.getWriter().println(html);
    }
    
    public static void writeTemplate(HttpServletResponse resp, String templateName)
    				throws IOException, KTemplateException {
    	writeTemplate(resp, templateName, null, null);
    }
    
    public static void writeTemplate(HttpServletResponse resp, String templateName,
    		String contentType, Map<String,Object> contextMap) throws IOException, KTemplateException {
        if (contentType == null) {
            contentType = "text/html";
        }
    	KTemplate t = new KTemplate(templateName);
        String s = null;
        
        if (contextMap != null) {
        	t.addContextMap(contextMap);
        }
        
        if (contentType == "text/html") {
        	s = t.toHtml();
        } else {
        	s = t.toString();
        }
        
    	resp.setContentType(contentType);  
        resp.getWriter().println(s);
    }


    public static void writeObject(HttpServletResponse resp,
            String contentType, byte[] data, String filename,
            boolean cacheControlEnabled) throws IOException {

        logger.debug("writeObject(): filename: " + filename);

        Date lastModified = new Date();
        int maxAge = 3600;

        if (contentType == null) {
            throw new IllegalArgumentException("ContentType is null");
        }

        if (data == null) {
            throw new IllegalArgumentException("Data is null");
        }

        String md5 = null;
        try { 
            md5 = KEncryptUtil.MD5(data); 
        } catch (Exception e) { logger.error(e); }

        resp.addHeader("Content-Length", Integer.toString(data.length));
        resp.addHeader("Last-Modified", KDateUtil.formatHttp(lastModified));

        if (md5 != null) {
            resp.addHeader("ETag", "\"" + md5 + "\"");
        }

        if (cacheControlEnabled) {
            resp.addHeader("Cache-Control",
                "public, must-revalidate, max-age=" + maxAge);
        } else {
            resp.addHeader("Cache-Control",
                "no-cache, no-store, must-revalidate");
            resp.setHeader("Pragma", "No-cache");
            resp.setDateHeader("Expires", 0);
        }

        if (filename != null) {
            String value = "attachment; filename=" + filename;
            resp.addHeader("Content-Disposition", value);
            
            //logger.debug("Content-Disposition value: " + value);
        }

        ServletOutputStream out = resp.getOutputStream();
        resp.setContentType(contentType);
        out.write(data);
        out.close();
    }

    public static String getFullRequestURL(HttpServletRequest req) {
        if (req == null) return null;

        //String reqURL = req.getRequestURL().toString();
        // NOTE: during development (using GWT hosted mode), the jetty
        // server would occasionally and spuriously throw a 
        // NullPointerException when call getRequestURL().  It's not
        // clear why this is happening or if this would occur in
        // script mode.
        StringBuffer sb = null;
        try {
            sb = req.getRequestURL();
        } catch (Exception e) {
            logger.error(e);
            return null;
        }
        String reqURL = sb.toString();

        String contextPath = req.getContextPath();

        if (contextPath != null && contextPath.length() > 0 
                && !contextPath.equals("/")
                && reqURL.endsWith(contextPath)) {
            reqURL += "/";
        }

        String queryString = req.getQueryString();

        if (queryString != null) {
            reqURL += "?" + queryString;
        }
        return (reqURL);
    }


    // get or create session
    public static HttpSession getSession(HttpServletRequest req) {
        	return getSession(req, true);
    }
    
    public static HttpSession getSession(HttpServletRequest req, boolean create) {
        if (req == null) return null;

        try {
            ServletContext context = req.getServletContext();
            SessionCookieConfig config = context.getSessionCookieConfig();
            if (config != null) {
                config.setPath("/");
                config.setDomain(getCookieDomain(req));
            }
        } catch (Throwable t) {
            logger.warn("Unable to set SessionCookieConfig for request: "
            		+ getFullRequestURL(req));
        }

        HttpSession session = req.getSession(create);

        if (session != null) {
            session.setMaxInactiveInterval(-1);
            //logger.debug("getSession() sessionId: " + session.getId()
             //   + "\nrequestURL: " + getFullRequestURL(req));
        } else {
            logger.info("req.getSession(true) returned null."
                +"\nrequestURL: " + getFullRequestURL(req));
        }

        return session;
    }
    
    public static String getSessionId(HttpServletRequest req) {
        HttpSession session = getSession(req);
        if (session == null) return null;
        return session.getId();
    }

    public static String getCookieDomain(HttpServletRequest req) {
    	String domain = null;
    	if (req != null) {
    		domain = req.getServerName();
    		String[] s = domain.split("\\.");
    		int len = s.length;
    		if (len >= 2 && !isNumeric(s[len-1])) {
    			domain = "." + s[len-2] + "." + s[len-1];
    		}
    	}

    	return domain;
    }
    
    private static boolean isNumeric(String number) {
        boolean isValid = false;

        if (number == null)
            return (false);

        /*
         * Number: A numeric value will have following format:
         *
         * ^[-+]? : Starts with an optional "+" or "-" sign. [0-9]* : May have
         * one or more digits. \\.? : May contain an optional "." (decimal
         * point) character. [0-9]+$ : ends with numeric digit.
         *
         */

        String regex = "((-|\\+)?[0-9]+(\\.[0-9]+)?)+";
        //String regex = "^[-+]?[0-9]*\\.?[0-9]+$";
        CharSequence inputStr = number;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputStr);

        if (matcher.matches())
            isValid = true;

        return (isValid);
    }

    public static void removeCookie(HttpServletRequest req, 
            HttpServletResponse resp, String name) {
        if (req != null) {
        	String domain = getCookieDomain(req);
            Cookie[] cookies = req.getCookies();
            if (cookies == null) return;
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    
                    //logger.debug("removeCookie: orig cookie" 
                    //		+ KClassUtil.toJson(cookie));
                    
                    cookie.setValue(null);
                    cookie.setMaxAge(0);
                    
                    if (cookie.getDomain() == null) {
                    	cookie.setDomain(domain);
                    }
                    
                    if (cookie.getPath() == null) {
                    	cookie.setPath("/");
                    }
                    
                    if (resp != null) {
                    	//logger.debug("removeCookie: updated cookie" 
                    	//		+ KClassUtil.toJson(cookie));
                        resp.addCookie(cookie);
                    }
                }
            }
        }
    }

    public static Cookie addCookie(HttpServletRequest req, 
    		HttpServletResponse resp, String name, String value) {
        String domain = getCookieDomain(req);
        return addCookie(req, resp, name, value, domain);
    }

    public static Cookie addCookie(HttpServletRequest req, 
    		HttpServletResponse resp, String name, String value, 
    		String domain) {
        // first remove all instances of this cookie
        removeCookie(req, resp, name);

        if (resp == null) {
            logger.warn("HttpServletResponse is null; cannot set cookie: [" 
            		+ name +"] to value: " + value);
        	return null;
        }


        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setDomain(domain);
        
        //logger.debug("adding cookie: " + KClassUtil.toString(cookie));
        resp.addCookie(cookie);
        return cookie;
    }

    public static String getCookie(HttpServletRequest req, String name) {
        if (req == null) return null;
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static void sendResponse(HttpServletResponse resp,
            String contentType, String content) throws IOException {
        //logger.debug(content);
        resp.addHeader("Cache-Control", "private,no-cache,no-store");
        resp.setContentType(contentType);
        resp.setCharacterEncoding("ISO-8859-1");
        PrintWriter writer = resp.getWriter();
        writer.print(content);
        writer.flush();
    }
    
    public static String getClientHostname(HttpServletRequest req) {
        if (req == null) {
            return "LOCAL";            
        }
        return req.getRemoteHost();
    }

    public static String getClientBrowser(HttpServletRequest req) {
        if (req == null) {
            return "LOCAL";            
        }
        return req.getHeader("User-Agent");
    }

    public static Double getClientLatitude(HttpServletRequest req) {
        if (req == null) return null;
        String s = req.getHeader("X-Kona-Geo-Lat");
        if (s == null) return null;
        return Double.valueOf(s);
    }
    
    public static Double getClientLongitude(HttpServletRequest req) {
        if (req == null) return null;
        String s = req.getHeader("X-Kona-Geo-Lng");
        if (s == null) return null;
        return Double.valueOf(s);
    }
    
    public static String getClientLocale(HttpServletRequest req) {
        if (req == null) return null;
        return req.getLocale().toString();
    }
    
    public static String getClientReferer(HttpServletRequest req) {
        if (req == null) return null;
        return req.getHeader("referer");
    }
    

    private static boolean isFormEncoded(HttpServletRequest req) {
        return ServletFileUpload.isMultipartContent(req);
    }
}
