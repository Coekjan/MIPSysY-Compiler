import exceptions.IdentificationException;
import exceptions.ParserException;
import frontend.*;
import utils.SimpleIO;

import java.io.IOException;
import java.util.Arrays;

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

    public static void parserTest(String in, String out) throws IOException {
        try {
            TokenSupporter supporter = new TokenSupporter(Tokenizer.lex(SimpleIO.input(in)));
            ParserUnit compUnit = ParserController.CompUnit.parse(supporter);
            SimpleIO.output(out, compUnit, parserUnit -> Arrays.stream(parserUnit.toString()
                    .split("\n"))
                    .filter(s -> !s.equals("<BlockItem>") && !s.equals("<Decl>") && !s.equals("<BType>"))
                    .reduce((s1, s2) -> s1 + "\n" + s2).orElse(""));
        } catch (IdentificationException e) {
            System.err.println("Identification Exception");
            System.exit(1);
        } catch (ParserException e) {
            System.err.println("Parser Exception");
            e.printStackTrace();
            System.exit(2);
        }
    }

    public static void transTest(String in, String out) throws IOException {
        try {
            TokenSupporter supporter = new TokenSupporter(Tokenizer.lex(SimpleIO.input(in)));
            ParserUnit compUnit = ParserController.CompUnit.parse(supporter);
            GlobalNode globalNode = SyntaxTreeBuilder.fetch(compUnit);
            SimpleIO.output(out, globalNode, GlobalNode::toString);
        } catch (IdentificationException e) {
            System.err.println("Identification Exception");
            System.exit(1);
        } catch (ParserException e) {
            System.err.println("Parser Exception");
            e.printStackTrace();
            System.exit(2);
        }
    }

    public static void main(String[] args) throws IOException {
        transTest("testfile.txt", "output.txt");
    }
}
