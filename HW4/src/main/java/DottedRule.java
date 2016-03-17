/**
 * Created by Jinyi on 3/13/16.
 */
public class DottedRule {

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int dotPos = getDotPosition();
        String[] rhs = getRule().getRhs();
        for(int i = 0; i < rhs.length; i++) {
            if (i == dotPos) {
                sb.append(".");
            }
            sb.append(rhs[i]);
        }
        if (dotPos >= rhs.length) {
            sb.append(".");
        }
        return Integer.toString(getStartPosition()) + " " + getRule().getLhs() + " " + sb.toString();
    }

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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DottedRule))
            return false;
        if (obj == this)
            return true;

        DottedRule other = (DottedRule) obj;
        return (this.rule.equals(other) &&
                this.getDotPosition() == other.getDotPosition() &&
                this.getStartPosition() == other.getStartPosition());
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
