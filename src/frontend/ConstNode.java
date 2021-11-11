package frontend;

import exceptions.SysYException;
import midend.*;
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
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        final WordValue word = new WordValue(String.valueOf(tc + 1));
        final IntermediateCode con = new Move(true, word, new ImmValue(constant));
        return Pair.of(st, new ICodeInfo(con, con, word, tc + 1));
    }

    @Override
    public ReturnType getRetType(SymbolTable symbolTable) {
        return ReturnType.INT;
    }
}
