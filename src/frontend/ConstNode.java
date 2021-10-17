package frontend;

public class ConstNode implements ExprNode {
    public static final ConstNode ZERO = new ConstNode(0);
    public static final ConstNode IGNR = new ConstNode(Integer.MAX_VALUE);

    public final int constant;

    public ConstNode(Token constToken) {
        this(Integer.parseInt(constToken.content));
    }

    public ConstNode(int constant) {
        this.constant = constant;
    }

    @Override
    public String toString() {
        return String.valueOf(constant);
    }
}
