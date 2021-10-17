package frontend;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FuncCallNode implements ExprNode {
    public final String name;
    public final int line;
    public final List<ExprNode> arguments;

    public FuncCallNode(Token identifier, List<ExprNode> arguments) {
        this(identifier.content, identifier.line, arguments);
    }

    public FuncCallNode(String name, int line, List<ExprNode> arguments) {
        this.name = name;
        this.line = line;
        this.arguments = Collections.unmodifiableList(arguments);
    }

    @Override
    public String toString() {
        return name + "(" + arguments.stream()
                .map(Objects::toString).reduce((x, y) -> x + ", " + y).orElse("") + ")";
    }
}
