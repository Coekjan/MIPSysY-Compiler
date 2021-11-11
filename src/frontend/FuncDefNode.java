package frontend;

import exceptions.SysYException;
import midend.FuncEntry;
import midend.IntermediateCode;
import midend.LabelTable;
import utils.Pair;

import java.util.*;

public class FuncDefNode implements VarDefNode {
    public final boolean returnInt;
    public final String name;
    public final int line;
    public final List<FuncParamNode> parameters;
    public final FuncBlockNode block;

    public FuncDefNode(boolean returnInt, Token identifier, List<FuncParamNode> parameters, FuncBlockNode block) {
        this(returnInt, identifier.content, identifier.line, parameters, block);
    }

    public FuncDefNode(boolean returnInt, String name, int line, List<FuncParamNode> parameters, FuncBlockNode block) {
        this.returnInt = returnInt;
        this.name = name;
        this.line = line;
        this.parameters = Collections.unmodifiableList(parameters);
        this.block = block;
    }

    @Override
    public String toString() {
        return (returnInt ? "INT " : "VOID ") + name + "(" + parameters.stream()
                .map(Objects::toString).reduce((x, y) -> x + ", " + y).orElse("") + ")" + block.toString();
    }

    @Override
    public Map<String, Pair<Boolean, Pair<ExprNode, ExprNode>>> getInfo() {
        return Collections.unmodifiableMap(new HashMap<String, Pair<Boolean, Pair<ExprNode, ExprNode>>>() {{
            parameters.forEach(p -> put(p.name, Pair.of(true, p.dimensions)));
        }});
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        final FuncDefNode self = this;
        symbolTable = new SymbolTable(symbolTable, new HashMap<String, FuncDefNode>() {{
            put(name, self);
        }});
        SymbolTable next = symbolTable.yield(Collections.emptyMap());
        for (FuncParamNode paramNode : parameters) {
            paramNode.check(next, inLoop);
            Map<String, VarDefNode> map = new HashMap<>(next.getHead());
            if (map.containsKey(paramNode.name)) {
                errors.add(Pair.of(paramNode.line, SysYException.Code.b));
            } else {
                map.put(paramNode.name, this);
            }
            next = next.update(map);
        }
        block.check(next, inLoop);
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        final FuncDefNode self = this;
        final List<FuncParamNode> simPara = new LinkedList<>();
        symbolTable = new SymbolTable(symbolTable, new HashMap<String, FuncDefNode>() {{
            put(name, self);
        }});
        SymbolTable next = symbolTable.yield(Collections.emptyMap());
        for (FuncParamNode paramNode : parameters) {
            final Pair<SymbolTable, SyntaxNode> p = paramNode.simplify(next);
            next = next.update(new HashMap<String, VarDefNode>(next.getHead()) {{
                put(paramNode.name, self);
            }});
            simPara.add((FuncParamNode) p.second);
        }
        final Pair<SymbolTable, SyntaxNode> simBlk = block.simplify(next);
        final FuncDefNode res = new FuncDefNode(returnInt, name, line, simPara, (FuncBlockNode) simBlk.second);
        return Pair.of(symbolTable.fixFuncRef(self, res), res);
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        final IntermediateCode head = new FuncEntry(name, parameters.size());
        lt.assignLabelToCode(name, head);
        IntermediateCode last = head;
        int tempCount = tc;
        final FuncDefNode self = this;
        st = new SymbolTable(st, new HashMap<String, FuncDefNode>() {{
            put(name, self);
        }});
        SymbolTable next = st.yield(Collections.emptyMap());
        for (FuncParamNode paramNode : parameters) {
            final ICodeInfo code = paramNode.iCode(lt, next, lpBegin, lpEnd, tempCount).second;
            next = next.update(new HashMap<String, VarDefNode>(next.getHead()) {{
                put(paramNode.name, self);
            }});
            last.link(code.first);
            last = code.second;
            tempCount = code.tempCount;
        }
        final ICodeInfo body = block.iCode(lt, next, lpBegin, lpEnd, tempCount).second;
        last.link(body.first);
        return Pair.of(st, new ICodeInfo(head, body.second, null, body.tempCount));
    }
}
