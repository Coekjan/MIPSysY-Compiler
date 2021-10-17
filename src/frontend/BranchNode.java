package frontend;

public class BranchNode implements StmtNode {
    public final ExprNode condition;
    public final StmtNode thenBlock;
    public final StmtNode elseBlock;

    public BranchNode(ExprNode condition, StmtNode thenBlock) {
        this(condition, thenBlock, null);
    }

    public BranchNode(ExprNode condition, StmtNode thenBlock, StmtNode elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public String toString() {
        if (elseBlock == null)
            return "IF (" + condition.toString() + ") " + thenBlock.toString();
        else
            return "IF (" + condition.toString() + ") " + thenBlock.toString() + "\nELSE " + elseBlock;
    }
}
