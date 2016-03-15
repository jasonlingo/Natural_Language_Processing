import java.util.*;

/**
 * Created by Jason on 3/13/16.
 */
public class Earley {

    private Map<String, List<DottedRule>> check;  //for checking duplicated rule in one column
    private Map<String, List<Rule>> rules;
    private List<DottedRule> chartList;  // keep the first DottedRule of each column
    private List<DottedRule> chartTail;

    public Earley() {
        this.check = new HashMap<String, List<DottedRule>>();
        this.chartList = new ArrayList<DottedRule>();
        this.chartTail = new ArrayList<DottedRule>();
        this.rules = null;
    }

    public void setRules(Map<String, List<Rule>> rules) {
        this.rules = rules;
    }

    public void parse(List<String> sentences) {
        if (this.rules == null) {
            System.out.println("No grammar!");
        }

        for (String orgSen: sentences) {
            String[] sen = orgSen.split(" ");
            System.out.println(decode(sen));
        }
    }

    private String decode(String[] sen) {

        List<DottedRule> chartList = new ArrayList<DottedRule>();
        List<DottedRule> chartTail = new ArrayList<DottedRule>();

        // initialize root dottedRule
        List<Rule> root = this.rules.get("ROOT");

        DottedRule dummy = new DottedRule(0, 0, null, 0);
        DottedRule curr = dummy;
        for (Rule r: root) {
            curr.next = new DottedRule(0, 0, r, r.getWeight());
            curr = curr.next;
        }
        chartList.add(dummy.next);
        chartTail.add(curr);


        for (int i = 0; i < sen.length; i++) {
            DottedRule head = chartList.get(i);
            while (head != null) {
                if (isComplete(head)) {
                    attach(head, i);
                } else {
                    if (!nextIsCat(head)) {
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
        curr = chartList.get(chartList.size() - 1);
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

        System.out.println("best weight:" + Double.toString(bestScore));

        return "";

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

        DottedRule current = dottedRule;

        // the name of lhs for prediciton, which is the rhs pointed by the dotPosition
        String predictKey = genPredictKey(current);

        // unique key for each entry in the Early chart
        String checkKey = genCheckKey(colNum, dottedRule, predictKey);

        // Check if the predicted rule is already in the column
        if (!check.containsKey(checkKey)) {

            List<Rule> predictResult = rules.get(predictKey);
            List<DottedRule> dottedPredictResult = new ArrayList<DottedRule>();

            for (Rule rule : predictResult) {
                // the initial dotted rule predicted should be 0
                // once the dot moves to the right, add the weight of rules
                // on the left
                DottedRule next = new DottedRule(colNum, 0, rule, 0);
                addToChart(next, colNum);
                dottedPredictResult.add(next);
                // dottedRule = dottedRule.next;
            }

            check.put(checkKey, dottedPredictResult);
        }

    }


    private void scan(int colNum, DottedRule dottedRule, String[] words) {

        if (colNum + 1 >= words.length) {
            return;
        }

        Rule rule = dottedRule.getRule();
        int dotPosition = dottedRule.getDotPosition(); // Is the position always 0?
        // no for cases like NP -> a majority of N?
        String[] rhs = rule.getRhs();
        ArrayList<String> terminals = new ArrayList<String>();

        while (!rules.containsKey(rhs[dotPosition])) {
            terminals.add(rhs[dotPosition]);
            dotPosition++;

        }
        //String terminal = rule.getRhs()[dotPosition];

        // check if all terminals match the sentence
        for (int i = 0; i < terminals.size(); i++) {
            if (!terminals.get(i).equals(words[colNum + i + 1])) {
                return;
            }
        }

        //if (terminal.equals(words[colNum + 1])) {

        DottedRule scannedRule = new DottedRule(colNum, dotPosition + 1, rule, rule.getWeight());

        addToChart(scannedRule, colNum + 1);

        // Update the HashMap check
        String checkKey = genCheckKey(colNum + 1, scannedRule, genPredictKey(scannedRule));

        if (!check.containsKey(checkKey)) {
            List<DottedRule> temp = new ArrayList<DottedRule>();
            temp.add(scannedRule);
            check.put(checkKey, temp);
        }
        else {
            List<DottedRule> temp = check.get(checkKey);
            temp.add(scannedRule);
            check.put(checkKey, temp);
        }

        //}


    }

    private void attach(DottedRule dottedRule, int colNum) {
        List<DottedRule> attached = new ArrayList<DottedRule>();
        String match = dottedRule.getRule().getLhs();
        int dotPos = dottedRule.getDotPosition();
        DottedRule head = chartList.get(dottedRule.getStartPosition());


        /*
         From the startPos column, find the DottedRules that have the same grammar at the right of the dot.
         Attached the found DottedRule in the current column.
         */
        while (head != null) {
            Rule rule = head.getRule();
            String[] rhs = rule.getRhs();
            if (match.equals(rhs[dotPos])) {
                DottedRule newDottedRule = new DottedRule(head.getStartPosition(),
                        head.getDotPosition() + 1,
                        rule,
                        head.getWeight() + dottedRule.getWeight());
                addToChart(newDottedRule, colNum);
            }
            head = head.next;
        }
    }


    /*
     If the dottedRule is not in the column, push it to the end of the list for the column
     */
    private void addToChart(DottedRule dottedRule, int colNum) {
        if (colNum >= chartTail.size()) {
            chartList.add(dottedRule);
            chartTail.add(dottedRule);
        } else {
            DottedRule tail = chartTail.get(colNum);
            tail.next = dottedRule;
            chartTail.set(colNum, tail.next);
        }
    }


    private String genPredictKey(DottedRule dottedRule) {
        return dottedRule.getRule().getRhs()[dottedRule.getDotPosition()];
    }


    private String genCheckKey(int colNum, DottedRule dottedRule, String predictKey) {
        return Integer.toString(colNum) + "_" + Integer.toString(dottedRule.getStartPosition()) + "_" + Integer.toString(dottedRule.getDotPosition()) + "_" + predictKey;
    }

}
