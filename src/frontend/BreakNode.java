package frontend;

import exceptions.SysYException;
import utils.Pair;

public class BreakNode implements StmtNode {
    public final int line;

    public BreakNode(int line) {
        this.line = line;
    }

    @Override
    public String toString() {
        return "BREAK";
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        if (!inLoop) {
            errors.add(Pair.of(line, SysYException.Code.m));
        }
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        return Pair.of(symbolTable, this);
    }
}
