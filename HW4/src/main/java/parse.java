import java.io.*;
import java.util.*;

public class Parse {
    HashMap<String, List<Rule>> grammars = new HashMap<String, List<Rule>> ();
    HashMap<String, List<DottedRule>> entries = new HashMap<String, List<DottedRule>>();
    List<String> sentences = new ArrayList<String>();


    public Parse(String grFile, String senFile) throws IOException {
        //Initialize input sentence as an array
        BufferedReader br = new BufferedReader(new FileReader(senFile));
        try {
            String line = br.readLine();

            while (line != null) {
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

//                sentences.add(line);
                line = br.readLine();
            }

        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            br.close();
        }


    }

    public static  void main(String[] args) throws IOException {
//        System.out.println(System.getProperty("user.dir"));
        Parse p = new Parse(args[0], args[1]);
        for (String s:p.sentences) {
            System.out.println(s);

        }
    }
}
