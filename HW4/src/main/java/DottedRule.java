import java.util.Arrays;

/**
 * Created by Jinyi on 3/13/16.
 */
public class DottedRule {


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(getStartPosition()));
        sb.append(" ");
        sb.append(String.valueOf(getDotPosition()));
        sb.append(" ");
        sb.append(getRule().getLhs());
        sb.append(" ");
        sb.append(Arrays.toString(rule.getRhs()));
        return sb.toString();
    }

//    private void genKey() {
//        StringBuilder sb = new StringBuilder();
//        sb.append(String.valueOf(getStartPosition()));
//        sb.append(" ");
//        sb.append(String.valueOf(getDotPosition()));
//        sb.append(" ");
//        sb.append(getRule().getLhs());
//        sb.append(" ");
//        sb.append(Arrays.toString(rule.getRhs()));
//
////        int dotPos = getDotPosition();
////        String[] rhs = rule.getRhs();
////        for (int i = 0; i < rhs.length; i++) {
////            if (i == dotPos) {
////                sb.append(".");
////            }
////
////            sb.append(rhs[i]);
////        }
////        if (dotPos >= rhs.length) {
////            sb.append(".");
////        }
////        sb.insert(0, " ");
////        sb.insert(0, getRule().getLhs());
////        sb.insert(0, " ");
////        sb.insert(0, String.valueOf(getStartPosition()));
//        this.key = sb.toString();
//
//    }

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

    public DottedRule next;       //next dottedRule in the same column
    public DottedRule previous;   //parsed from previous dottedRule
    public DottedRule previousColumn;   // Second backpointer to track where the attached rule
                                        // is from (which entry in the previous column)
    public int childCnt;
    private int startPosition;
    private int dotPosition;
    private Rule rule;
    private double weight;
//    private String key;


    public DottedRule(int startPosition, int dotPosition, Rule rule, double weight) {
        this.startPosition = startPosition;
        this.dotPosition = dotPosition;
        this.rule = rule;
        this.weight = weight;
        this.childCnt = 0;
//        if (this.rule != null) {
//            genKey();
//        }
    }
}
