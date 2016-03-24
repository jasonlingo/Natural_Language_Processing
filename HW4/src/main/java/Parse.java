import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Parse {
    HashMap<String, List<Rule>> grammars = new HashMap<String, List<Rule>> ();
//    HashMap<String, List<DottedRule>> entries = new HashMap<String, List<DottedRule>>();
    List<String> sentences = new ArrayList<String>();


    public Parse(String grFile, String senFile) throws IOException {
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
        Parse p = new Parse(args[0], args[1]);

        Earley5 e = new Earley5();
        e.setRules(p.grammars);
        e.parse(p.sentences);

        //test IO

//        for (Map.Entry<String, List<Rule>> entry : p.grammars.entrySet()) {
//
//            List<Rule> tmp = entry.getValue();
//            for (Rule r : tmp) {
//                // System.out.print(entry.getKey() + "\t");
//                System.out.print(r.getWeight() + "\t" + r.getLhs());
//                for (String s: r.getRhs())
//                    System.out.print(" " + s);
//                System.out.print("\n");
//            }
//        }

        
    }
}
