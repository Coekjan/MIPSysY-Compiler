package frontend;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FuncDefNode {
    public final boolean returnInt;
    public final String name;
    public final int line;
    public final List<FuncParamNode> parameters;
    public final BlockNode block;

    public FuncDefNode(boolean returnInt, Token identifier, List<FuncParamNode> parameters, BlockNode block) {
        this(returnInt, identifier.content, identifier.line, parameters, block);
    }

    public FuncDefNode(boolean returnInt, String name, int line, List<FuncParamNode> parameters, BlockNode block) {
        this.returnInt = returnInt;
        this.name = name;
        this.line = line;
        this.parameters = Collections.unmodifiableList(parameters);
        this.block = block;
    }

    @Override
    public String toString() {
        return (returnInt ? "INT " : "VOID ") + name + "(" + parameters.stream()
                .map(Objects::toString).reduce((x, y) -> x + ", " + y).orElse("") + ")" + block.toString();
    }
}
