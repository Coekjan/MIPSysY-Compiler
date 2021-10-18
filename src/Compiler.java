import exceptions.SysYException;
import frontend.*;
import utils.Pair;
import utils.SimpleIO;

import java.io.IOException;
import java.util.*;

public class Compiler {
    public static void tokenizerTest(String in, String out) throws IOException {
        try {
            SimpleIO.output(out, Tokenizer.lex(SimpleIO.input(in)),
                    l -> l.stream().map(Token::toString).reduce((x, y) -> x + "\n" + y).orElse(""));
        } catch (SysYException e) {
            System.out.println(e.stringify());
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
        } catch (SysYException e) {
            System.out.println(e.stringify());
            System.exit(1);
        } catch (ParserController.ParseError e) {
            System.err.println("Expect " + e.type);
            System.exit(1);
        }
    }

    public static void transTest(String in, String out) throws IOException {
        try {
            TokenSupporter supporter = new TokenSupporter(Tokenizer.lex(SimpleIO.input(in)));
            ParserUnit compUnit = ParserController.CompUnit.parse(supporter);
            GlobalNode globalNode = SyntaxTreeBuilder.fetch(compUnit);
            globalNode.check(new SymbolTable(Collections.emptyMap(), new HashMap<String, FuncDefNode>() {{
                put("getint", new FuncDefNode(true, "getint", 0, Collections.emptyList(),
                        new FuncBlockNode(Collections.emptyList(), true, 0)));
            }}), false);
            SimpleIO.output(out, new ArrayList<Pair<Integer, SysYException.Code>>(Tokenizer.errors) {{
                addAll(ParserController.errors);
                addAll(SyntaxNode.errors);
            }}, a -> a.stream().distinct().sorted(Comparator.comparing(o -> o.first))
                    .map(p -> p.first + " " + p.second).reduce((x, y) -> x + "\n" + y).orElse(""));
        } catch (SysYException e) {
            System.out.println(e.stringify());
            System.exit(1);
        } catch (ParserController.ParseError e) {
            System.err.println("Expect " + e.type);
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException {
        transTest("testfile.txt", "error.txt");
    }
}
