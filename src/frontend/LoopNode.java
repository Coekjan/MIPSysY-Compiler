package frontend;

import backend.Imm;
import exceptions.SysYException;
import midend.*;
import utils.Pair;

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
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        final ExprNode simCond = (ExprNode) condition.simplify(symbolTable).second;
        final StmtNode simBody = (StmtNode) loopBody.simplify(symbolTable.yield(Collections.emptyMap())).second;
        final BranchNode branchNode = (BranchNode) new BranchNode(simCond, simBody, new BreakNode(0))
                .simplify(symbolTable).second;
        return Pair.of(symbolTable, new LoopNode(new ConstNode(1), branchNode));
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        final ICodeInfo conditionCode = condition.iCode(lt, st, lpBegin, lpEnd, tc).second;
        final String loopBegin = lt.createLabel(true, true);
        final String loopEnd = lt.createLabel(true, false);
        final IntermediateCode branch = new Branch(Branch.BranchOption.EQ, conditionCode.finalSym, new ImmValue(0), loopEnd);
        final IntermediateCode jumpBegin = new Jump(loopBegin);
        final IntermediateCode end = new Nop();
        final ICodeInfo bodyCode = loopBody.iCode(lt, st.yield(Collections.emptyMap()),
                loopBegin, loopEnd, conditionCode.tempCount).second;
        conditionCode.second.link(branch);
        branch.link(bodyCode.first);
        bodyCode.second.link(jumpBegin);
        jumpBegin.link(end);
        lt.assignLabelToCode(loopBegin, conditionCode.first);
        lt.assignLabelToCode(loopEnd, end);
        return Pair.of(st, new ICodeInfo(conditionCode.first, end, null, bodyCode.tempCount));
    }

    @Override
    public List<ReturnNode> findRet() {
        return loopBody.findRet();
    }
}
