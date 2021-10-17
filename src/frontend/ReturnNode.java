package frontend;

import exceptions.SysYException;

import java.util.Collections;
import java.util.List;

public class ReturnNode implements StmtNode {
    public final int line;
    public final ExprNode returnValue;

    public ReturnNode(int line) {
        this(line, null);
    }

    public ReturnNode(int line, ExprNode returnValue) {
        this.line = line;
        this.returnValue = returnValue;
    }

    @Override
    public String toString() {
        if (returnValue == null) {
            return "RETURN";
        }
        return "RETURN " + returnValue;
    }

    @Override
    public List<ReturnNode> findRet() {
        return Collections.singletonList(this);
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        if (returnValue != null) returnValue.check(symbolTable, inLoop);
        return symbolTable;
    }
}
