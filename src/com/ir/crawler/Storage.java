package com.ir.crawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.bson.BasicBSONObject;

import com.ir.db.Db;
import com.ir.textprocessor.Tokenize;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.util.JSON;

public class Storage {

    static String crawlerDb = "Crawler_final";
    static String textProcDb = "TextProcessor";

    public static void add(String url, String text, int docId, String subDomain, String html) throws Exception {
        DB database = Db.connect();
        if (database != null) {
            DBCollection collection = database.getCollection("WebDocs");
            BasicDBObject document = new BasicDBObject();
            document.put("url", url);
            document.put("text", text);
            document.put("html", html);
            document.put("docId", docId);
            document.put("subDomain", subDomain);
            collection.insert(document);
        }
    }
    // Get all Data

    public static void getAllData() throws Exception {
        DB database = Db.connect();
        if (database != null) {
            DBCollection collection = database.getCollection("WebDocs");
            DBCursor cursor = collection.find();
            while (cursor.hasNext()) {
                System.out.println(cursor.next());
            }
        }
    }

    public static String getData(String dataOption, BasicDBObject query) throws Exception {
        String result = "";
        DB database = Db.connect();
        if (database != null) {
            DBCollection collection = database.getCollection("WebDocs");
            DBCursor cursor = collection.find(query);
            while (cursor.hasNext()) {
                String c = cursor.next().toString();
                Object obj = JSON.parse(c);
                result = (((BasicBSONObject) obj).getString(dataOption));
                //System.out.println(result);				
                //resultArray.add(result.split("[\\.\\?\\!][\\r\\n\\t ]+")[0] + ".");
                //System.out.println(line);	
            }
        }
        return result;
    }
    // Get all Data

    public static void textProcessing() throws Exception {
        DB database = Db.connect();
        ArrayList<String> totalTokens = new ArrayList<String>();
        ArrayList<String> totalTokensSWR = new ArrayList<String>();
        ArrayList<String> totalThreeGramsSWR = new ArrayList<String>();
        ArrayList<String> totalSubDomains = new ArrayList<String>();
        int maxWords = 0;
        String maxUrl = "";
        long startTime = System.currentTimeMillis();

        if (database != null) {
            DBCollection collection = database.getCollection("WebDocs");
            DBCursor cursor = collection.find();
            cursor.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
            cursor.skip(56968);
            while (cursor.hasNext()) {
                String c = cursor.next().toString();
                Object obj = JSON.parse(c);
                String url = ((BasicBSONObject) obj).getString("url");
                String text = ((BasicBSONObject) obj).getString("text");
                String subDomains = ((BasicBSONObject) obj).getString("subDomain");
                String docId = ((BasicBSONObject) obj).getString("docId");
                ArrayList<String> tokens = Tokenize.tokenizeFile(text);
                ArrayList<String> tokensSWR = Tokenize.tokenizeFileSWR(text);
                totalTokens.addAll(tokens);
                totalTokensSWR.addAll(tokensSWR);
                totalSubDomains.add(url);
                ArrayList<String> threeGrams = Tokenize.threeGramBuilder(tokens);
                ArrayList<String> threeGramsSWR = Tokenize.threeGramBuilder(tokensSWR);

                ArrayList<String> newThreeGrams = new ArrayList<>();
                for (String str : threeGramsSWR) {
                    if (threeGrams.contains(str)) {
                        newThreeGrams.add(str);
                    }
                }
                totalThreeGramsSWR.addAll(newThreeGrams);
                DB tpDatabase = Db.connectToTextDb();
                if (tpDatabase != null) {
                    DBCollection dataCollection = tpDatabase.getCollection("data");
                    BasicDBObject document = new BasicDBObject();
                    document.put("url", url);
                    document.put("text", text);
                    document.put("tokens", tokens.toString());
                    document.put("tokensSWR", tokensSWR.toString());
                    document.put("threeGramsSWR", newThreeGrams.toString());
                    document.put("docId", docId);
                    document.put("subDomain", subDomains);
                    dataCollection.insert(document);
                }
                int words = tokens.size();
                if (maxWords < words) {
                    maxUrl = url;
                    maxWords = words;
                }
            }
            HashMap<String, Integer> frequencyMap = Tokenize.computeWordFrequencies(totalTokensSWR);
            TreeMap<String, Integer> sortedMap = Tokenize.sort(frequencyMap);
            Tokenize.FileWriter("CommonWords.txt", sortedMap);
            System.out.println("" + maxWords + "==>" + maxUrl);
            HashMap<String, Integer> frequencyMapthreeGramsSWR = Tokenize.computeWordFrequencies(totalThreeGramsSWR);
            TreeMap<String, Integer> sortedMapthreeGramsSWR = Tokenize.sort(frequencyMapthreeGramsSWR);
            Tokenize.FileWriter("Common3Grams.txt", sortedMapthreeGramsSWR);
            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;
            System.out.println(timeTaken);
        }
    }

}
