import java.util.*;

/**
 * Created by Jason on 3/23/16.
 */
public class Earley6 {

    private Set<String> check;  //for checking duplicated rule in one column
    private Map<String, List<Rule>> rules;
    private List<DottedRule> chartHead;           // keep the first DottedRule of each column
    private List<DottedRule> chartTail;
    private Map<String, DottedRule> dottedRulePos;// record the position of DottedRules

    private Map<String, List<Rule>> tempRules;
    private StringBuilder sb;
    private Map<String, DottedRule> attachMap;

    public Earley6() {
        this.check = new HashSet<String>();
        this.chartHead = new ArrayList<DottedRule>();
        this.chartTail = new ArrayList<DottedRule>();
        this.rules = null;
        this.dottedRulePos = new HashMap<String, DottedRule>();
        this.sb = new StringBuilder();
        this.attachMap = new HashMap<String, DottedRule>();

    }

    public void setRules(Map<String, List<Rule>> rules) {
        this.rules = rules;
    }

    public void parse(List<String> sentences) {
        // for speed up purpose, delete unused terminal
        tempRules = new HashMap<String, List<Rule>>(rules);

        for (String orgSen : sentences) {
//            System.out.println(orgSen);
            String[] sen = orgSen.split(" ");

            // initialization
            sb.setLength(0);
            chartHead.clear();
            chartTail.clear();
            check.clear();
            dottedRulePos.clear();

            rules = new HashMap<String, List<Rule>>(tempRules);
            deleteUnusedTerminals(sen, rules);

//            long startTime = System.nanoTime();
            System.out.println(decode(sen));
//            long endTime = System.nanoTime();
//            System.out.println("Running time: " + (endTime - startTime)/1000000 + " ms");

        }
    }

    private String decode(String[] sen) {
//        System.out.println(Arrays.toString(sen));

        // initialize root dottedRule
        List<Rule> root = this.rules.get("ROOT");

        DottedRule dummy = new DottedRule(0, 0, null, 0);
        DottedRule curr = dummy;
        for (Rule r : root) {
            curr.next = new DottedRule(0, 0, r, r.getWeight());
            curr = curr.next;
        }
        chartHead.add(dummy.next);
        dottedRulePos.put(genAttachCheckKey(0, dummy.next), dummy.next);
        chartTail.add(curr);
        dottedRulePos.put(genAttachCheckKey(0, curr), curr);

        for (int i = 0; i <= sen.length; i++) {
            if (i >= chartHead.size()) {
                return "None";
            }
            DottedRule head = chartHead.get(i);
            attachMap.clear();
            dottedRulePos.clear();
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

            printEntry(bestParse, true);
            System.out.println(sb.toString().trim());

//            System.out.println();
//            System.out.println("best weight:" + String.valueOf(bestScore));
            System.out.println(String.valueOf(bestScore));
            return "";
        }
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
        String checkKey = genCheckKey(colNum, predictKey);

        // Check if the predicted rule is already in the column
        if (!check.contains(checkKey)) {

            List<Rule> predictResult = rules.get(predictKey);
            List<DottedRule> dottedPredictResult = new ArrayList<DottedRule>();
            for (Rule rule : predictResult) {
                // the initial dotted rule predicted should be 0
                // once the dot moves to the right, add the weight of rules
                // on the left
                DottedRule next = new DottedRule(colNum, 0, rule, rule.getWeight());
                next.previous = dottedRule;
                addToChart(next, colNum);
                dottedPredictResult.add(next);
            }

            check.add(checkKey);
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

        String scanKey = genScanKey(dottedRule);
        dottedRulePos.put(scanKey, scannedRule);

        // Add second backpoint
        scannedRule.previousColumn = dottedRule;

        addToChart(scannedRule, colNum + 1);
    }

    private void attach(DottedRule dottedRule, int colNum) {
        String match = dottedRule.getRule().getLhs();
        DottedRule head = chartHead.get(dottedRule.getStartPosition());
        /*
         From the startPos column, find the DottedRules that have the same grammar at the right of the dot.
         Attached the found DottedRule in the current column.
         */
        //TODO: use hashmap instead of loop from the head to the tail
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

                // Track the previous column
                newDottedRule.previousColumn = head;
                newDottedRule.previous = dottedRule;

                // Keep track of previous and previousColumn pair for updating weights
                String preColKey = genPreColKey(colNum, dottedRule, newDottedRule);
                attachMap.put(preColKey, head);

                if (!check.contains(attachCheckKey)) {
                    addToChart(newDottedRule, colNum);
                    check.add(attachCheckKey);
                    dottedRule.childCnt++;
                    dottedRulePos.put(genChildKey(colNum, dottedRule, dottedRule.childCnt), newDottedRule);
                } else {
                    //replace if the weight is better
                    replaceDottedRule(dottedRule, newDottedRule, colNum);
                }

            }
            head = head.next;
        }
    }


    /*
     Check the given DottedRule is better than the same rule in the specified column.
     */
    private boolean replaceDottedRule(DottedRule org, DottedRule dottedRule, int colNum) {
        String key = genAttachCheckKey(colNum, dottedRule);
        if (dottedRulePos.containsKey(key)) {
            DottedRule curr = dottedRulePos.get(key);

            // keep track of the child
            org.childCnt++;
            dottedRulePos.put(genChildKey(colNum, org, org.childCnt), curr);

            if (dottedRule.getWeight() < curr.getWeight()) {
                curr.setWeight(dottedRule.getWeight());
                updateChildWeight(colNum, curr);
                updateScanRuleWeight(curr);

                curr.previous = dottedRule.previous;
                curr.previousColumn = dottedRule.previousColumn;
                return true;
            }
        } else {
            // only for checking key error
            System.out.println("not found dottedRule for replacing");
        }
        return false;
    }

    private void updateScanRuleWeight(DottedRule previousCol) {
        String scanKey = previousCol.toString();
        DottedRule scanRule = dottedRulePos.get(scanKey);
        if (scanRule != null) {
            scanRule.setWeight(previousCol.getWeight());
        }

    }

    private boolean updateChildWeight(int colNum, DottedRule previous) {
        for (int i = 1; i <= previous.childCnt; i++) {
            String childKey = genChildKey(colNum, previous, i);
            DottedRule child = dottedRulePos.get(childKey);
            String preColKey = genPreColKey(colNum, previous, child);
            DottedRule preCol = attachMap.get(preColKey);

            double newWeight = preCol.getWeight() + previous.getWeight();
            if (newWeight < child.getWeight()) {
                child.setWeight(newWeight);
                child.previousColumn = preCol;
                child.previous = previous;
                updateScanRuleWeight(child);
                if (child.childCnt > 0) {
                    updateChildWeight(colNum, child);
                }
            }
        }

        return true;
    }

    /*
     If the dottedRule is not in the column, push it to the end of the list for the column
     */
    private void addToChart(DottedRule dottedRule, int colNum) {
        if (colNum >= chartTail.size()) {
            chartHead.add(dottedRule);
            chartTail.add(dottedRule);
            dottedRulePos.put(genAttachCheckKey(chartHead.size(), dottedRule), dottedRule);
        } else {
            DottedRule tail = chartTail.get(colNum);
            tail.next = dottedRule;
            chartTail.set(colNum, tail.next);
            dottedRulePos.put(genAttachCheckKey(colNum, dottedRule), dottedRule);
        }
    }


    private void printEntry(DottedRule bestParse, boolean start) {
        if (bestParse.previousColumn == null) {
            sb.append(" (");
            sb.append(bestParse.getRule().getLhs());
        } else {
            printEntry(bestParse.previousColumn, false);
            if (bestParse.previous == null) {
                int i = bestParse.getDotPosition() - 1;
                if (i >= 0) {
                    sb.append(" ");
                    sb.append(bestParse.getRule().getRhs()[i]);
                }
            } else {
                printEntry(bestParse.previous, false);
                sb.append(")");
            }
        }
        if (start) {
            sb.append(")");
        }
    }

