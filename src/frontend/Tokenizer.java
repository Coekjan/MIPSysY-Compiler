package frontend;

import exceptions.IdentificationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
    private final List<Token> tokens = new LinkedList<>();

    public Tokenizer(File file) throws IOException, IdentificationException {
        final InputStream inputStream = new FileInputStream(file);
        final StringJoiner stringJoiner = new StringJoiner("\n");
        final Scanner scanner = new Scanner(inputStream);
        while (scanner.hasNextLine()) {
            stringJoiner.add(scanner.nextLine());
        }
        scanner.close();
        final String code = stringJoiner.toString();
        final Optional<String> make = Token.typePatterns.entrySet().stream()
                .map(e -> "(?<" + e.getKey().toString() + ">" + e.getValue() + ")")
                .reduce((s1, s2) -> s1 + "|" + s2);
        if (!make.isPresent()) {
            throw new IllegalArgumentException();
        }
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
    }

    public List<Token> getTokens() {
        return tokens;
    }
}
