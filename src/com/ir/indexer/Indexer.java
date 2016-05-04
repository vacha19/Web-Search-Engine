package com.ir.indexer;

import java.awt.List;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.base.Splitter;
import com.ir.crawler.Storage;
import com.ir.db.Db;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class Indexer {

    static HashMap<String, Posting> invertedIndex;
    static HashMap<String, Integer> urlWordCount;

    public Indexer() {
        invertedIndex = new HashMap<>();
        urlWordCount = new HashMap<>();
    }

    public static int buildIndex() {
        DB db = Db.connectToTextDb();
        if (db != null) {
            DBCollection collection = db.getCollection("data");
            DBCursor cursor = collection.find();
            cursor.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
            while (cursor.hasNext()) {
                String c = cursor.next().toString();
                Object obj = JSON.parse(c);
                String url = ((BasicBSONObject) obj).getString("url");
                String tokens = ((BasicBSONObject) obj).getString("tokens");
                String title = ((BasicBSONObject) obj).getString("title");
                String links = ((BasicBSONObject) obj).getString("links");
                String h1Tag = ((BasicBSONObject) obj).getString("h1Tag");
                String docId = ((BasicBSONObject) obj).getString("docId");
                indexing(url, tokens, docId);
            }
            try {
                storeInDb();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private static void storeInDb() throws Exception {
        DB db = Db.connectToIndexerDb();
        System.out.println(invertedIndex.size());
        if (db != null) {
            DBCollection collection = db.getCollection("data");
            int count = 0;
            for (Entry<String, Posting> entry : invertedIndex.entrySet()) {
                ArrayList<DBObject> urlDetailsList = new ArrayList<DBObject>();
                HashMap<String, Double> tfIdfMap = new HashMap<>();
                String token = entry.getKey();
                Posting posting = entry.getValue();
                for (Entry<String, Integer> tfMap : posting.getPostings().entrySet()) {
                    String url = tfMap.getKey();
                    Integer term = tfMap.getValue();
                    double tf = posting.calculateTf(term, urlWordCount.get(url));
                    double tfIdf = posting.tfIdf(tf, posting.getTotalNumberOfOccurrences());
                    tfIdfMap.put(url, tfIdf);
                    //double score=
                }
                //scoreMap.put(token,collection,  url,tfIdf);
                TreeMap<String, Double> tfIdf = sort(tfIdfMap);
                for (Entry<String, Double> resMap : tfIdf.entrySet()) {
                    String url = resMap.getKey();
                    double tfIdfValue = resMap.getValue();
                    BasicDBObject urlDetails = new BasicDBObject().append("url", url);
                    urlDetails.append("positions", posting.getPositions().get(url));
                    urlDetails.append("tfIdf", tfIdfValue);
                    urlDetailsList.add(urlDetails);
                }

                BasicDBObject invertedIndexDetails = new BasicDBObject("$set", new BasicDBObject().append("token", token)).append("$push", new BasicDBObject("details", new BasicDBObject("$each", urlDetailsList)));
                collection.update(new BasicDBObject("token", token), invertedIndexDetails, true, false);
                System.out.println("Inserted in db - " + count);
                count++;
            }
        }
    }

    public static TreeMap<String, Double> sort(HashMap<String, Double> tfIdfMap) {
        ValueComparator vc = new ValueComparator(tfIdfMap);
        TreeMap<String, Double> sortedMap = new TreeMap<String, Double>(vc);
        sortedMap.putAll(tfIdfMap);
        return sortedMap;
    }

    static class ValueComparator implements Comparator<String> {

        Map<String, Double> map;

        private ValueComparator(HashMap<String, Double> base) {
            this.map = base;
        }

        public int compare(String a, String b) {
            if (map.get(a) >= map.get(b)) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private static void indexing(String url, String tokens, String docId) {
        String[] arr = tokens.split(", ");
        Integer position = 0;
        int N = arr.length;
        urlWordCount.put(url, N);
        for (String token : arr) {
            Posting posting = invertedIndex.get(token);
            if (posting == null) {
                posting = new Posting(token);
            }
            posting.addPosition(url, position);
            posting.addOccurrence(url, N);
            position++;
            invertedIndex.put(token, posting);
        }
    }

    public HashMap<String, Double> getTfIdfData(String dataOption, BasicDBObject query) throws Exception {
        DB database = Db.connectToIndexerDb();
        HashMap<String, Double> urlMap = new HashMap<>();
        if (database != null) {
            DBCollection collection = database.getCollection("data");
            DBCursor cursor = collection.find(query);
            while (cursor.hasNext()) {
                String c = cursor.next().toString();
                Object obj = JSON.parse(c);
                JSONObject json = new JSONObject(((BasicBSONObject) obj));
                JSONArray jsonArray = json.getJSONArray("details");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonobject = jsonArray.getJSONObject(i);
                    String url = jsonobject.getString("url");
                    double tfIdf = (double) jsonobject.get(dataOption);
                    urlMap.put(url, tfIdf);
                }
            }
        }
        return urlMap;
    }

    public HashMap<String, Double> getTfIdfDataByUrl(String dataOption, BasicDBObject query, String checkUrl) throws Exception {
        DB database = Db.connectToIndexerDb();
        HashMap<String, Double> urlMap = new HashMap<>();
        if (database != null) {
            DBCollection collection = database.getCollection("data");
            DBCursor cursor = collection.find(query);
            while (cursor.hasNext()) {
                String c = cursor.next().toString();
                Object obj = JSON.parse(c);
                JSONObject json = new JSONObject(((BasicBSONObject) obj));
                JSONArray jsonArray = json.getJSONArray("details");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonobject = jsonArray.getJSONObject(i);
                    String url = jsonobject.getString("url");
                    if (url.equalsIgnoreCase(checkUrl)) {
                        double tfIdf = (double) jsonobject.get(dataOption);
                        urlMap.put(url, tfIdf);
                    }
                }
            }
        }
        return urlMap;
    }

    public HashMap<String, ArrayList<Integer>> getPositionData(String dataOption, BasicDBObject query) throws Exception {
        DB database = Db.connectToIndexerDb();
        HashMap<String, ArrayList<Integer>> urlMap = new HashMap<>();
        if (database != null) {
            DBCollection collection = database.getCollection("data");
            DBCursor cursor = collection.find(query);
            while (cursor.hasNext()) {
                String c = cursor.next().toString();
                Object obj = JSON.parse(c);
                JSONObject json = new JSONObject(((BasicBSONObject) obj));
                JSONArray jsonArray = json.getJSONArray("details");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonobject = jsonArray.getJSONObject(i);
                    String url = jsonobject.getString("url");
                    JSONArray arrayResp = new JSONArray(jsonobject.get(dataOption).toString());
                    ArrayList<Integer> myList = new ArrayList<>();
                    for (int j = 0; j < arrayResp.length(); j++) {
                        myList.add(arrayResp.getInt(j));
                    }
                    urlMap.put(url, myList);
                }
            }
        }
        return urlMap;
    }

    static void printIndex() {
        System.out.println("======================================================================");
        System.out.println("*********************Index LIST*************************************");
        System.out.println("======================================================================");
        for (Entry<String, Posting> entry : invertedIndex.entrySet()) {
            String key = entry.getKey();
            Posting value = entry.getValue();
            System.out.println("(String, Index)::\t(" + key + ", " + value.getPostings() + ")");
            System.out.println("(String, Index)::\t(" + key + ", " + value.getPositions() + ")");
        }
        System.out.println("\n\n");
    }

    public void update() throws Exception {
        DB database = Db.connectToIndexerDb();
        //DB db = Db.connectToRankingDb();
        BasicDBObject query = new BasicDBObject("token", new BasicDBObject("$regex", "retrieval"));
        if (database != null) {
            DBCollection collection = database.getCollection("data");
            DBCursor cursor = collection.find(query);
            int count = 0;
            cursor.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
            while (cursor.hasNext()) {
                String c = cursor.next().toString();
                //System.out.println(c);
                Object obj = JSON.parse(c);
                int urlCounts = 0;
                JSONObject json = new JSONObject(((BasicBSONObject) obj));
                JSONArray jsonArray = json.getJSONArray("details");
                String token = json.getString("token");
                ArrayList<DBObject> urlDetailsList = new ArrayList<DBObject>();

                HashMap<String, Double> tfIdfMap = new HashMap<>();
                HashMap<String, Double> scoreMap = new HashMap<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonobject = jsonArray.getJSONObject(i);
                    if (urlCounts > 1000) {
                        break;
                    }
                    urlCounts++;
                    String url = jsonobject.getString("url");
                    //System.out.println(url);
                    double tfIdf = jsonobject.getDouble("tfIdf");
                    double score = updateScores(token, url, tfIdf);
                    tfIdfMap.put(url, tfIdf);
                    scoreMap.put(url, score);
                }
                TreeMap<String, Double> scores = sort(scoreMap);
                count++;
                HashMap<String, Double> map = new HashMap<>();
                for (Entry<String, Double> resMap : scores.entrySet()) {
                    String url = resMap.getKey();
                    double scoreValue = resMap.getValue();
                    BasicDBObject urlDetails = new BasicDBObject().append("url", url);
                    //urlDetails.append("tfIdf", tfIdfMap.get(url));
                    urlDetails.append("tfIdf", scoreValue);
                    urlDetailsList.add(urlDetails);
                    //System.err.println(url);
                }
                BasicDBObject document = new BasicDBObject();
                document.put("details", urlDetailsList);
                document.put("token", token);
                BasicDBObject invertedIndexDetails = new BasicDBObject("$set", document);
                collection.update(new BasicDBObject("token", token), invertedIndexDetails, true, false);
                System.out.println("Inserted in db - " + count);
            }
        }
    }

    public void parseTags() throws Exception {
        DB database = Db.connect();
        if (database != null) {
            DBCollection collection = database.getCollection("WebDocs");
            DBCursor cursor = collection.find();

            int count = 0;
            cursor.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
            while (count < 575222 && cursor.hasNext()) {
                String c = cursor.next().toString();
                Object obj = JSON.parse(c);
                String html = ((BasicBSONObject) obj).getString("html");
                String url = ((BasicBSONObject) obj).getString("url");
                ArrayList<String> anchors = new ArrayList<>();
                ArrayList<String> h1s = new ArrayList<>();
                ArrayList<String> h2s = new ArrayList<>();
                ArrayList<String> h3s = new ArrayList<>();
                ArrayList<String> content_titles = new ArrayList<>();
                Document doc = Jsoup.parse(html);
                String title = doc.title();
                Elements links = doc.select("a");
                Elements divs = doc.select("div#content_title");
                Elements h1Tag = doc.select("h1");
                Elements h2Tag = doc.select("h2");
                Elements h3Tag = doc.select("h3");
                for (Element link : links) {
                    String absHref = link.attr("abs:href");
                    if (absHref != "") {
                        anchors.add(absHref);
                    }
                }
                if (h1Tag != null) {
                    for (Element h1 : h1Tag) {
                        h1s.add(h1.text());

                    }
                }
                if (h2Tag != null) {
                    for (Element h2 : h2Tag) {
                        h2s.add(h2.text());

                    }
                }
                if (h3Tag != null) {
                    for (Element h3 : h3Tag) {
                        h3s.add(h3.text());

                    }
                }
                if (divs != null) {
                    for (Element div : divs) {
                        content_titles.add(div.text());
                    }
                }

                BasicDBObject document = new BasicDBObject();
                document.put("title", title.toString());
                document.put("links", anchors);
                document.put("h1Tag", h1s);
                document.put("h2Tag", h2s);
                document.put("h3Tag", h3s);
                document.put("content_title", content_titles);

                BasicDBObject invertedIndexDetails = new BasicDBObject("$push", document);
                collection.update(new BasicDBObject("url", url), invertedIndexDetails, true, false);
                count++;
                System.out.println("Inserted in db - " + count);

            }
        }
    }

    private static double updateScores(String token, String url, double tfIdf)
            throws Exception {
        DB database = Db.connect();
        DBCollection collection = database.getCollection("WebDocs");
        double score = tfIdf;
        BasicDBObject query = new BasicDBObject();
        query.put("url", url);

        DBCursor cursor = collection.find(query);
        String c = cursor.next().toString();
        System.out.println(c);

        Object obj = JSON.parse(c);
        ArrayList<String> links = null;
        ArrayList<String> h1Tags = null;
        ArrayList<String> content_titles = null;
        String title = ((BasicBSONObject) obj).getString("title");
        String temp = ((BasicBSONObject) obj).getString("links");
        if (temp != null) {
            links = new ArrayList<String>(Arrays.asList(temp.split(",")));
        }
        temp = ((BasicBSONObject) obj).getString("h1Tag");
        if (temp != null) {
            h1Tags = new ArrayList<String>(Arrays.asList(temp.split(",")));
        }
        temp = ((BasicBSONObject) obj).getString("content_title");
        if (temp != null) {
            content_titles = new ArrayList<String>(Arrays.asList(temp
                    .split(",")));

        }

        if (h1Tags != null) {
            for (String h1Tag : h1Tags) {

                if (h1Tag.toLowerCase().contains(token)) {
                    //System.err.println(h1Tag);
                    score += 0.1;
                    System.out.println(url + ":" + score);

                }
            }
        }

        if (content_titles != null) {
            for (String div : content_titles) {
                if (div.toLowerCase().contains(token)) {
                    //System.err.println(div);
                    score += 5;
                    System.out.println(url + ":" + score);
                }
            }
        }

        if (links != null) {

            for (String link : links) {

                if (link.contains(token)) {
                    score += 5;

                }
                System.out.println(url + ":" + score);
            }
        }
        if (title != null) {
            title = title.toLowerCase();

            if (title.contains(token)) {
                score += 500;
            }
            System.out.println(url + ":" + score);

            if (url.contains(token)) {
                score += 5;
            }
            System.out.println(url + ":" + score);
        }
        //System.out.println(url + ":" + score);
        return score;
    }
}
