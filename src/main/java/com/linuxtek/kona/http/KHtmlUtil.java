/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.http;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

/**
 * KHtmlUtil.
 */

public class KHtmlUtil {
    
    public static String prettyPrint(String html) {
        Document doc = Jsoup.parse(html);
    	doc.outputSettings().prettyPrint(true);
        return doc.toString();
    }
    
    public static String mobilize(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Element body = doc.body();

        convertTablesToDivs(body);
        stripEmptyDivs(body);
        stripEmptyLists(body);
        stripScripts(body);
        stripStyles(body);
        stripComments(body);

        stripAttribute(body, "align");
        stripAttribute(body, "height");
        stripAttribute(body, "width");
        stripAttribute(body, "style");
        stripAttribute(body, "rel");
        stripAttribute(body, "id");
        stripAttribute(body, "class");


        String html = body.html();

        /*
        // divs/p
        html = html.replaceAll("/<div[^>]*>/ism", "");
        html = html.replaceAll("/<\\/div>/ism","<br/><br/>");
        html = html.replaceAll("/<p[^>]*>/ism","");
        html = html.replaceAll("/<\\/p>/ism", "<br/>");

        // h tags
        html = html.replaceAll("/<h[1-5][^>]*>(.*?)<\\/h[1-5]>/ism", 
                "<br/><b>$1</b><br/><br/>");

        // remove align/height/width/style/rel/id/class tags
        html = html.replaceAll("/\\salign=(\"?\"?).*?\\1/ism","");
        html = html.replaceAll("/\\sheight=(\"?\"?).*?\\1/ism","");
        html = html.replaceAll("/\\swidth=(\"?\"?).*?\\1/ism","");
        html = html.replaceAll("/\\sstyle=(\"?\"?).*?\\1/ism","");
        html = html.replaceAll("/\\srel=(\"?\"?).*?\\1/ism","");
        html = html.replaceAll("/\\sid=(\"?\"?).*?\\1/ism","");
        html = html.replaceAll("/\\sclass=(\"?\"?).*?\\1/ism","");

        // remove coments
        html = html.replaceAll("/<\\!--.*?-->/ism","");


        // multiple \n
        html = html.replaceAll("/\n{2,}/ism","");

        // remove multiple <br/>
        html = html.replaceAll("/(<br\\s?\\/?>){2}/ism","<br/>");
        html = html.replaceAll("/(<br\\s?\\/?>\\s*){3,}/ism","<br/><br/>");

        //tables
        html = html.replaceAll("/<table[^>]*>/ism", "");
        html = html.replaceAll("/<\\/table>/ism", "<br/>");
        html = html.replaceAll("/<(tr|td|th)[^>]*>/ism", "");
        html = html.replaceAll("/<\\/(tr|td|th)[^>]*>/ism", "<br/>");
        */

        return html;
    }


    private static void stripEmptyTag(Element body, String tag) {
        Elements elements = body.select(tag);
        for (Element e : elements) {
            if ((e.html() == null || e.html().trim().length() == 0)
                    && (e.text() == null || e.text().trim().length() == 0)){
                e.remove();
            }
        }
    }

    private static void stripEmptyDivs(Element body) {
        stripEmptyTag(body, "div");
    }

    private static void stripEmptyLists(Element body) {
        stripEmptyTag(body, "li");
        stripEmptyTag(body, "ul");
        stripEmptyTag(body, "ol");
    }

    private static void stripScripts(Element body) {
        Elements divs = body.select("script");
        divs.empty();
    }

    private static void stripStyles(Element body) {
        Elements divs = body.select("style");
        divs.empty();
    }

    private static void stripAttribute(Element body, String attribute) {
        Elements elements = body.getElementsByAttribute(attribute);
        for (Element e : elements) {
            e.removeAttr(attribute);
        }
    }

    private static void stripComments(Node body) {
        for (int i=0; i < body.childNodes().size(); i++) {
            Node child = body.childNode(i);
            if (child.nodeName().equals("#comment"))
                child.remove();
            else {
                stripComments(child);
                i++;
            }
        }
    }

    private static void convertTablesToDivs(Element body) {
        Elements elements = body.select("table");
        for (Element element : elements) {
            element.tagName("div");
        }

        elements = body.select("tbody");
        for (Element element : elements) {
            element.tagName("div");
        }

        elements = body.select("tr");
        for (Element element : elements) {
            element.tagName("div");
        }

        elements = body.select("th");
        for (Element element : elements) {
            element.tagName("p");
        }

        elements = body.select("td");
        for (Element element : elements) {
            element.tagName("p");
        }
    }


    public static String cleanupHtmlDoc(String s) {
        if (s != null) {
            Document doc = Jsoup.parse(s);
            doc.outputSettings().prettyPrint(true);
            s = doc.toString();
        }
        return s;
    }

}
