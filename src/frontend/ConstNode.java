package frontend;

import exceptions.SysYException;
import utils.Pair;

public class ConstNode implements ExprNode {
    public static final ConstNode ZERO = new ConstNode(0);
    public static final ConstNode IGNR = new ConstNode(Integer.MAX_VALUE);

    public final int constant;

    public ConstNode(Token constToken) {
        this(Integer.parseInt(constToken.content));
    }

    public ConstNode(int constant) {
        this.constant = constant;
    }

    @Override
    public String toString() {
        return String.valueOf(constant);
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        return Pair.of(symbolTable, this);
    }

    @Override
    public ReturnType getRetType(SymbolTable symbolTable) {
        return ReturnType.INT;
    }
}
