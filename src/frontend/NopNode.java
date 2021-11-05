package frontend;

import exceptions.SysYException;
import utils.Pair;

public class NopNode implements StmtNode {
    public static final NopNode NOP = new NopNode();

    private NopNode() {
    }

    @Override
    public String toString() {
        return "NOP";
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
