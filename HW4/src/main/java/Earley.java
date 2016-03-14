import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jason on 3/13/16.
 */
public class Earley {

    Map<String, List<Rule>> check;
    Map<String, List<Rule>> rules;
    Rule chartHead;
    Rule chartTail;

    public Earley() {
        this.check = new HashMap<String, List<Rule>>();
        this.chartHead = null;
        this.chartTail = null;
        this.rules = null;
    }

    public void setRules(Map<String, List<Rule>> rules) {
        this.rules = rules;
//        chartHead = new DottedRule(0, );
    }

    public void parse(List<String> sentences) {
        if (this.rules == null) {
            System.out.println("No grammar!");
        }
    }

    private void predict() {

    }

    private void scan() {

    }

    private void attach() {

    }

    private void addToChart() {

    }

    /*
     Parse input sentence using Earley algorithm.
     */
    public void parse(String sen) {

    }

}
