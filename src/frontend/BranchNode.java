package frontend;

import exceptions.SysYException;
import utils.Pair;

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
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        final ExprNode simCond = (ExprNode) condition.simplify(symbolTable).second;
        final StmtNode simThen = (StmtNode) thenBlock.simplify(symbolTable).second;
        if (elseBlock == null) return Pair.of(symbolTable, new BranchNode(simCond, simThen));
        final StmtNode simElse = (StmtNode) elseBlock.simplify(symbolTable.yield(Collections.emptyMap())).second;
        return Pair.of(symbolTable, new BranchNode(simCond, simThen, simElse));
    }

    @Override
    public List<ReturnNode> findRet() {
        return new ArrayList<ReturnNode>(thenBlock.findRet()) {{
            if (elseBlock != null) addAll(elseBlock.findRet());
        }};
    }
}
