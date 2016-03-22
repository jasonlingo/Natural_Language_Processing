import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by Jason on 3/13/16.
 */
public class Earley3 {

    private Set<String> check;  //for checking duplicated rule in one column
    private Map<String, List<Rule>> rules;
    private List<DottedRule> chartHead;           // keep the first DottedRule of each column
    private List<DottedRule> chartTail;
    private Map<String, DottedRule> dottedRulePos;// record the position of DottedRules
    private Set<String> attachMap; // for extra credit of problem 2

    private Map<String, List<Rule>> tempRules;

    public Earley3() {
        this.check = new HashSet<String>();
        this.chartHead = new ArrayList<DottedRule>();
        this.chartTail = new ArrayList<DottedRule>();
        this.rules = null;
        this.dottedRulePos = new HashMap<String, DottedRule>();
        this.attachMap = new HashSet<String>();
    }

    public void setRules(Map<String, List<Rule>> rules) {
        this.rules = rules;
    }

    public void parse(List<String> sentences) {
        if (this.rules == null) {
            System.out.println("No grammar!");
        }

        tempRules = new HashMap<String, List<Rule>>(rules);
        for (String orgSen : sentences) {
            String[] sen = orgSen.split(" ");


            rules = new HashMap<String, List<Rule>>(tempRules);
            deleteUnusedTerminals(sen, rules);

            long startTime = System.nanoTime();
            System.out.println(decode(sen));
            long endTime = System.nanoTime();
            System.out.println("Running time: " + (endTime - startTime)/1000000 + " ms");

        }
    }

    private String decode(String[] sen) {
        System.out.println(Arrays.toString(sen));

        chartHead.clear();
        chartTail.clear();
        check.clear();
        dottedRulePos.clear();
        attachMap.clear();

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
            printEntry(bestParse, true);

            System.out.println();
            System.out.println("weight:" + Double.toString(bestScore));
            System.out.println("best possibility:" + Math.pow(2,-bestScore));
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
        String checkKey = genCheckKey(colNum, dottedRule, predictKey);


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

        // Add second backpoint
        scannedRule.previousColumn = dottedRule;

        addToChart(scannedRule, colNum + 1);
    }

    private void attach(DottedRule dottedRule, int colNum) {
        attachMap.add(colNum + dottedRule.getRule().getLhs());
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
                DottedRule toBeAttached = new DottedRule(head.getStartPosition(),
                        head.getDotPosition() + 1,
                        rule,
                        head.getWeight() + dottedRule.getWeight());
                String attachCheckKey = genAttachCheckKey(colNum, toBeAttached);
                // Track the previous column
                toBeAttached.previousColumn = head;
                toBeAttached.previous = dottedRule;

                // Extra credit for problem 2

//                if (attachMap.containsKey(colNum + "_" + toBeAttached.getRule().getLhs())) {
//                    attach(toBeAttached, colNum);
//                }
//                else {
//                    List<DottedRule> temp = new ArrayList<DottedRule>();
//                    temp.add(head);
//                    attachMap.put( colNum + "_" + dottedRule.getRule().getLhs(), temp);
//                }

                // End of extra credit for problem 2

                if (!check.contains(attachCheckKey)) {
                    addToChart(toBeAttached, colNum);
                    check.add(attachCheckKey);

                } else {
                    //replace if the weight is better
                    replaceDottedRule(toBeAttached, colNum);

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
                // may have to modify here
                curr.setWeight(dottedRule.getWeight());
                curr.previous = dottedRule.previous;
                curr.previousColumn = dottedRule.previousColumn;

                if (attachMap.contains(colNum + dottedRule.getRule().getLhs())) {
                    attach(dottedRule, colNum);
                }
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
    }


    private void printEntry(DottedRule bestParse, boolean start) {
        if (bestParse.previousColumn == null) {
            System.out.print("(" + bestParse.getRule().getLhs() + " ");
        } else {
            printEntry(bestParse.previousColumn, false);
            if (bestParse.previous == null) {
                int i = bestParse.getDotPosition() - 1;
                if (i >= 0) {
                    System.out.print(" " + bestParse.getRule().getRhs()[i] + " ");
                }
            } else {
                printEntry(bestParse.previous, false);
                System.out.print(")");
            }
        }
        if (start) {
            System.out.println(")");
        }
    }

    private void printChart(int colNum) {
        for (int i = colNum; i < chartHead.size(); i++) {
            System.out.println("-----" + Integer.toString(i) + "th column -----");
            DottedRule h = chartHead.get(i);
            while (h != null) {
                System.out.println(h.toString());
                if (h.previousColumn != null) {
//                    System.out.print("  previous column is " + h.previousColumn.toString());
                    if (h.previous != null) {
//                        System.out.println();
//                        System.out.print("  previous is " + h.previous.toString());
                    }
                    System.out.println();
                }
                h = h.next;
            }
        }
    }

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
