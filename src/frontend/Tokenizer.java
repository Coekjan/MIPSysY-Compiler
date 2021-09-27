package frontend;

import exceptions.IdentificationException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
    public static List<Token> lex(String code) throws IOException, IdentificationException {
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
                    if (!Token.Type.ignore(t)) {
                        tokens.add(new Token(content, t, lineNumber));
                    }
                    continue extract;
                }
            }
            throw new IdentificationException();
        }
        return tokens;
    }
}
