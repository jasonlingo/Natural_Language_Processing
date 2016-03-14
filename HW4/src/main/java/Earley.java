imoprt java.util.*;

/**
 * Created by Jason on 3/13/16.
 */
public class Earley {

    Map<String, List<Rule>> check;
    Map<String, List<Rule>> rules;
    Rule chartHead;
    Rule chartTail;

    public Earley() {
        this.check = new HashMap<>();
        this.chartHead = null;
        this.chartTail = null;
        this.rules = null;
    }


    public void addRules(Map<String, List<Rule>> rules) {
        this.rules = rules;
    }

    /*
     For each non-terminal 
     */
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
