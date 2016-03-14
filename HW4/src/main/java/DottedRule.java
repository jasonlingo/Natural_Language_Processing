/**
 * Created by Jinyi on 3/13/16.
 */
public class DottedRule {

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getDotPosition() {
        return dotPosition;
    }

    public void setDotPosition(int dotPosition) {
        this.dotPosition = dotPosition;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public DottedRule next;


    public DottedRule(int dotPosition, Rule rule, int weight) {
        this.dotPosition = dotPosition;
        this.rule = rule;
        this.weight = weight;
        this.next = null;
    }

    private int dotPosition;
    private Rule rule;
    private int weight;

}
