package frontend;

import exceptions.SysYException;

public class UnaryExprNode implements ExprNode {

    public enum Operator {
        POS, NEG, NOT
    }

    public final Operator operator;
    public final ExprNode exprNode;

    public UnaryExprNode(Operator operator, ExprNode exprNode) {
        this.operator = operator;
        this.exprNode = exprNode;
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
}
