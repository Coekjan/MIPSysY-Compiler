import exceptions.IdentificationException;
import frontend.Token;
import frontend.Tokenizer;
import utils.SimpleIO;

import java.io.IOException;

public class Compiler {
    public static void tokenizerTest(String in, String out) throws IOException {
        try {
            SimpleIO.output(out, Tokenizer.lex(SimpleIO.input(in)),
                    l -> l.stream().map(Token::toString).reduce((x, y) -> x + "\n" + y).orElse(""));
        } catch (IdentificationException e) {
            System.err.println("Identification Exception");
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException {
        tokenizerTest("testfile.txt", "output.txt");
    }
}
