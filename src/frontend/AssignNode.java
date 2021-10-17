package frontend;

public class AssignNode implements StmtNode {
    public final LValNode left;
    public final ExprNode right;

    public AssignNode(LValNode left, ExprNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + left.toString() + ") := (" + right.toString() + ")";
    }
}
