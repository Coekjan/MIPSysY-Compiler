package frontend;

import exceptions.SysYException;
import midend.AssignBinaryOperation;
import midend.IntermediateCode;
import midend.LabelTable;
import midend.Value;
import utils.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BinaryExprNode implements ExprNode {
    public enum Operator {
        ADD, SUB, MUL, DIV, MOD, AND, OR, GT, GE, LT, LE, EQ, NE
    }

    @FunctionalInterface
    private interface ExecBinOperator {
        int exec(int x, int y);
    }

    public final Operator operator;
    public final ExprNode left;
    public final ExprNode right;

    public BinaryExprNode(Operator operator, ExprNode left, ExprNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    private int operate(int x, int y) {
        final Map<Operator, ExecBinOperator> operations =
                Collections.unmodifiableMap(new HashMap<Operator, ExecBinOperator>() {{
                    put(Operator.ADD, Integer::sum);
                    put(Operator.SUB, (a, b) -> a - b);
                    put(Operator.MUL, (a, b) -> a * b);
                    put(Operator.DIV, (a, b) -> a / b);
                    put(Operator.MOD, (a, b) -> a % b);
                    put(Operator.AND, (a, b) -> a != 0 && b != 0 ? 1 : 0);
                    put(Operator.OR, (a, b) -> a != 0 || b != 0 ? 1 : 0);
                    put(Operator.GT, (a, b) -> a > b ? 1 : 0);
                    put(Operator.GE, (a, b) -> a >= b ? 1 : 0);
                    put(Operator.LT, (a, b) -> a < b ? 1 : 0);
                    put(Operator.LE, (a, b) -> a <= b ? 1 : 0);
                    put(Operator.EQ, (a, b) -> a == b ? 1 : 0);
                    put(Operator.NE, (a, b) -> a != b ? 1 : 0);
                }});
        return operations.get(operator).exec(x, y);
    }

    @Override
    public String toString() {
        return "(" + left.toString() + ") " + operator.name() + " (" + right.toString() + ")";
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        left.check(symbolTable, inLoop);
        right.check(symbolTable, inLoop);
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        final ExprNode l = (ExprNode) left.simplify(symbolTable).second;
        final ExprNode r = (ExprNode) right.simplify(symbolTable).second;
        if (l instanceof ConstNode && r instanceof ConstNode) {
            return Pair.of(symbolTable, new ConstNode(operate(((ConstNode) l).constant, ((ConstNode) r).constant)));
        }
        return Pair.of(symbolTable, new BinaryExprNode(operator, l, r));
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        final Map<Operator, AssignBinaryOperation.BinaryOperation> opMap =
                Collections.unmodifiableMap(new HashMap<Operator, AssignBinaryOperation.BinaryOperation>() {{
                    put(Operator.ADD, AssignBinaryOperation.BinaryOperation.ADD);
                    put(Operator.SUB, AssignBinaryOperation.BinaryOperation.SUB);
                    put(Operator.MUL, AssignBinaryOperation.BinaryOperation.MUL);
                    put(Operator.DIV, AssignBinaryOperation.BinaryOperation.DIV);
                    put(Operator.MOD, AssignBinaryOperation.BinaryOperation.MOD);
                    put(Operator.AND, AssignBinaryOperation.BinaryOperation.AND);
                    put(Operator.OR, AssignBinaryOperation.BinaryOperation.OR);
                    put(Operator.GT, AssignBinaryOperation.BinaryOperation.GT);
                    put(Operator.GE, AssignBinaryOperation.BinaryOperation.GE);
                    put(Operator.LT, AssignBinaryOperation.BinaryOperation.LT);
                    put(Operator.LE, AssignBinaryOperation.BinaryOperation.LE);
                    put(Operator.EQ, AssignBinaryOperation.BinaryOperation.EQ);
                    put(Operator.NE, AssignBinaryOperation.BinaryOperation.NE);
                }});
        final Pair<SymbolTable, ICodeInfo> l = left.iCode(lt, st, lpBegin, lpEnd, tc);
        final Pair<SymbolTable, ICodeInfo> r = right.iCode(lt, st, lpBegin, lpEnd, l.second.tempCount);
        final Value lv = l.second.finalSym;
        final Value rv = r.second.finalSym;
        final Value yieldValue = Value.pack(lv, rv, String.valueOf(r.second.tempCount + 1));
        l.second.second.link(r.second.first);
        final IntermediateCode code = new AssignBinaryOperation(true, yieldValue,
                opMap.get(operator), lv, rv);
        r.second.second.link(code);
        return Pair.of(st, new ICodeInfo(l.second.first, code, yieldValue, r.second.tempCount + 1));
    }

    @Override
    public ReturnType getRetType(SymbolTable symbolTable) throws SysYException {
        ReturnType leftRet = left.getRetType(symbolTable);
        ReturnType rightRet = right.getRetType(symbolTable);
        if (leftRet != rightRet) throw new SysYException(SysYException.Code.e);
        if (leftRet == ReturnType.INT) return ReturnType.INT;
        throw new SysYException(SysYException.Code.e);
    }
}
