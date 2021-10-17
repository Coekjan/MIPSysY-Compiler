package frontend;

public class ContNode implements StmtNode {
    public final int line;

    public ContNode(int line) {
        this.line = line;
    }

    @Override
    public String toString() {
        return "CONTINUE";
    }
}
