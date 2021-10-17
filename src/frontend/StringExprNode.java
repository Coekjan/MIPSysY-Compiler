package frontend;

public class StringExprNode implements ExprNode {
    public final String content;

    public StringExprNode(Token token) {
        this(token.content);
    }

    public StringExprNode(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}
