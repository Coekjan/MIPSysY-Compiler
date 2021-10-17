package frontend;

public class BreakNode implements StmtNode {
    public final int line;

    public BreakNode(int line) {
        this.line = line;
    }

    @Override
    public String toString() {
        return "BREAK";
    }
}
