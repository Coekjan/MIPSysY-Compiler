package frontend;

public class NopNode implements StmtNode {
    public static final NopNode NOP = new NopNode();

    private NopNode() {
    }

    @Override
    public String toString() {
        return "NOP";
    }
}
