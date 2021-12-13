import backend.LoopScheduler;
import backend.Translator;
import exceptions.SysYException;
import frontend.*;
import midend.*;
import utils.Pair;
import utils.SimpleIO;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Compiler {
    public static boolean opt = true;

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
            globalNode.check(new SymbolTable(Collections.emptyMap(), new HashMap<>()), false);
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

    public static void constCalTest(String in, String out) throws IOException {
        try {
            final TokenSupporter supporter = new TokenSupporter(Tokenizer.lex(SimpleIO.input(in)));
            final ParserUnit compUnit = ParserController.CompUnit.parse(supporter);
            final GlobalNode globalNode = SyntaxTreeBuilder.fetch(compUnit);
            final SymbolTable initSymbols = new SymbolTable(Collections.emptyMap(), new HashMap<>());
            globalNode.check(initSymbols, false);
            final List<Pair<Integer, SysYException.Code>> errors =
                    new ArrayList<Pair<Integer, SysYException.Code>>(Tokenizer.errors) {{
                        addAll(ParserController.errors);
                        addAll(SyntaxNode.errors);
            }};
            if (!errors.isEmpty()) {
                SimpleIO.output(out, errors, a -> a.stream().distinct().sorted(Comparator.comparing(o -> o.first))
                        .map(p -> p.first + " " + p.second).reduce((x, y) -> x + "\n" + y).orElse(""));
                return;
            }
            final GlobalNode globalNodeWithoutConstExp = (GlobalNode) globalNode.simplify(initSymbols).second;
            SimpleIO.output(out, globalNodeWithoutConstExp.toString(), s -> s);
        } catch (SysYException e) {
            System.out.println(e.stringify());
            System.exit(1);
        } catch (ParserController.ParseError e) {
            System.err.println("Expect " + e.type);
            System.exit(1);
        }
    }

    public static void irTest(String in, String out) throws IOException {
        try {
            final TokenSupporter supporter = new TokenSupporter(Tokenizer.lex(SimpleIO.input(in)));
            final ParserUnit compUnit = ParserController.CompUnit.parse(supporter);
            final GlobalNode globalNode = SyntaxTreeBuilder.fetch(compUnit);
            final SymbolTable initSymbols = new SymbolTable(Collections.emptyMap(), new HashMap<>());
            globalNode.check(initSymbols, false);
            final List<Pair<Integer, SysYException.Code>> errors =
                    new ArrayList<Pair<Integer, SysYException.Code>>(Tokenizer.errors) {{
                        addAll(ParserController.errors);
                        addAll(SyntaxNode.errors);
                    }};
            if (!errors.isEmpty()) {
                SimpleIO.output(out, errors, a -> a.stream().distinct().sorted(Comparator.comparing(o -> o.first))
                        .map(p -> p.first + " " + p.second).reduce((x, y) -> x + "\n" + y).orElse(""));
                return;
            }
            final GlobalNode globalNodeWithoutConstExp = (GlobalNode) globalNode.simplify(initSymbols).second;
            final LabelTable lt = new LabelTable();
            final Pair<IntermediateCode, IntermediateCode> global = globalNodeWithoutConstExp.iCode(lt,
                    new SymbolTable(Collections.emptyMap(),
                            new HashMap<>()), null, null, 0).second;
            final IntermediateCode head = optimize(lt, global).first;
            final IntermediateVirtualMachine machine = new IntermediateVirtualMachine(head);
            IntermediateCode p = head;
            final StringJoiner make = new StringJoiner("\n");
            while (p != null) {
                final Optional<List<String>> labels = lt.find(p);
                if (labels.isPresent()) {
                    for (String s : labels.get()) {
                        make.add(s + ":");
                    }
                }
                if (!(p instanceof Nop)) {
                    make.add(p.toString());
                }
                p = p.getNext();
            }
            SimpleIO.output(out, make, StringJoiner::toString);
            machine.run(lt);
        } catch (SysYException e) {
            System.out.println(e.stringify());
            System.exit(1);
        } catch (ParserController.ParseError e) {
            System.err.println("Expect " + e.type);
            System.exit(1);
        }
    }

    public static void mipsTest(String in, String out) throws IOException {
        try {
            final TokenSupporter supporter = new TokenSupporter(Tokenizer.lex(SimpleIO.input(in)));
            final ParserUnit compUnit = ParserController.CompUnit.parse(supporter);
            final GlobalNode globalNode = SyntaxTreeBuilder.fetch(compUnit);
            final SymbolTable initSymbols = new SymbolTable(Collections.emptyMap(), new HashMap<>());
            globalNode.check(initSymbols, false);
            final List<Pair<Integer, SysYException.Code>> errors =
                    new ArrayList<Pair<Integer, SysYException.Code>>(Tokenizer.errors) {{
                        addAll(ParserController.errors);
                        addAll(SyntaxNode.errors);
                    }};
            if (!errors.isEmpty()) {
                SimpleIO.output(out, errors, a -> a.stream().distinct().sorted(Comparator.comparing(o -> o.first))
                        .map(p -> p.first + " " + p.second).reduce((x, y) -> x + "\n" + y).orElse(""));
                return;
            }
            final GlobalNode globalNodeWithoutConstExp = (GlobalNode) globalNode.simplify(initSymbols).second;
            final LabelTable lt = new LabelTable();
            final Pair<IntermediateCode, IntermediateCode> global =
                    globalNodeWithoutConstExp.iCode(lt, new SymbolTable(Collections.emptyMap(),
                            new HashMap<>()), null, null, 0).second;
            final IntermediateCode head = optimize(lt, global).first;
            IntermediateCode p = head;
            final StringJoiner make = new StringJoiner("\n");
            while (p != null) {
                final Optional<List<String>> labels = lt.find(p);
                if (labels.isPresent()) {
                    for (String s : labels.get()) {
                        make.add(s + ":");
                    }
                }
                if (!(p instanceof Nop)) {
                    make.add(p.toString());
                }
                p = p.getNext();
            }
            SimpleIO.output("ir.txt", make, StringJoiner::toString);
            final Translator translator = new Translator(head, new LoopScheduler(), lt);
            final String s = translator.translate();
            SimpleIO.output(out, s, l -> l);
        } catch (SysYException e) {
            System.out.println(e.stringify());
            System.exit(1);
        } catch (ParserController.ParseError e) {
            System.err.println("Expect " + e.type);
            System.exit(1);
        }
    }

    public static Pair<IntermediateCode, IntermediateCode> optimize(LabelTable lt, Pair<IntermediateCode, IntermediateCode> block) {
        if (opt) {
            final Pair<IntermediateCode, IntermediateCode> blk = new Optimizer.SimplifyConst().apply(lt,
                            new Optimizer.RemoveRedundantLabel().apply(lt,
                                    new Optimizer.RemoveNop().apply(lt, block)));
            IntermediateCode p = blk.first;
            IntermediateCode start;
            IntermediateCode end = null;
            while (p != null) {
                if (p instanceof FuncEntry) {
                    p = p.getNext();
                    while (p instanceof ParameterFetch) {
                        p = p.getNext();
                    }
                    start = p;
                    while (p != null && !(p instanceof FuncEntry)) {
                        end = p;
                        p = p.getNext();
                    }
                    assert end != null;
                    new BasicBlockOptimizer().apply(lt, Pair.of(start, end));
                } else {
                    p = p.getNext();
                }
            }
            return blk;
        } else {
            return block;
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            for (String arg : args) {
                if (arg.equals("--O0")) {
                    opt = false;
                }
            }
        }
        mipsTest("testfile.txt", "mips.txt");
    }
}
