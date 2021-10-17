package frontend;

import exceptions.SysYException;
import utils.Pair;

import java.util.*;

public class FuncDefNode implements VarDefNode {
    public final boolean returnInt;
    public final String name;
    public final int line;
    public final List<FuncParamNode> parameters;
    public final FuncBlockNode block;

    public FuncDefNode(boolean returnInt, Token identifier, List<FuncParamNode> parameters, FuncBlockNode block) {
        this(returnInt, identifier.content, identifier.line, parameters, block);
    }

    public FuncDefNode(boolean returnInt, String name, int line, List<FuncParamNode> parameters, FuncBlockNode block) {
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

    @Override
    public Map<String, Pair<Boolean, Pair<ExprNode, ExprNode>>> getInfo() {
        return Collections.unmodifiableMap(new HashMap<String, Pair<Boolean, Pair<ExprNode, ExprNode>>>() {{
            parameters.forEach(p -> put(p.name, Pair.of(true, p.dimensions)));
        }});
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        final FuncDefNode self = this;
        symbolTable = new SymbolTable(symbolTable, new HashMap<String, FuncDefNode>() {{
            put(name, self);
        }});
        SymbolTable next = symbolTable.yield(Collections.emptyMap());
        for (FuncParamNode paramNode : parameters) {
            paramNode.check(next, inLoop);
            Map<String, VarDefNode> map = new HashMap<>(next.getHead());
            if (map.containsKey(paramNode.name)) {
                errors.add(Pair.of(paramNode.line, SysYException.Code.b));
            } else {
                map.put(paramNode.name, this);
            }
            next = next.update(map);
        }
        block.check(next, inLoop);
        return symbolTable;
    }
}
