package frontend;

import exceptions.SysYException;
import midend.IntermediateCode;
import midend.LabelTable;
import midend.Return;
import utils.Pair;

import java.util.Collections;
import java.util.List;

public class ReturnNode implements StmtNode {
    public final int line;
    public final ExprNode returnValue;

    public ReturnNode(int line) {
        this(line, null);
    }

    public ReturnNode(int line, ExprNode returnValue) {
        this.line = line;
        this.returnValue = returnValue;
    }

    @Override
    public String toString() {
        if (returnValue == null) {
            return "RETURN";
        }
        return "RETURN " + returnValue;
    }

    @Override
    public List<ReturnNode> findRet() {
        return Collections.singletonList(this);
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        if (returnValue != null) returnValue.check(symbolTable, inLoop);
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        if (returnValue != null) return Pair.of(symbolTable,
                new ReturnNode(line, (ExprNode) returnValue.simplify(symbolTable).second));
        return Pair.of(symbolTable, this);
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        if (returnValue != null) {
            final ICodeInfo exprCode = returnValue.iCode(lt, st, lpBegin, lpEnd, tc).second;
            final IntermediateCode ret = new Return(exprCode.finalSym);
            exprCode.second.link(ret);
            return Pair.of(st, new ICodeInfo(exprCode.first, ret, null, exprCode.tempCount));
        } else {
            final IntermediateCode ret = new Return();
            return Pair.of(st, new ICodeInfo(ret, ret, null, tc));
        }
    }
}
