package frontend;

import utils.Pair;

public class LValNode implements ExprNode {
    public final String name;
    public final int line;
    public final Pair<ExprNode, ExprNode> indexes;

    public LValNode(Token identifier, Pair<ExprNode, ExprNode> indexes) {
        this(identifier.content, identifier.line, indexes);
    }

    public LValNode(String name, int line, Pair<ExprNode, ExprNode> indexes) {
        this.name = name;
        this.line = line;
        this.indexes = indexes;
    }

    @Override
    public String toString() {
        return name + indexes.toString();
    }
}
