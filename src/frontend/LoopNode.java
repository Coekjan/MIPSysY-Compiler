package frontend;

public class LoopNode implements StmtNode {
    public final ExprNode condition;
    public final StmtNode loopBody;

    public LoopNode(ExprNode condition, StmtNode loopBody) {
        this.condition = condition;
        this.loopBody = loopBody;
    }

    @Override
    public String toString() {
        return "WHILE (" + condition.toString() + ") " + loopBody.toString();
    }
}
