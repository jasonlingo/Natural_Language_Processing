import java.util.*;

/**
 * Created by Jason on 3/13/16.
 */
public class Earley {

    private Map<String, List<DottedRule>> check;  //for checking duplicated rule in one column
    private Map<String, List<Rule>> rules;
    private List<DottedRule> chartHead;           // keep the first DottedRule of each column
    private List<DottedRule> chartTail;
    private Map<String, DottedRule> dottedRulePos;// record the position of DottedRules

    public Earley() {
        this.check = new HashMap<String, List<DottedRule>>();
        this.chartHead = new ArrayList<DottedRule>();
        this.chartTail = new ArrayList<DottedRule>();
        this.rules = null;
        this.dottedRulePos = new HashMap<String, DottedRule>();
    }

    public void setRules(Map<String, List<Rule>> rules) {
        this.rules = rules;
    }

    public void parse(List<String> sentences) {
        if (this.rules == null) {
            System.out.println("No grammar!");
        }

        for (String orgSen : sentences) {
            String[] sen = orgSen.split(" ");
            System.out.println(decode(sen));
        }
    }

    private String decode(String[] sen) {
        System.out.println(Arrays.toString(sen));

        chartHead.clear();
        chartTail.clear();
        check.clear();
        dottedRulePos.clear();

        // initialize root dottedRule
        List<Rule> root = this.rules.get("ROOT");

        DottedRule dummy = new DottedRule(0, 0, null, 0);
        DottedRule curr = dummy;
        for (Rule r : root) {
            curr.next = new DottedRule(0, 0, r, r.getWeight());
            curr = curr.next;
        }
        chartHead.add(dummy.next);
        dottedRulePos.put("0" + "_" + dummy.next.toString(), dummy.next);
        chartTail.add(curr);
        dottedRulePos.put("0" + "_" + curr.toString(), curr);


        for (int i = 0; i <= sen.length; i++) {
            if (i >= chartHead.size()) {
                return "None";
            }
            DottedRule head = chartHead.get(i);
            while (head != null) {
                if (isComplete(head)) {
                    attach(head, i);
                } else {
                    if (nextIsCat(head)) {
                        // The current rule (at the right of the dot) is a grammar that
                        // can be expanded to other grammar.
                        predict(i, head);
                    } else {
                        scan(i, head, sen);
                    }
                }
                head = head.next;
            }
        }

        // From the last column, find the ROOT constituent with the lowest log probability
        curr = chartHead.get(chartHead.size() - 1);
        DottedRule bestParse = null;
        double bestScore = Double.MAX_VALUE;
        while (curr != null) {
            if (curr.getStartPosition() == 0 && curr.getRule().getLhs().equals("ROOT")) {
                if (curr.getWeight() < bestScore) {
                    bestParse = curr;
                    bestScore = curr.getWeight();
                }
            }
            curr = curr.next;
        }

        if (bestParse == null) {
            return "None";
        } else {
            System.out.println("best weight:" + Double.toString(bestScore));
            return "";
        }
//        if ( bestScore >= Double.MAX_VALUE ) {
//            System.out.println("NONE");
//            return "";
//        }
//        for (int i = 0; i < sen.length; i++) {
//            System.out.print(sen[i] + ' ');
//        }
//        System.out.println( "best weight:" + Double.toString(bestScore));
//
//        return "";

    }

    /*
     Check whether this DottedRule is complete, i.e. the dot is at the end of the rule.
     */
    private Boolean isComplete(DottedRule dottedRule) {
        return dottedRule.getRule().getRhs().length == dottedRule.getDotPosition();
    }

