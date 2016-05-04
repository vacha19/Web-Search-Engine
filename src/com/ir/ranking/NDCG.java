package com.ir.ranking;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class NDCG {

    private NDCG() {
    }

    public static double compute(
            List<String> ranked_items,
            List<String> correct_items,
            Collection<Integer> ignore_items) {

        if (ignore_items == null) {
            ignore_items = new HashSet<Integer>();
        }

        double dcg = 0;
        double idcg = computeIDCG(correct_items.size());
        int left_out = 0;

        for (int i = 0; i < ranked_items.size(); i++) {
            String item_id = ranked_items.get(i);
            if (ignore_items.contains(item_id)) {
                left_out++;
                continue;
            }

            if (!correct_items.toString().contains(item_id)) {
                continue;
            }
            int rank = i + 1 - left_out;
            dcg += Math.log(2) / Math.log(rank + 1);
            //System.err.println("Rank - "+rank);   
        }
        //System.out.println(dcg/idcg);
        return dcg / idcg;
    }

    static double computeIDCG(int n) {
        double idcg = 0;
        for (int i = 0; i < n; i++) {
            idcg += Math.log(2) / Math.log(i + 2);
        }
        //System.out.println(idcg);
        return idcg;
    }
}
