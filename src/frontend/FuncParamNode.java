package frontend;

import exceptions.SysYException;
import utils.Pair;

public class FuncParamNode implements SyntaxNode {
    public final String name;
    public final int line;
    public final Pair<ExprNode, ExprNode> dimensions;

    public FuncParamNode(Token identifier, Pair<ExprNode, ExprNode> dimensions) {
        this(identifier.content, identifier.line, dimensions);
    }

    public FuncParamNode(String name, int line, Pair<ExprNode, ExprNode> dimensions) {
        this.name = name;
        this.line = line;
        this.dimensions = dimensions;
    }

    @Override
    public String toString() {
        return name + dimensions.toString();
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        dimensions.first.check(symbolTable, inLoop);
        dimensions.second.check(symbolTable, inLoop);
        return symbolTable;
    }
}
