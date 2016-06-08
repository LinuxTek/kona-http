/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.http;


/**
 * KHttpClientException.
 */

@SuppressWarnings("serial")
public class KHttpClientException extends RuntimeException {
	private Integer statusCode = null;
    
    public KHttpClientException(Integer statusCode) {
        super();
        this.statusCode = statusCode;
    }
    
    public KHttpClientException(String message) {
        super(message);
    }
    
    public KHttpClientException(Integer statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public KHttpClientException(Integer statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public KHttpClientException(Throwable cause) {
        super(cause);
    }
    
    public KHttpClientException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
}
