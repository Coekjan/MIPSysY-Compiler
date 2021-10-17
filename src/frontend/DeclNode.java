package frontend;

import java.util.List;
import java.util.Objects;

public class DeclNode implements BlockItemNode {
    public final boolean modifiable;
    public final List<DefNode> defNodes;

    public DeclNode(boolean modifiable, List<DefNode> defNodes) {
        this.modifiable = modifiable;
        this.defNodes = defNodes;
    }

    @Override
    public String toString() {
        return (modifiable ? "CONST INT " : "INT ") + defNodes.stream()
                .map(Objects::toString).reduce((x, y) -> x + ", " + y).orElse("");
    }
}
