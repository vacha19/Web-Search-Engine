package com.ir.crawler;

import java.util.Set;
import java.util.regex.Pattern;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class Crawler extends WebCrawler {

    private final static Pattern FILTERS
            = Pattern.compile(".*\\.(bmp|gif|jpe?g|png|tiff?|pdf|ico|xaml|pict|rif|pptx?|ps"
                    + "|mid|mp2|mp3|mp4|wav|wma|au|aiff|flac|ogg|3gp|aac|amr|au|vox"
                    + "|avi|mov|mpe?g|ra?m|m4v|smil|wm?v|swf|aaf|asf|flv|mkv"
                    + "|zip|rar|gz|7z|aac|ace|alz|apk|arc|arj|dmg|jar|lzip|lha|js|tex|ppt|doc|docx|xls|csv|exe|xlsx|tar|iso|epub)"
                    + "(\\?.*)?$");

    /**
     * This method receives two parameters. The first parameter is the page in
     * which we have discovered this new url and the second parameter is the new
     * url. You should implement this function to specify whether the given url
     * should be crawled or not (based on your crawling logic). In this example,
     * we are instructing the crawler to ignore urls that have css, js, git, ...
     * extensions and to only accept urls that start with
     * "http://www.ics.uci.edu/". In this case, we didn't need the referringPage
     * parameter to make the decision.
     *
     * @param referringPage
     * @param url
     * @return
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        return !FILTERS.matcher(href).matches()
                && href.contains(".ics.uci.edu/") //adding probable traps
                && !href.startsWith("http://archive.ics.uci.edu/")
                && !href.startsWith("http://calendar.ics.uci.edu/")
                && !href.startsWith("https://calendar.ics.uci.edu/")
                && !href.startsWith("http://flamingo.ics.uci.edu/releases")
                && !href.startsWith("http://ironwood.ics.uci.edu/")
                && !href.startsWith("http://drzaius.ics.uci.edu/")
                && !href.startsWith("http://duttgroup.ics.uci.edu/")
                && !href.startsWith("https://duttgroup.ics.uci.edu/")
                && !href.startsWith("http://fano.ics.uci.edu/ca/")
                && !href.startsWith("http://djp3-pc2.ics.uci.edu/LUCICodeRespository/")
                && !href.startsWith("https://www.ics.uci.edu/prospective/")
                && !href.startsWith("http://www.ics.uci.edu/prospective/");
    }

    /**
     * This function is called when a page is fetched and ready to be processed
     * by your program.
     *
     * @param page
     */
    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        System.out.println("URL: " + url);
        if (page.getParseData() instanceof HtmlParseData) {
            int docId = page.getWebURL().getDocid();
            String subDomain = page.getWebURL().getSubDomain();
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String text = htmlParseData.getText();
            String html = htmlParseData.getHtml();
            Set<WebURL> links = htmlParseData.getOutgoingUrls();
            System.out.println("Text length: " + text.length());
            //System.out.println("Html length: " + html.length());
            //System.out.println("Number of outgoing links: " + links.size());
            try {
                Storage.add(url, text, docId, subDomain, html);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

}
