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
            String[] sen = orgSen.split();
            System.out.println(decode(sen));
        }
    }

    private String decode(String[] sen) {
        List<DottedRule> chartList = new ArrayList<DottedRule>();
        List<DottedRule> chartTail = new ArrayList<DottedRule>();

        // initialize root dottedRule
        List<Rule> root = this.rules.get("ROOT");

        DottedRule dummy = new DottedRule(0, null, 0);
        DottedRule curr = dummy;
        for (Rule r: root) {
            curr.next = new DottedRule(0, r, r.getWeight());
            curr = curr.next;
        }
        chartList.add(dummy.next);
        chartTail.add(curr);

        int colNum = 0;
        for (int i = 0; i < sen.length(); i++) {
            DottedRule head = chartList.get(i);
            while (head != null) {
                if (isComplete(head)) {
                    attach(head);
                } else {
                    if (!nextIsCat(head)) {
                        // The current rule (at the right of the dot) is a grammar that
                        // can be expanded to other grammar.
                        predict(colNum, head);
                    } else {
                        scan(head, sen); // Need pass sentence to it
                    }
                }
                head = head.next;
            }
            colNum++;
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
                // dottedRule.next = new DottedRule(colNum, 0, rule, rule.getWeight() + current.getWeight());
                DottedRule next = new DottedRule(colNum, 0, rule, rule.getWeight() + current.getWeight());
                addToChart(next, colNum);
                dottedPredictResult.add(next);
                // dottedRule = dottedRule.next;
            }

            check.put(checkKey, dottedPredictResult);
        }

    }


    private void scan(int colNum, DottedRule dottedRule, String[] words) {

        Rule rule = dottedRule.getRule();
        int dotPosition = dottedRule.getDotPosition();
        String terminal = rule.getRhs()[dotPosition];


        if (terminal.equals(words[colNum + 1])) {
            double weight = dottedRule.getWeight();

            //TODO is weight correct here?
            DottedRule scannedRule = new DottedRule(colNum, dotPosition + 1, rule, weight);

            addToChart(scannedRule, colNum + 1);

            // Update the HashMap check
            String checkKey = genCheckKey(colNum + 1, scannedRule, genPredictKey(scannedRule));

            //TODO: only one DottedRule for each checkKey entry?
            List<DottedRule> temp = new ArrayList<DottedRule>();
            temp.add(scannedRule);

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

        }



        //chartList.add(dottedRule);

    }

    private void attach(DottedRule dottedRule) {
        Rule rule = dottedRule.getRule();
        int dotPosition = dottedRule.getDotPosition();
        int startPos = dottedRule.getStartPosition();

        DottedRule head = chartList.get(startPos);



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
        return "" + Integer.toString(colNum) + dottedRule.getStartPosition() + dottedRule.getDotPosition() + predictKey;
    }

}
