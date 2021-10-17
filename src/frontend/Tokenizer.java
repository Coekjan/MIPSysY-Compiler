package frontend;

import exceptions.SysYException;
import utils.Pair;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
    public static final List<Pair<Integer, SysYException.Code>> errors = new LinkedList<>();

    public static List<Token> lex(String code) throws SysYException {
        final List<Token> tokens = new LinkedList<>();
        final Optional<String> make = Token.typePatterns.entrySet().stream()
                .map(e -> "(?<" + e.getKey().toString() + ">" + e.getValue() + ")")
                .reduce((s1, s2) -> s1 + "|" + s2);
        assert make.isPresent();
        final Pattern pattern = Pattern.compile(make.get() + "|(?<ERROR>.)");
        final Matcher matcher = pattern.matcher(code);
        int lineNumber = 1;
        extract: while (matcher.find()) {
            for (Token.Type t: Token.Type.values()) {
                String content = matcher.group(t.toString());
                if (content != null) {
                    lineNumber += content.chars().boxed().filter(c -> c == '\n').count();
                    if (t == Token.Type.STRCON) {
                        for (int i = 1; i < content.length() - 1; ++i) {
                            int ascii = content.charAt(i);
                            if (ascii == 32 || ascii == 33 || ascii >= 40 && ascii <= 126) {
                                if (ascii == 92 && content.charAt(i + 1) != 'n') {
                                    errors.add(Pair.of(lineNumber, SysYException.Code.a));
                                }
                            } else if (ascii == 37) {
                                if (content.charAt(i + 1) != 'd') errors.add(Pair.of(lineNumber, SysYException.Code.a));
                            } else {
                                errors.add(Pair.of(lineNumber, SysYException.Code.a));
                            }
                        }
                    }
                    if (!Token.Type.ignore(t)) {
                        tokens.add(new Token(content, t, lineNumber));
                    }
                    continue extract;
                }
            }
            throw new SysYException(SysYException.Code.u);
        }
        return tokens;
    }
}
