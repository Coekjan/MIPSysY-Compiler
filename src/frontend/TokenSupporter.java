package frontend;

import java.util.*;

public class TokenSupporter implements Cloneable {
    public final List<Token> tokens;
    private int pointer;

    public TokenSupporter(List<Token> tokens) {
        this.tokens = Collections.unmodifiableList(new ArrayList<>(tokens));
    }

    public boolean isEmpty() {
        return pointer == tokens.size();
    }

    public int size() {
        return tokens.size() - pointer;
    }

    public Token get(int index) {
        return tokens.get(pointer + index);
    }

    public Optional<Token> take() {
        return isEmpty() ? Optional.empty() : Optional.of(tokens.get(pointer++));
    }

    public void pushBack() {
        pointer = pointer == 0 ? 0 : pointer - 1;
    }

    public Optional<Token> prev() {
        return pointer == 0 ? Optional.empty() : Optional.of(tokens.get(pointer - 1));
    }

    @Override
    public TokenSupporter clone() {
        try {
            return (TokenSupporter) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
