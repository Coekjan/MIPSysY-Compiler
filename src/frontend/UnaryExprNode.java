package frontend;

public class UnaryExprNode implements ExprNode {
    public enum Operator {
        POS, NEG, NOT
    }

    public final Operator operator;
    public final ExprNode exprNode;

    public UnaryExprNode(Operator operator, ExprNode exprNode) {
        this.operator = operator;
        this.exprNode = exprNode;
    }

    @Override
    public String toString() {
        return operator.toString() + " (" + exprNode.toString() + ")";
    }
}
