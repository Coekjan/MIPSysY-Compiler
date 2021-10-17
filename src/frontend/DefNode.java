package frontend;

import exceptions.SysYException;
import utils.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DefNode implements SyntaxNode {
    public final String name;
    public final int line;
    public final Pair<ExprNode, ExprNode> dimensions;
    public final List<ExprNode> initValues;

    public DefNode(Token identifier, Pair<ExprNode, ExprNode> dimensions, List<ExprNode> initValues) {
        this(identifier.content, identifier.line, dimensions, initValues);
    }

    public DefNode(String name, int line, Pair<ExprNode, ExprNode> dimensions, List<ExprNode> initValues) {
        this.name = name;
        this.line = line;
        this.dimensions = dimensions;
        this.initValues = Collections.unmodifiableList(initValues);
    }

    @Override
    public String toString() {
        return name + dimensions.toString() + " := {" + initValues.stream()
                .map(Objects::toString).reduce((x, y) -> x + ", " + y).orElse("") + "}";
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        dimensions.first.check(symbolTable, inLoop);
        dimensions.second.check(symbolTable, inLoop);
        for (ExprNode initValue : initValues) {
            initValue.check(symbolTable, inLoop);
        }
        return symbolTable;
    }
}
