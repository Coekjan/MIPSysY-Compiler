package frontend;

public class BinaryExprNode implements ExprNode {
    public enum Operator {
        ADD, SUB, MUL, DIV, MOD, AND, OR, GT, GE, LT, LE, EQ, NE
    }

    public final Operator operator;
    public final ExprNode left;
    public final ExprNode right;

    public BinaryExprNode(Operator operator, ExprNode left, ExprNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + left.toString() + ") " + operator.name() + " (" + right.toString() + ")";
    }
}
