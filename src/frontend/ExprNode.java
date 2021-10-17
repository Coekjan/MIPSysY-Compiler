package frontend;

import exceptions.SysYException;

public interface ExprNode extends StmtNode {
    enum ReturnType {
        VOID, INT, DIM1, DIM2, STR
    }

    ReturnType getRetType(SymbolTable symbolTable) throws SysYException;
}
