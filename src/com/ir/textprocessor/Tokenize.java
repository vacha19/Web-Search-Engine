package com.ir.textprocessor;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import com.ir.crawler.Storage;

public class Tokenize {

    public String textFile;

    @SuppressWarnings("null")
    public Tokenize() throws FileNotFoundException, IOException {
    }

    public static void FileWriter(String subDomainsFile, TreeMap<String, Integer> sortedSubDomains) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(subDomainsFile, "UTF-8");
            writer.println(sortedSubDomains);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> tokenizeFileSWR(String data)
            throws Exception {

        ArrayList<String> tokenList = new ArrayList<>();
        ArrayList<String> stopwords = stopwordsList();
        Scanner scanner = new Scanner(data);
        while (scanner.hasNextLine()) {
            String s = scanner.nextLine();
            Pattern checkRegex = Pattern.compile("[A-Za-z0-9']{1,100}");
            Matcher regexMatcher = checkRegex.matcher(s);
            while (regexMatcher.find()) {
                if (regexMatcher.group().length() != 0) {
                    tokenList.add((regexMatcher.group().trim().toLowerCase()));
                }
                tokenList.removeAll(stopwords);
            }
        }
        scanner.close();
        return tokenList;
    }

    public static ArrayList<String> tokenizeFile(String data)
            throws Exception {
        ArrayList<String> tokenList = new ArrayList<>();
        Scanner scanner = new Scanner(data);
        while (scanner.hasNextLine()) {
            String s = scanner.nextLine();
            Pattern checkRegex = Pattern.compile("[A-Za-z0-9']{1,100}");
            Matcher regexMatcher = checkRegex.matcher(s);
            while (regexMatcher.find()) {
                if (regexMatcher.group().length() != 0) {
                    tokenList.add((regexMatcher.group().trim().toLowerCase()));
                }
                //tokenList.removeAll(stopwords);
            }
        }
        scanner.close();
        return tokenList;
    }

    public static HashMap<String, Integer> computeWordFrequencies(
            ArrayList<String> resultList) {
        HashMap<String, Integer> m = new HashMap<String, Integer>();
        for (String a : resultList) {
            Integer freq = m.get(a);
            m.put(a, (freq == null) ? 1 : freq + 1);
        }
        return m;

    }

    public static TreeMap<String, Integer> sort(HashMap<String, Integer> frequencyMap) {
        ValueComparator vc = new ValueComparator(frequencyMap);
        TreeMap<String, Integer> sortedMap = new TreeMap<String, Integer>(vc);
        sortedMap.putAll(frequencyMap);
        return sortedMap;
    }

    public static ArrayList<String> stopwordsList()
            throws FileNotFoundException, IOException {
        FileInputStream list = new FileInputStream("C:/Users/Vacha Shah/Documents/ir/stopwords.txt");
        BufferedReader br1 = new BufferedReader(new InputStreamReader(list));
        int k = 0;
        String s1 = null;
        ArrayList<String> stopwords = new ArrayList<>();
        while ((s1 = br1.readLine()) != null) {
            stopwords.add(s1);
            k++;
        }
        return stopwords;

    }

    public static ArrayList<String> threeGramBuilder(
            ArrayList<String> resultList) {
        ArrayList<String> threeGrams = new ArrayList<>();
        for (int i = 0; i < resultList.size() - 3 + 1; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < i + 3; j++) {
                sb.append((j > i ? " " : "") + resultList.get(j));
            }
            threeGrams.add(sb.toString());
        }
        return threeGrams;

    }

    public static TreeMap<String, Integer> subDomains(ArrayList<String> urls) throws Exception {
        HashMap<String, Integer> subDomainsResult = new HashMap<String, Integer>();
        String subDomainsFile = "subDomains.txt";
        ArrayList<String> domains = new ArrayList<String>();
        System.out.println(domains);
        try {
            for (String url : urls) {
                String domain = url;
                domain = domain.replace("http://www.", "");
                domain = domain.replace(domain.substring(domain.indexOf('/') + 1), "");
                domains.add(domain);
            }
            for (String domain : domains) {
                int value = Collections.frequency(domains, domain);
                if (domain != "") {
                    subDomainsResult.put(domain, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(subDomainsResult);
        TreeMap<String, Integer> sortedSubDomains = new TreeMap<String, Integer>(subDomainsResult);
        sortedSubDomains.putAll(subDomainsResult);
        System.out.println(subDomainsResult);
        FileWriter(subDomainsFile, sortedSubDomains);
//		for(Map.Entry<String,Integer> entry : sortedSubDomains.entrySet()) {
//			  String key = entry.getKey();
//			  Integer value = entry.getValue();
//			  System.out.println("http://"+key + " => " + value);
//			}
        return sortedSubDomains;
    }

    static class ValueComparator implements Comparator<String> {

        Map<String, Integer> map;

        public ValueComparator(Map<String, Integer> base) {
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
}
