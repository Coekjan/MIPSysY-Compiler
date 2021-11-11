package frontend;

import exceptions.SysYException;
import midend.*;
import utils.Pair;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GlobalNode implements SyntaxNode {
    public final List<DeclNode> declNodes;
    public final List<FuncDefNode> funcDefNodes;
    public final FuncDefNode mainFuncDef;

    public GlobalNode(List<DeclNode> declNodes, List<FuncDefNode> funcDefNodes, FuncDefNode mainFuncDef) {
        this.declNodes = Collections.unmodifiableList(declNodes);
        this.funcDefNodes = Collections.unmodifiableList(funcDefNodes);
        this.mainFuncDef = mainFuncDef;
    }

    @Override
    public String toString() {
        return declNodes.stream().map(DeclNode::toString)
                .reduce((x, y) -> x + "\n" + y).orElse("") + "\n" +
                funcDefNodes.stream().map(FuncDefNode::toString)
                        .reduce((x, y) -> x + "\n" + y).orElse("") + "\n" + mainFuncDef.toString();
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        SymbolTable next = symbolTable;
        for (DeclNode declNode : declNodes) {
            next = declNode.check(next, inLoop);
        }
        for (FuncDefNode funcDefNode : funcDefNodes) {
            next = funcDefNode.check(next, inLoop);
        }
        return mainFuncDef.check(next, inLoop);
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        SymbolTable next = symbolTable;
        final List<DeclNode> simDecl = new LinkedList<>();
        final List<FuncDefNode> simFunc = new LinkedList<>();
        for (DeclNode declNode : declNodes) {
            final Pair<SymbolTable, SyntaxNode> p = declNode.simplify(next);
            next = p.first;
            simDecl.add((DeclNode) p.second);
        }
        for (FuncDefNode funcDefNode : funcDefNodes) {
            final Pair<SymbolTable, SyntaxNode> p = funcDefNode.simplify(next);
            next = p.first;
            simFunc.add((FuncDefNode) p.second);
        }
        final Pair<SymbolTable, SyntaxNode> p = mainFuncDef.simplify(next);
        final FuncDefNode simMain = (FuncDefNode) p.second;
        return Pair.of(p.first, new GlobalNode(simDecl, simFunc, simMain));
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        int tempCount = tc;
        IntermediateCode last = new Nop();
        SymbolTable next = st;
        final IntermediateCode head = last;
        for (DeclNode declNode : declNodes) {
            final Pair<SymbolTable, ICodeInfo> p = declNode.iCode(lt, next, lpBegin, lpEnd, tempCount, true);
            next = p.first;
            last.link(p.second.first);
            last = p.second.second;
            tempCount = p.second.tempCount;
        }
        final IntermediateCode callMain = new CallFunction("main");
        final IntermediateCode exit = new Exit();
        last.link(callMain);
        callMain.link(exit);
        last = exit;
        for (FuncDefNode funcDefNode : funcDefNodes) {
            final Pair<SymbolTable, ICodeInfo> p = funcDefNode.iCode(lt, next, lpBegin, lpEnd, tempCount);
            next = p.first;
            last.link(p.second.first);
            last = p.second.second;
            tempCount = p.second.tempCount;
        }
        final Pair<SymbolTable, ICodeInfo> p = mainFuncDef.iCode(lt, next, lpBegin, lpEnd, tempCount);
        last.link(p.second.first);
        return Pair.of(p.first, new ICodeInfo(head, p.second.second, null, p.second.tempCount));
    }
}
