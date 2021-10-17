package frontend;

public class ReturnNode implements StmtNode {
    public final ExprNode returnValue;

    public ReturnNode() {
        this(null);
    }

    public ReturnNode(ExprNode returnValue) {
        this.returnValue = returnValue;
    }

    @Override
    public String toString() {
        if (returnValue == null) {
            return "RETURN";
        }
        return "RETURN " + returnValue;
    }
}
