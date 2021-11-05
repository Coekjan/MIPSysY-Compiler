package frontend;

import exceptions.SysYException;
import utils.Pair;

public class StringExprNode implements ExprNode {
    public final String content;

    public StringExprNode(Token token) {
        this(token.content);
    }

    public StringExprNode(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }

    @Override
    public ReturnType getRetType(SymbolTable symbolTable) {
        return ReturnType.STR;
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        return Pair.of(symbolTable, this);
    }
}
