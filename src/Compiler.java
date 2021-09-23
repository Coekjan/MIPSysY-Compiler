import exceptions.IdentificationException;
import frontend.Token;
import frontend.Tokenizer;

import java.io.*;
import java.util.List;

public class Compiler {
    public static void tokenizerTest() throws IOException {
        try {
            final List<Token> tokens = new Tokenizer(new File("testfile.txt")).getTokens();
            final PrintWriter out = new PrintWriter("output.txt");
            out.println(tokens.stream()
                    .map(Token::toString)
                    .reduce((s1, s2) -> s1 + "\n" + s2).orElse(""));
            out.close();
        } catch (IdentificationException e) {
            System.err.println("Identification Exception");
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException {
        tokenizerTest();
    }
}
