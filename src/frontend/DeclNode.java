package frontend;

import exceptions.SysYException;
import utils.Pair;

import java.util.*;

public class DeclNode implements BlockItemNode, VarDefNode {
    public final boolean modifiable;
    public final List<DefNode> defNodes;

    public DeclNode(boolean modifiable, List<DefNode> defNodes) {
        this.modifiable = modifiable;
        this.defNodes = Collections.unmodifiableList(defNodes);
    }

    @Override
    public String toString() {
        return (modifiable ? "CONST INT " : "INT ") + defNodes.stream()
                .map(Objects::toString).reduce((x, y) -> x + ", " + y).orElse("");
    }

    @Override
    public Map<String, Pair<Boolean, Pair<ExprNode, ExprNode>>> getInfo() {
        return Collections.unmodifiableMap(new HashMap<String, Pair<Boolean, Pair<ExprNode, ExprNode>>>() {{
            defNodes.forEach(d -> put(d.name, Pair.of(modifiable, d.dimensions)));
        }});
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        SymbolTable next = symbolTable;
        for (DefNode defNode : defNodes) {
            defNode.check(next, inLoop);
            Map<String, VarDefNode> map = new HashMap<>(next.getHead());
            if (map.containsKey(defNode.name)) {
                errors.add(Pair.of(defNode.line, SysYException.Code.b));
            } else {
                map.put(defNode.name, this);
            }
            next = next.update(map);
        }
        return next;
    }
}
