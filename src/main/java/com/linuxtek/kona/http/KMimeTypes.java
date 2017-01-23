/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.http;
import java.util.Map;

import org.apache.log4j.Logger;

/*
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatch;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;
*/



import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeTypes;

import com.linuxtek.kona.util.KFileUtil;

import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class KMimeTypes {
	private static Logger logger = Logger.getLogger(KMimeTypes.class);
	
    private static Map<String,List<String>> mimeTypeMap = new HashMap<String,List<String>>();

	public static final String[] FORMAT_NAMES = { "jpeg", "gif", "png", "bmp",
			"pcx", "iff", "ras", "pbm", "pgm", "ppm", "psd", "swf" };

    public static final String[] IMAGE_MIME_TYPES= { 
    		"image/jpeg",
    		"image/pjpeg",
    		"image/gif", 
    		"image/png", 
    		"image/bmp", 
    		"image/pcx", 
    		"image/tiff",
    		"image/ras", 
    		"image/x-portable-bitmap", 
    		"image/x-portable-graymap",
    		"image/x-portable-pixmap", 
    		"image/psd",
            "image/x-xcf"
    };
    
    public static final String[] AUDIO_MIME_TYPES = {
    		"audio/amr",
            "audio/3gpp",
            "audio/3gpp2",
    		"audio/basic",
    		"audio/L24",
    		"audio/mp4",
    		"audio/mpeg",
    		"audio/ogg",
    		"audio/vorbis",
    		"audio/vnd.rn-realaudio",
    		"audio/vnd.wav",
    		"audio/wav",
    		"audio/x-wav",
    		"audio/webm",
    		"audio/x-aac",
    		"audio/x-caf"
    		
    };
    
    public static final String[] VIDEO_MIME_TYPES = {
    		"video/mpeg",
    		"video/mp4",
    		"video/ogg",
    		"video/quicktime",
    		"video/webm",
    		"video/x-matroska",
            "application/x-matroska",
    		"video/x-ms-wmv",
    		"video/x-flv"
    };
    
    public static final String[] DOCUMENT_MIME_TYPES = {
    		"application/vnd.oasis.opendocument.text",
    		"application/vnd.oasis.opendocument.spreadsheet",
    		"application/vnd.oasis.opendocument.presentation",
    		"application/vnd.oasis.opendocument.graphics",
    		"application/vnd.ms-excel",
    		"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    		"application/vnd.ms-powerpoint",
    		"application/vnd.openxmlformats-officedocument.presentationml.presentation",
    		"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    		"application/vnd.ms-xpsdocument",
    		"application/pdf",
    		"application/postscript",
            "application/x-latex",
            "text/css",
            "text/html",
            "text/plain",
            "text/xml",
            "application/javascript",
            "application/json",
    };
    
    public static final String[] ARCHIVE_MIME_TYPES = {
            "application/x-7z-compressed",
            "application/x-rar-compressed",
            "application/x-tar",
            "application/zip",
            "application/gzip"
    };
    
    
    public static boolean isArchive(String contentType) {
    	for (String s : ARCHIVE_MIME_TYPES) {
    		if (contentType.equalsIgnoreCase(s)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public static boolean isAudio(String contentType) {
    	for (String s : AUDIO_MIME_TYPES) {
    		if (contentType.equalsIgnoreCase(s)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public static boolean isDocument(String contentType) {
    	for (String s : DOCUMENT_MIME_TYPES) {
    		if (contentType.equalsIgnoreCase(s)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public static boolean isImage(String contentType) {
    	for (String s : IMAGE_MIME_TYPES) {
    		if (contentType.equalsIgnoreCase(s)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public static boolean isVideo(String contentType) {
    	for (String s : VIDEO_MIME_TYPES) {
    		if (contentType.equalsIgnoreCase(s)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public static boolean isMedia(String contentType) {
    	return isAudio(contentType) || isVideo(contentType) || isImage(contentType);
    }
    
    /*
    public static String getContentType(File f) {
    	try {
    		return getContentType(KFileUtil.toByteArray(f));
		} catch (IOException e) {
			return null;
		}
    }
    
    public static String getContentType(byte[] b) {
    	MagicMatch match = getMimeInfo(b);
    	if (match == null) {
    		return probeContentType(b);
    	}
    	return match.getMimeType();
    }
    
    public static MagicMatch getMimeInfo(File f) {
    	try {
    		MagicMatch match = Magic.getMagicMatch(f, false, false);
    		return match;
    	} catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
    		return null;
    	}
    }
    
    public static MagicMatch getMimeInfo(byte[] b) {
    	try {
    		// getMagicMatch accepts Files or byte[], which is nice if you want to test streams
    		MagicMatch match = Magic.getMagicMatch(b);
    		if (match.getMimeType().equals("???")) {
    			return null;
    		}
    		return match;
    	} catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
    		return null;
    	}
    }
    */
    
 

    private static final Detector DETECTOR = new DefaultDetector(
            MimeTypes.getDefaultMimeTypes());

    
    public static String getContentType(final String path) {
        return getContentType(new File(path));
    }
    
    public static String getContentType(final File file) {
		try {
			TikaInputStream tikaIS = TikaInputStream.get(file);
			return getContentType(tikaIS);
		} catch (FileNotFoundException e) {
            return null;
		}
    }
    
    public static String getContentType(final byte[] data) {
    	TikaInputStream tikaIS = TikaInputStream.get(data);
		return getContentType(tikaIS);
    }
    
    private static String getContentType(TikaInputStream tikaIS) {
        try {
            final Metadata metadata = new Metadata();
            return DETECTOR.detect(tikaIS, metadata).toString();
        } catch (IOException e) {
            return null;
        } finally {
            if (tikaIS != null) {
                try {
					tikaIS.close();
				} catch (IOException e) {
				}
            }
        }
    }
    
    public static String probeContentType(byte[] b) {
    	try {
    		File f = KFileUtil.writeTempFile(b);
    		return probeContentType(f);
		} catch (IOException e) {
			return null;
		}
    }
    
    public static String probeContentType(String filePath) {
    	return probeContentType(new File(filePath));
    }

    public static String probeContentType(Path file) {
        return probeContentType(file.toFile());
    }
    	
    public static String probeContentType(File f) {
    	logger.debug("Probing contentType for file: " + f.getAbsolutePath());
    	Path source = Paths.get(f.getAbsolutePath());
    	try {
			return Files.probeContentType(source);
		} catch (IOException e) {
			return null;
		}
    }
    
    public static String getExtension(String contentType) {
        switch(contentType) {
        // images
        case "image/jpeg":
        case "image/pjpeg":
            return "jpg";
            
        case "image/gif":
        	return "gif";
            
        case "image/png":
        	return "png";
            
        case "image/bmp":
        	return "bmp";
            
        case "image/pcx": 
        	return "pcx";
            
        case"image/tiff":
        	return "tif";
            
        case "image/ras": 
        	return "ras";
            
        case "image/x-portable-bitmap": 
        case "image/x-portable-graymap":
    	case "image/x-portable-pixmap": 
        	return "pbm";
            
    	case "image/psd":
        	return "pbm";
            
        case "image/x-xcf":
        	return "xcf";
        
            
        // images
        case "video/mpeg":
            return "mpeg";
            
        case "video/mp4":
            return "mp4";
            
        case "video/ogg":
            return "ogg";
            
        case "video/quicktime":
            return "mov";
            
        case "video/webm":
        case "video/x-matroska":
        case "application/x-matroska":
            return "webm";
            
        case "video/x-ms-wmv":
            return "wmv";
            
        case "video/x-flv":
            return "flv";
            
        // images
        case "audio/amr":
            return "amr";
            
        case "audio/3gpp":
        case "audio/3gpp2":
            return "3gpp";
            
        case "audio/basic":
            return "basic";
            
        case "audio/L24":
            return "l24";
            
        case "audio/mp4":
            return "mp4";
            
        case "audio/mpeg":
            return "mpeg";
            
        case "audio/ogg":
            return "ogg";
            
        case "audio/vorbis":
            return "vorbis";
            
        case "audio/vnd.rn-realaudio":
            return "ra";
            
        case "audio/vnd.wav":
        case "audio/wav":
        case "audio/x-wav":
            return "wav";
            
        case "audio/webm":
            return "webm";
            
        case "audio/x-aac":
            return "aac";
            
        case "audio/x-caf":
            return "caf";
            
        // documents
        case "text/plain":
            return "txt";
            
        case "text/html":
            return "html";
            
        case "application/pdf":
            return "pdf";
            
        case "text/vcard":
        case "text/x-vcard":
            return "vcf";
            
        case "text/calendar":
            return "ics";
            
        case "text/vcalendar":
        case "text/x-vcalendar":
            return "vcs";
            
        case "text/rtf":
        case "application/rtf":
            return "rtf";
            
        case "text/richtext":
            return "rtx";
            
        case "application/json":
            return "json";
            
        case "application/javascript":
            return "js";
            
        case "text/css":
            return "css";
                
                
        default:
        	return null;
        }
    }
    
} 
