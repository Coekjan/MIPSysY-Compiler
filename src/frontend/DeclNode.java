package frontend;

import exceptions.SysYException;
import midend.*;
import utils.Pair;

import java.util.*;

import static frontend.ConstNode.ZERO;

public class DeclNode implements BlockItemNode, VarDefNode {
    public final boolean modifiable;
    public final List<DefNode> defNodes;

    public DeclNode(boolean modifiable, List<DefNode> defNodes) {
        this.modifiable = modifiable;
        this.defNodes = Collections.unmodifiableList(defNodes);
    }

    @Override
    public String toString() {
        return (modifiable ? "INT " : "CONST INT ") + defNodes.stream()
                .map(Objects::toString).reduce((x, y) -> x + ", " + y).orElse("");
    }

    @Override
    public Map<String, Pair<Boolean, Pair<ExprNode, ExprNode>>> getInfo() {
        return Collections.unmodifiableMap(new HashMap<String, Pair<Boolean, Pair<ExprNode, ExprNode>>>() {{
            defNodes.forEach(d -> put(d.name, Pair.of(modifiable, d.dimensions)));
        }});
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        SymbolTable next = symbolTable;
        for (DefNode defNode : defNodes) {
            defNode.check(next, inLoop);
            Map<String, VarDefNode> map = new HashMap<>(next.getHead());
            if (map.containsKey(defNode.name)) {
                errors.add(Pair.of(defNode.line, SysYException.Code.b));
            } else {
                map.put(defNode.name, this);
            }
            next = next.update(map);
        }
        return next;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        SymbolTable next = symbolTable;
        final List<DefNode> simDef = new LinkedList<>();
        DeclNode temp = this;
        for (DefNode defNode : defNodes) {
            final Pair<SymbolTable, SyntaxNode> p = defNode.simplify(next);
            Map<String, VarDefNode> map = new HashMap<>(next.getHead());
            map.put(defNode.name, temp);
            simDef.add((DefNode) p.second);
            final DeclNode old = temp;
            temp = new DeclNode(modifiable, simDef);
            next = next.update(map).fixVarRef(old, temp);
        }
        final DeclNode res = new DeclNode(modifiable, simDef);
        return Pair.of(next, res);
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        return iCode(lt, st, lpBegin, lpEnd, tc, false);
    }

    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc, boolean global) {
        SymbolTable next = st;
        int tempCount = tc;
        IntermediateCode last = new Nop();
        final IntermediateCode head = last;
        final DeclNode self = this;
        for (DefNode defNode : defNodes) {
            final String name = defNode.name;
            next = next.update(new HashMap<String, VarDefNode>(next.getHead()) {{
                put(name, self);
            }});
            final List<Value> initValues = new ArrayList<>();
            for (ExprNode e : defNode.initValues) {
                final ICodeInfo code = e.iCode(lt, next, lpBegin, lpEnd, tempCount).second;
                last.link(code.first);
                last = code.second;
                initValues.add(code.finalSym);
                tempCount = code.tempCount;
            }
            if (defNode.dimensions.first == ZERO && defNode.dimensions.second == ZERO) {
                assert initValues.size() <= 1;
                final WordValue word = new WordValue(name + "%" + next.tellDepth(name));
                final Declaration decl = new Declaration(global, modifiable, word, 1, initValues);
                last.link(decl);
                last = decl;
            } else {
                final AddrValue addr = new AddrValue(name + "%" + next.tellDepth(name));
                final int size = defNode.dimensions.second == ZERO ?
                        ((ConstNode) defNode.dimensions.first).constant :
                        ((ConstNode) defNode.dimensions.first).constant *
                                ((ConstNode) defNode.dimensions.second).constant;
                final Declaration decl = new Declaration(global, modifiable, addr, size, initValues);
                last.link(decl);
                last = decl;
            }
        }
        return Pair.of(next, new ICodeInfo(head, last, null, tempCount));
    }
}
