/**
 * Created by Jinyi on 3/13/16.
 */
public class DottedRule {

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
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

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public DottedRule next;
    public DottedRule previous;
    private int startPosition;
    private int dotPosition;
    private Rule rule;
    private double weight;

    public DottedRule(int startPosition, int dotPosition, Rule rule, double weight) {
        this.startPosition = startPosition;
        this.dotPosition = dotPosition;
        this.rule = rule;
        this.weight = weight;
    }
}
