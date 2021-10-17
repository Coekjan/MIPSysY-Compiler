package frontend;

import exceptions.SysYException;

import java.util.Collections;
import java.util.List;

public class LoopNode implements StmtNode {
    public final ExprNode condition;
    public final StmtNode loopBody;

    public LoopNode(ExprNode condition, StmtNode loopBody) {
        this.condition = condition;
        this.loopBody = loopBody;
    }

    @Override
    public String toString() {
        return "WHILE (" + condition.toString() + ") " + loopBody.toString();
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        condition.check(symbolTable, inLoop);
        loopBody.check(symbolTable.yield(Collections.emptyMap()), true);
        return symbolTable;
    }

    @Override
    public List<ReturnNode> findRet() {
        return loopBody.findRet();
    }
}
