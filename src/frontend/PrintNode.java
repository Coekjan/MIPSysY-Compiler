package frontend;

import exceptions.SysYException;
import midend.*;
import utils.Pair;

import java.util.LinkedList;
import java.util.List;

public class PrintNode extends FuncCallNode {
    public final String strNode;

    public PrintNode(Token identifier, String strNode, List<ExprNode> arguments) {
        super(identifier, arguments);
        this.strNode = strNode;
    }

    public PrintNode(String name, int line, String strNode, List<ExprNode> arguments) {
        super(name, line, arguments);
        this.strNode = strNode;
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        for (ExprNode node : arguments) {
            node.check(symbolTable, inLoop);
        }
        if (strNode.split("%d").length - 1 != arguments.size()) {
            errors.add(Pair.of(line, SysYException.Code.l));
        }
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        final List<ExprNode> simArgs = new LinkedList<>();
        for (ExprNode exprNode : arguments) {
            simArgs.add((ExprNode) exprNode.simplify(symbolTable).second);
        }
        return Pair.of(symbolTable, new PrintNode(name, line, strNode, simArgs));
    }

    @Override
    public ReturnType getRetType(SymbolTable symbolTable) {
        return ReturnType.VOID;
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        int tempCount = tc;
        IntermediateCode last = new Nop();
        final IntermediateCode head = last;
        for (int i = arguments.size() - 1; i >= 0; i--) {
            final ExprNode exprNode = arguments.get(i);
            final ICodeInfo code = exprNode.iCode(lt, st, lpBegin, lpEnd, tempCount).second;
            final IntermediateCode push = new PushArgument(code.finalSym);
            tempCount = code.tempCount;
            last.link(code.first);
            code.second.link(push);
            last = push;
        }
        final IntermediateCode print = new Print(strNode);
        last.link(print);
        return Pair.of(st, new ICodeInfo(head, print, null, tempCount));
    }
}
