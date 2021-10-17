package frontend;

import exceptions.SysYException;

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
}
