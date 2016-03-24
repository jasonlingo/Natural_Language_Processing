import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Parse2 {
    HashMap<String, List<Rule>> grammars = new HashMap<String, List<Rule>> ();
//    HashMap<String, List<DottedRule>> entries = new HashMap<String, List<DottedRule>>();
    List<String> sentences = new ArrayList<String>();


    public Parse2(String grFile, String senFile) throws IOException {
        //Initialize input sentence as an array
        BufferedReader br = new BufferedReader(new FileReader(senFile));
        try {
            String line = br.readLine();

            while (line != null) {
                if (line.length() > 0)
                    sentences.add(line);
                line = br.readLine();
            }

        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            br.close();
        }

        //Initialize grammars
        br = new BufferedReader(new FileReader(grFile));
        try {
            String line = br.readLine();

            while (line != null) {
                if (!line.equals("")) {
                    String[] splits = line.split("\t");
                    double weight = -(Math.log(Double.parseDouble(splits[0])) / Math.log(2));
                    String lhs = splits[1];
                    String[] rhs = splits[2].split(" ");
                    if (!grammars.containsKey(lhs)) {
                        ArrayList<Rule> tmp = new ArrayList<Rule>();
                        tmp.add(new Rule(weight, lhs, rhs));
                        grammars.put(lhs, tmp);
                    } else {
                        List<Rule> tmp = grammars.get(lhs);
                        tmp.add(new Rule(weight, lhs, rhs));
                    }
                }
                line = br.readLine();
            }

        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            br.close();
        }
    }

    public static  void main(String[] args) throws IOException {
        Parse2 p = new Parse2(args[0], args[1]);
        Earley2 e = new Earley2();
        e.setRules(p.grammars);
        e.parse(p.sentences);


        
    }
}
