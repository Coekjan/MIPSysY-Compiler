package frontend;

import exceptions.SysYException;

public class BinaryExprNode implements ExprNode {
    public enum Operator {
        ADD, SUB, MUL, DIV, MOD, AND, OR, GT, GE, LT, LE, EQ, NE
    }

    public final Operator operator;
    public final ExprNode left;
    public final ExprNode right;

    public BinaryExprNode(Operator operator, ExprNode left, ExprNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
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
    public ReturnType getRetType(SymbolTable symbolTable) throws SysYException {
        ReturnType leftRet = left.getRetType(symbolTable);
        ReturnType rightRet = right.getRetType(symbolTable);
        if (leftRet != rightRet) throw new SysYException(SysYException.Code.e);
        if (leftRet == ReturnType.INT) return ReturnType.INT;
        throw new SysYException(SysYException.Code.e);
    }
}
