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

<<<<<<< HEAD
=======
//    public DottedRule(int dotPosition, Rule rule, int weight) {
//        this.dotPosition = dotPosition;
//        this.rule = rule;
//        this.weight = weight;
//        this.next = null;
//    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

>>>>>>> master

    public DottedRule next;
    private int startPosition;
    private int dotPosition;
    private Rule rule;

    public DottedRule(int startPosition, int dotPosition, Rule rule, double weight) {
        this.startPosition = startPosition;
        this.dotPosition = dotPosition;
        this.rule = rule;
        this.weight = weight;
    }

<<<<<<< HEAD
    private int dotPosition;
    private Rule rule;
    private int weight;
    public DottedRule next;
=======
    private double weight;
>>>>>>> master

}
