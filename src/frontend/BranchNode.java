package frontend;

import exceptions.SysYException;
import midend.*;
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
        final StmtNode simElse = elseBlock == null ? null :
                (StmtNode) elseBlock.simplify(symbolTable.yield(Collections.emptyMap())).second;
        if (simCond instanceof BinaryExprNode) {
            BinaryExprNode.Operator op = ((BinaryExprNode) simCond).operator;
            if (op == BinaryExprNode.Operator.AND) {
                final BranchNode inside = new BranchNode(((BinaryExprNode) simCond).right, simThen, simElse);
                final BranchNode outside = new BranchNode(((BinaryExprNode) simCond).left, inside, simElse);
                return outside.simplify(symbolTable);
            } else if (op == BinaryExprNode.Operator.OR) {
                final BranchNode inside = new BranchNode(((BinaryExprNode) simCond).right, simThen, simElse);
                final BranchNode outside = new BranchNode(((BinaryExprNode) simCond).left, simThen, inside);
                return outside.simplify(symbolTable);
            }
        }
        return Pair.of(symbolTable, new BranchNode(simCond, simThen, simElse));
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        final ICodeInfo conditionCode = condition.iCode(lt, st, lpBegin, lpEnd, tc).second;
        final Value brCond = new WordValue(String.valueOf(conditionCode.tempCount + 1));
        final String thenEndLabel = lt.createLabel();
        final String elseEndLabel = lt.createLabel();
        final ICodeInfo thenBlockCode = thenBlock.iCode(lt, st.yield(Collections.emptyMap()),
                lpBegin, lpEnd, conditionCode.tempCount + 1).second;
        final IntermediateCode invert = new AssignUnaryOperation(true, brCond,
                AssignUnaryOperation.UnaryOperation.NOT, conditionCode.finalSym);
        final IntermediateCode branch = new Branch(brCond, thenEndLabel);
        final IntermediateCode jumpEnd = new Jump(elseEndLabel);
        final IntermediateCode end = new Nop();
        lt.assignLabelToCode(thenEndLabel, end);
        conditionCode.second.link(invert);
        invert.link(branch);
        branch.link(thenBlockCode.first);
        thenBlockCode.second.link(jumpEnd);
        jumpEnd.link(end);
        if (elseBlock != null) {
            final ICodeInfo elseBlockCode = elseBlock.iCode(lt, st.yield(Collections.emptyMap()),
                    lpBegin, lpEnd, thenBlockCode.tempCount).second;
            end.link(elseBlockCode.first);
            lt.assignLabelToCode(elseEndLabel, elseBlockCode.second);
            return Pair.of(st, new ICodeInfo(conditionCode.first,
                    elseBlockCode.second, null, elseBlockCode.tempCount));
        } else {
            lt.assignLabelToCode(elseEndLabel, end);
        }
        return Pair.of(st, new ICodeInfo(conditionCode.first, end, null, thenBlockCode.tempCount));
    }

    @Override
    public List<ReturnNode> findRet() {
        return new ArrayList<ReturnNode>(thenBlock.findRet()) {{
            if (elseBlock != null) addAll(elseBlock.findRet());
        }};
    }
}
