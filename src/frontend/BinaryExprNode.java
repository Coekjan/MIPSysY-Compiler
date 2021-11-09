package frontend;

import exceptions.SysYException;
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
                    put(Operator.AND, (a, b) -> a == 1 && b == 1 ? 1 : 0);
                    put(Operator.OR, (a, b) -> a == 1 || b == 1 ? 1 : 0);
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
    public ReturnType getRetType(SymbolTable symbolTable) throws SysYException {
        ReturnType leftRet = left.getRetType(symbolTable);
        ReturnType rightRet = right.getRetType(symbolTable);
        if (leftRet != rightRet) throw new SysYException(SysYException.Code.e);
        if (leftRet == ReturnType.INT) return ReturnType.INT;
        throw new SysYException(SysYException.Code.e);
    }
}
