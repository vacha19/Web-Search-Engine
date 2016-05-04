package com.ir.db;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;

public class Db {

    static MongoClient crawlerClient = null;
    static MongoClient textProcClient = null;
    static MongoClient indexClient = null;
    static DB db = null;
    static DB textDb = null;
    static DB indexDb = null;

    public static DB connect() {
        if (crawlerClient != null) {
            try {
                return crawlerClient.getDB("Crawler_new");
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                return null;
            }
        } else {
            try {
                crawlerClient = new MongoClient("localhost", 27017);
                db = crawlerClient.getDB("Crawler_new");
                System.out.println("Connect to database successfully");
            } catch (NullPointerException e) {
                e.printStackTrace();
                System.exit(1);
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return null;
            }
        }
        return db;
    }

    public static DB connectToTextDb() {
        if (textProcClient != null) {
            try {
                return textProcClient.getDB("TextProcessor");
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                return null;
            }
        } else {
            try {
                textProcClient = new MongoClient("localhost", 27017);
                textDb = textProcClient.getDB("TextProcessor");
                System.out.println("Connect to text database successfully");
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return null;
            }
        }
        return textDb;
    }

    public static DB connectToIndexerDb() {
        if (indexClient != null) {
            try {
                return indexClient.getDB("InvertedIndex");
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                return null;
            }
        } else {
            try {
                indexClient = new MongoClient("localhost", 27017);
                indexDb = indexClient.getDB("InvertedIndex");
                System.out.println("Connect to index database successfully");
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return null;
            }
        }
        return indexDb;
    }

    public static DB connectToIndexerFinalDb() {
        if (indexClient != null) {
            try {
                return indexClient.getDB("InvertedIndex_final");
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                return null;
            }
        } else {
            try {
                indexClient = new MongoClient("localhost", 27017);
                indexDb = indexClient.getDB("InvertedIndex_final");
                System.out.println("Connect to index database successfully");
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return null;
            }
        }
        return indexDb;
    }

}
