package frontend;

import exceptions.SysYException;
import utils.Pair;

import java.util.List;

public class PrintNode extends FuncCallNode {
    public final StringExprNode strNode;
    public PrintNode(Token identifier, StringExprNode strNode, List<ExprNode> arguments) {
        super(identifier, arguments);
        this.strNode = strNode;
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        for (ExprNode node : arguments) {
            node.check(symbolTable, inLoop);
        }
        if (strNode.content.split("%d").length - 1 != arguments.size()) {
            errors.add(Pair.of(line, SysYException.Code.l));
        }
        return symbolTable;
    }

    @Override
    public ReturnType getRetType(SymbolTable symbolTable) {
        return ReturnType.VOID;
    }
}
