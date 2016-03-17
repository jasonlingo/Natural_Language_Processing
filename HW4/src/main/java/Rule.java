public class Rule {

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String[] getRhs() {
        return rhs;
    }

    public void setRhs(String[] rhs) {
        this.rhs = rhs;
    }

    public String getLhs() {
        return lhs;
    }

    public void setLhs(String lhs) {
        this.lhs = lhs;
    }


    public Rule(double weight, String lhs, String[] rhs) {
        this.weight = weight;
        this.rhs = rhs;
        this.lhs = lhs;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Rule))
            return false;
        if (obj == this)
            return true;

        Rule other = (Rule) obj;
        return (this.lhs.equals(other.lhs) && this.rhs == other.rhs);
    }

    private double weight; // NP -> a majority . of N
    private String lhs;
    private String[] rhs;


}
