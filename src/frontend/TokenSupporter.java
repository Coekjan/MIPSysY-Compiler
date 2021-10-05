package frontend;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TokenSupporter {
    public final List<Token> tokens;

    public TokenSupporter(List<Token> tokens) {
        this.tokens = new LinkedList<>(tokens);
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    public int size() {
        return tokens.size();
    }

    public Optional<Token> take() {
        return isEmpty() ? Optional.empty() : Optional.of(tokens.remove(0));
    }
}
