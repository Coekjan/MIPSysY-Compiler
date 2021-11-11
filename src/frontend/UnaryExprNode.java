package frontend;

import exceptions.SysYException;
import midend.AssignUnaryOperation;
import midend.IntermediateCode;
import midend.LabelTable;
import midend.WordValue;
import utils.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UnaryExprNode implements ExprNode {
    public enum Operator {
        POS, NEG, NOT
    }

    @FunctionalInterface
    private interface ExecUnaOperator {
        int exec(int x);
    }

    public final Operator operator;
    public final ExprNode exprNode;

    public UnaryExprNode(Operator operator, ExprNode exprNode) {
        this.operator = operator;
        this.exprNode = exprNode;
    }

    private int operate(int x) {
        final Map<Operator, ExecUnaOperator> operations =
                Collections.unmodifiableMap(new HashMap<Operator, ExecUnaOperator>() {{
                    put(Operator.POS, a -> a);
                    put(Operator.NEG, a -> -a);
                    put(Operator.NOT, a -> a == 0 ? 1 : 0);
                }});
        return operations.get(operator).exec(x);
    }

    @Override
    public String toString() {
        return operator.toString() + " (" + exprNode.toString() + ")";
    }

    @Override
    public ReturnType getRetType(SymbolTable symbolTable) throws SysYException {
        if (exprNode.getRetType(symbolTable) == ReturnType.INT) return ReturnType.INT;
        throw new SysYException(SysYException.Code.e);
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        exprNode.check(symbolTable, inLoop);
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        final ExprNode simExpr = (ExprNode) exprNode.simplify(symbolTable).second;
        if (simExpr instanceof ConstNode) {
            return Pair.of(symbolTable, new ConstNode(operate(((ConstNode) simExpr).constant)));
        }
        return Pair.of(symbolTable, new UnaryExprNode(operator, simExpr));
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        final Map<Operator, AssignUnaryOperation.UnaryOperation> operations =
                Collections.unmodifiableMap(new HashMap<Operator, AssignUnaryOperation.UnaryOperation>() {{
                    put(Operator.POS, AssignUnaryOperation.UnaryOperation.POS);
                    put(Operator.NEG, AssignUnaryOperation.UnaryOperation.NEG);
                    put(Operator.NOT, AssignUnaryOperation.UnaryOperation.NOT);
                }});
        final ICodeInfo exprCode = exprNode.iCode(lt, st, lpBegin, lpEnd, tc).second;
        final WordValue word = new WordValue(String.valueOf(exprCode.tempCount + 1));
        final IntermediateCode code = new AssignUnaryOperation(true, word, operations.get(operator),
                exprCode.finalSym);
        exprCode.second.link(code);
        return Pair.of(st, new ICodeInfo(exprCode.first, code, word, exprCode.tempCount + 1));
    }
}
