package com.ir.search;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.ir.crawler.Storage;
import com.ir.indexer.Indexer;
import com.ir.ranking.NDCG;
import com.mongodb.BasicDBObject;
import org.jsoup.nodes.Document;

public class Main {

    public static void main(String[] args) throws Exception {
        //Db.connect();
        //Controller c = new Controller();
        //c.startCrawler();
        String searchQuery = "";
        
        //Storage.textProcessing();
        //Pattern p=Pattern.compile("[is]");
        //Matcher m=p.matcher(example);
        //if(m.matches())
        //  System.out.println("Yes");
        //else
        //  System.out.println("No");
        Indexer index = new Indexer();
        //index.update();
        //index.buildIndex();
        System.out.println("Query: " + searchQuery);
        searchQuery = searchQuery.toLowerCase();
        String trimmed = searchQuery.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        //System.out.println(words);
        HashMap<String, Double> urlMap = new HashMap<>();
        if (words == 1) {
            getTfIdf(searchQuery, index, urlMap);
        } else {
            Scanner scanner = new Scanner(searchQuery);
            while (scanner.hasNext()) {
                String s = scanner.next();
                //System.out.println(s);
                getTfIdf(s, index, urlMap);
            }
            scanner.close();
        }
        ArrayList<String> rankedResults = sortTopTenUrls(index, urlMap);
        System.out.println("Ranked Results: ");
        for (int j = 0; j < rankedResults.size(); j++) {
            System.out.println(rankedResults.get(j));
        }
        Elements links = getGoogleResults(searchQuery.concat(" site:ics.uci.edu"));
        double ndcg = NDCG(links, rankedResults);
        System.out.println(ndcg);
    }

    private static Elements getGoogleResults(String searchQuery) throws IOException {
        String google = "http://www.google.com/search?num=10&q=";
        String charset = "UTF-8";
        String userAgent = "ExampleBot 1.0 (+http://example.com/bot)";
        System.out.println("Google Results: ");
        Elements links = Jsoup.connect(google + URLEncoder.encode(searchQuery, charset)).userAgent(userAgent).get().select(".g>.r>a");

        for (Element link : links) {
            String title = link.text();
            String url = link.absUrl("href");
            url = URLDecoder.decode(url.substring(url.indexOf('=') + 1, url.indexOf('&')), "UTF-8");
            if (!url.startsWith("http")) {
                continue;
            }
            if (url.endsWith("pdf")) {
                continue;
            }
            System.out.println(url);
        }
        return links;
    }

    private static double NDCG(Elements links, ArrayList<String> rankedResults) throws UnsupportedEncodingException {
        int i = 0;
        ArrayList<String> correctUrls = new ArrayList<String>();
        int count = 1;
        for (Element link : links) {
            if (count > 5) {
                break;
            }
            String url = link.absUrl("href");
            url = URLDecoder.decode(url.substring(url.indexOf('=') + 1, url.indexOf('&')), "UTF-8");
            if (!url.startsWith("http")) {
                continue;
            }
            if (url.endsWith("pdf")) {
                continue;
            }
            count++;
            correctUrls.add(url);
        }
        //System.err.println("Correct Urls" + correctUrls);
        double ndcg = NDCG.compute(rankedResults, correctUrls, null);
        return ndcg;
    }

    private static HashMap<String, Double> getTfIdf(String searchQuery, Indexer index, HashMap<String, Double> urlMap) throws Exception {
        HashMap<String, Double> tfIdf_temp = new HashMap<>();
        BasicDBObject query = new BasicDBObject("token", new BasicDBObject("$regex", searchQuery));
        tfIdf_temp = index.getTfIdfData("tfIdf", query);
        int count = 0;
        for (Entry<String, Double> entry : tfIdf_temp.entrySet()) {

            String key = entry.getKey();
            Double value = entry.getValue();
            urlMap.put(key, value);
           if (count > 200) {
               break;
           }
           count++;
            BasicDBObject titleQuery = new BasicDBObject();
            titleQuery.put("url", key);
            String html = Storage.getData("html", titleQuery);
            Document doc = Jsoup.parse(html);
            String title = doc.title();
            Elements links = doc.select("a");
            Elements hTags = doc.select("h1, h2, h3, h4, h5, h6");
            if (hTags != null) {
                Elements h1Tags = hTags.select("h1");
                Elements h2Tags = hTags.select("h2");
                Elements h3Tags = hTags.select("h3");
                if (h1Tags != null) {
                    for (Element h1Tag : h1Tags) {

                        if (h1Tag.text().toLowerCase().contains(searchQuery)) {
                            updataTFIDF(urlMap, key, value, 0.1);
                        }
                    }
                }
                if (h2Tags != null) {
                    for (Element h2Tag : h2Tags) {
                        if (h2Tag.text().toLowerCase().contains(searchQuery)) {
                            updataTFIDF(urlMap, key, value, 0.08);
                        }
                    }
                }
                if (h3Tags != null) {
                    for (Element h3Tag : h3Tags) {
                        if (h3Tag.text().toLowerCase().contains(searchQuery)) {
                            updataTFIDF(urlMap, key, value, 0.07);
                        }
                    }
                }

            }
            Elements divs = doc.select("div#content_title");
            if (divs != null) {
                for (Element div : divs) {
                    if (div.text().toLowerCase().contains(searchQuery)) {
                        updataTFIDF(urlMap, key, value, 5);
                        //System.out.println("DIV - "+div.text());
                    }
                }
            }

            if (links != null) {

                for (Element link : links) {
                    String absHref = link.attr("abs:href");
                    if (absHref.contains(searchQuery)) {
                        updataTFIDF(urlMap, key, value, 5);
                    }
                }
            }
            title = title.toLowerCase();

            if (title.contains(searchQuery)) {
                //System.out.println("Title "+title+" url "+key);
                updataTFIDF(urlMap, key, value, 500);
            }

            if (key.contains(searchQuery)) {
                updataTFIDF(urlMap, key, value, 5);
            }
        }
        return urlMap;
    }

    private static ArrayList<String> sortTopTenUrls(Indexer index, HashMap<String, Double> urlMap) throws Exception {
        ArrayList<String> topTenUrlslist = new ArrayList<>();
        TreeMap<String, Double> sortedUrlMap = index.sort(urlMap);
        sortUrls(index, urlMap);
        for (Entry<String, Double> entry : sortedUrlMap.entrySet()) {
            if (topTenUrlslist.size() > 4) {
                break;
            }
            String onlyUrl = entry.getKey();
            if (entry.getKey().contains("?")) {
                onlyUrl = entry.getKey().substring(0, entry.getKey().lastIndexOf("?"));
            }
            if (!topTenUrlslist.contains(onlyUrl)) {
                topTenUrlslist.add(onlyUrl);
            }
        }
        return topTenUrlslist;
    }

    private static void sortUrls(Indexer index, HashMap<String, Double> urlMap) throws Exception {
        ArrayList<String> topTenUrlslist = new ArrayList<>();
        TreeMap<String, Double> sortedUrlMap = index.sort(urlMap);
        for (Entry<String, Double> entry : sortedUrlMap.entrySet()) {
            if (topTenUrlslist.size() > 10) {
                break;
            }
            String onlyUrl = entry.getKey();
            //System.out.println("TOP TEN URLS - "+onlyUrl);
        }
    }

    private static void updataTFIDF(HashMap<String, Double> urlMap, String key, Double value, double factor) {
        if (urlMap.get(key) == null) {
            urlMap.put(key, value + factor);
        } else {
            double newValue = urlMap.get(key) + value;
            urlMap.put(key, newValue + factor);
        }

    }
    
    
}

