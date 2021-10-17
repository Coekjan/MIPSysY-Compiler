package frontend;

import exceptions.SysYException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BranchNode implements StmtNode {
    public final ExprNode condition;
    public final StmtNode thenBlock;
    public final StmtNode elseBlock;

    public BranchNode(ExprNode condition, StmtNode thenBlock) {
        this(condition, thenBlock, null);
    }

    public BranchNode(ExprNode condition, StmtNode thenBlock, StmtNode elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public String toString() {
        if (elseBlock == null)
            return "IF (" + condition.toString() + ") " + thenBlock.toString();
        else
            return "IF (" + condition.toString() + ") " + thenBlock.toString() + "\nELSE " + elseBlock;
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        condition.check(symbolTable, inLoop);
        thenBlock.check(symbolTable.yield(Collections.emptyMap()), inLoop);
        if (elseBlock != null) elseBlock.check(symbolTable.yield(Collections.emptyMap()), inLoop);
        return symbolTable;
    }

    @Override
    public List<ReturnNode> findRet() {
        return new ArrayList<ReturnNode>(thenBlock.findRet()) {{
            if (elseBlock != null) addAll(elseBlock.findRet());
        }};
    }
}
