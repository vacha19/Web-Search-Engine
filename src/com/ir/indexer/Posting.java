package com.ir.indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Posting {

    Set<String> docIds;
    HashMap<String, Integer> postings;
    HashMap<String, ArrayList<Integer>> positions;
    JSONObject obj;

    public Posting(String token) {
        postings = new HashMap<>();
        positions = new HashMap<>();
    }

    public void addOccurrence(String documentId, Integer N) {
        Integer termFrequency = 1;
        if (postings.get(documentId) == null) {
            postings.put(documentId, termFrequency);
        } else {
            termFrequency = postings.get(documentId);
            termFrequency++;
            postings.put(documentId, termFrequency);
        }
    }

    int getTotalNumberOfOccurrences() {
        int collectionFrequency = 0;
        Set<String> urls = getDocumentIds();
        for (String url : urls) {
            if (postings.get(url) != null) {
                collectionFrequency++;
            }
        }
        return collectionFrequency;
    }

    Set<String> getDocumentIds() {
        return postings.keySet();
    }

    HashMap<String, Integer> getPostings() {
        return postings;
    }

    int getNumberOfOccurrencesInDocument(String documentId) {
        int termFrequency = 0;
        if (postings.get(documentId) != null) {
            termFrequency = postings.get(documentId);
        }
        return termFrequency;
    }

    public void addPosition(String url, Integer pos) {
        ArrayList<Integer> position = new ArrayList<>();
        if (positions.get(url) == null) {
            position.add(pos);
            positions.put(url, position);
        } else {
            position = positions.get(url);
            position.add(pos);
            positions.put(url, position);
        }
        positions.put(url, position);
    }

    public HashMap<String, ArrayList<Integer>> getPositions() {
        return positions;
    }

    public double tfCalc(double tf2) {
        return tf2;
    }

    public double idfCalc(int i) {
        double n = 56186;
        return Math.log(n / i);
    }

    public double tfIdf(double tf2, int i) {
        return tfCalc(tf2) * idfCalc(i);
    }

    public double calculateTf(Integer term, Integer N) {
        double termFreq = ((double) term / N);
        return termFreq;
    }
}