    private Boolean nextIsCat(DottedRule dottedRule) {

        String[] rhs = dottedRule.getRule().getRhs();
        int keyIdx = dottedRule.getDotPosition();
        try {
            String key = rhs[keyIdx];
            return rules.containsKey(key);
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return false;
    }


    private void predict(int colNum, DottedRule dottedRule) {
        // the name of lhs for prediciton, which is the rhs pointed by the dotPosition
        String predictKey = genPredictKey(dottedRule);

        // unique key for each entry in the Early chart
        String checkKey = genCheckKey(colNum, dottedRule, predictKey);

        List<Rule> predictResult = rules.get(predictKey);
        List<DottedRule> dottedPredictResult = new ArrayList<DottedRule>();

        // Check if the predicted rule is already in the column
        if (!check.containsKey(checkKey)) {
            for (Rule rule : predictResult) {
                // the initial dotted rule predicted should be 0
                // once the dot moves to the right, add the weight of rules
                // on the left
                DottedRule next = new DottedRule(colNum, 0, rule, rule.getWeight());
                next.previous = dottedRule;
                addToChart(next, colNum);
                dottedPredictResult.add(next);
            }

            check.put(checkKey, dottedPredictResult);

        } else {
            boolean updated = false;
            for (Rule rule : predictResult) {

                DottedRule next = new DottedRule(colNum, 0, rule, rule.getWeight());
//                next.previous = dottedRule;
                dottedPredictResult.add(next);
                if (replaceDottedRule(next, colNum)) {
                    updated = true;
                }
            }
            if (updated) {
                check.put(checkKey, dottedPredictResult);
            }
        }
    }


    private void scan(int colNum, DottedRule dottedRule, String[] words) {

        if (colNum >= words.length) {
            return;
        }

        Rule rule = dottedRule.getRule();
        int dotPosition = dottedRule.getDotPosition();
        String[] rhs = rule.getRhs();
        String terminal = rhs[dotPosition];

        if (!terminal.equals(words[colNum])) return;

        DottedRule scannedRule = new DottedRule(dottedRule.getStartPosition(), ++dotPosition, rule, dottedRule.getWeight());

        addToChart(scannedRule, colNum + 1);
    }

    private void attach(DottedRule dottedRule, int colNum) {
        String match = dottedRule.getRule().getLhs();
        DottedRule head = chartHead.get(dottedRule.getStartPosition());

        /*
         From the startPos column, find the DottedRules that have the same grammar at the right of the dot.
         Attached the found DottedRule in the current column.
         */
        while (head != null) {
            int dotPos = head.getDotPosition();
            Rule rule = head.getRule();
            String[] rhs = rule.getRhs();
            if (dotPos < rhs.length && match.equals(rhs[dotPos])) {
                DottedRule newDottedRule = new DottedRule(head.getStartPosition(),
                        head.getDotPosition() + 1,
                        rule,
                        head.getWeight() + dottedRule.getWeight());
                String attachCheckKey = genAttachCheckKey(colNum, newDottedRule);
                if (!check.containsKey(attachCheckKey)) {
                    newDottedRule.previous = head;
                    addToChart(newDottedRule, colNum);
                    check.put(attachCheckKey, null);
                } else {
                    //replace if the weight is better
                    replaceDottedRule(newDottedRule, colNum);
                }
            }
            head = head.next;
        }
    }


    /*
     Check the given DottedRule is better than the same rule in the specified column.
     */
    private boolean replaceDottedRule(DottedRule dottedRule, int colNum) {
        String key = String.valueOf(colNum) + "_" + dottedRule.toString();
        if (dottedRulePos.containsKey(key)) {
            DottedRule curr = dottedRulePos.get(key);
            if (dottedRule.getWeight() < curr.getWeight()) {
                curr.setWeight(dottedRule.getWeight());
                curr.previous = dottedRule.previous;
                return true;
            }
        } else {
            System.out.println("not found dottedRule for replacing");
        }
        return false;
    }

    /*
     If the dottedRule is not in the column, push it to the end of the list for the column
     */
    private void addToChart(DottedRule dottedRule, int colNum) {
        if (colNum >= chartTail.size()) {
            chartHead.add(dottedRule);
            chartTail.add(dottedRule);
            dottedRulePos.put(String.valueOf(chartHead.size()) + "_" + dottedRule.toString(), dottedRule);
        } else {
            DottedRule tail = chartTail.get(colNum);
            tail.next = dottedRule;
            chartTail.set(colNum, tail.next);
            dottedRulePos.put(String.valueOf(colNum) + "_" + dottedRule.toString(), dottedRule);
        }
//        printChart(colNum);
    }

    private void printChart(int colNum) {
        for (int i = colNum; i < chartHead.size(); i++) {
            System.out.println("-----" + Integer.toString(i) + "th column -----");
            DottedRule h = chartHead.get(i);
            while (h != null) {
                System.out.println(h.toString());
                h = h.next;
            }
        }
    }

    private String genPredictKey(DottedRule dottedRule) {
        return dottedRule.getRule().getRhs()[dottedRule.getDotPosition()];
    }

    private String genCheckKey(int colNum, DottedRule dottedRule, String predictKey) {
//        return Integer.toString(colNum) + "_" + Integer.toString(dottedRule.getStartPosition()) + "_" + Integer.toString(dottedRule.getDotPosition()) + "_" + predictKey;
        return Integer.toString(colNum) + "_" + predictKey;
    }

    private String genAttachCheckKey(int colNum, DottedRule dottedRule) {
        return Integer.toString(colNum) + "_" +
                Integer.toString(dottedRule.getStartPosition()) + "_" +
                Integer.toString(dottedRule.getDotPosition()) + "_" +
                dottedRule.getRule().getLhs() + "_" +
                Arrays.toString(dottedRule.getRule().getRhs());
    }
}
