package frontend;

import exceptions.SysYException;
import midend.IntermediateCode;
import midend.LabelTable;
import midend.Return;
import utils.Pair;

import java.util.List;

public class FuncBlockNode extends BlockNode {
    public final boolean hasReturn;
    public final int endLine;

    public FuncBlockNode(List<BlockItemNode> items, boolean hasReturn, int endLine) {
        super(items);
        this.hasReturn = hasReturn;
        this.endLine = endLine;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        final Pair<SymbolTable, SyntaxNode> p = super.simplify(symbolTable);
        final BlockNode blockNode = (BlockNode) p.second;
        return Pair.of(p.first, new FuncBlockNode(blockNode.items, hasReturn, endLine));
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        super.check(symbolTable, inLoop);
        if (hasReturn && (items.isEmpty() || !(items.get(items.size() - 1) instanceof ReturnNode))) {
            errors.add(Pair.of(endLine, SysYException.Code.g));
        }
        final List<ReturnNode> returnNodes = findRet();
        if (!hasReturn) {
            for (ReturnNode node : returnNodes) {
                if (node.returnValue != null) {
                    errors.add(Pair.of(node.line, SysYException.Code.f));
                }
            }
        }
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        final Pair<SymbolTable, ICodeInfo> p = super.iCode(lt, st, lpBegin, lpEnd, tc);
        final IntermediateCode ret = new Return();
        p.second.second.link(ret);
        return Pair.of(p.first, new ICodeInfo(p.second.first, ret, null, p.second.tempCount));
    }
}