//    private void printChart(int colNum) {
//        for (int i = colNum; i < chartHead.size(); i++) {
//            System.out.println("-----" + Integer.toString(i) + "th column -----");
//            DottedRule h = chartHead.get(i);
//            while (h != null) {
//                System.out.println(h.toString());
//                if (h.previousColumn != null) {
////                    System.out.print("  previous column is " + h.previousColumn.toString());
//                    if (h.previous != null) {
////                        System.out.println();
////                        System.out.print("  previous is " + h.previous.toString());
//                    }
//                    System.out.println();
//                }
//                h = h.next;
//            }
//        }
//    }

    public void deleteUnusedTerminals(String[] sen, Map<String, List<Rule>> rules) {
        List<String> wordList = Arrays.asList(sen);

        for(Map.Entry<String, List<Rule>> entry : rules.entrySet()) {
            List<Rule> ruleList = new ArrayList<Rule>(entry.getValue());
            for (int i = 0; i < ruleList.size(); i++) {
                Rule rule = ruleList.get(i);
                if (rule.getRhs().length == 1 && !rules.containsKey(rule.getRhs()[0])) {
                    if (!wordList.contains(rule.getRhs()[0])) {
                        ruleList.remove(i);
                        i--;
                    }
                }
            }
            rules.put(entry.getKey(), ruleList);
        }
    }

    private String genPredictKey(DottedRule dottedRule) {
        return dottedRule.getRule().getRhs()[dottedRule.getDotPosition()];
    }

    private String genCheckKey(int colNum, String predictKey) {
        return String.valueOf(colNum) + "_" + predictKey;
    }

    private String genAttachCheckKey(int colNum, DottedRule dottedRule) {
        return String.valueOf(colNum) + "_" + dottedRule.toString();
    }

    private String genPreColKey(int colNum, DottedRule dottedRule, DottedRule newDottedRule) {
//        return String.valueOf(colNum) + "_" + dottedRule.toString() + "_" + newDottedRule.toString();
        return dottedRule.toString() + "_" + newDottedRule.toString();
    }

    private String genChildKey(int colNum, DottedRule previous, int childNum) {
//        return String.valueOf(colNum) + "_" + previous.toString() + "_c" + String.valueOf(childNum);
        return previous.toString() + "_c" + String.valueOf(childNum);
    }

    private String genScanKey(DottedRule previousCol) {
        return previousCol.toString();
    }
}