package frontend;

import exceptions.SysYException;
import midend.*;
import utils.Pair;

import java.util.Optional;
import java.util.stream.Collectors;

import static frontend.ConstNode.ZERO;

public class LValNode implements ExprNode {
    public final String name;
    public final int line;
    public final Pair<ExprNode, ExprNode> indexes;

    public LValNode(Token identifier, Pair<ExprNode, ExprNode> indexes) {
        this(identifier.content, identifier.line, indexes);
    }

    public LValNode(String name, int line, Pair<ExprNode, ExprNode> indexes) {
        this.name = name;
        this.line = line;
        this.indexes = indexes;
    }

    @Override
    public String toString() {
        return name + indexes.toString();
    }

    @Override
    public ReturnType getRetType(SymbolTable symbolTable) throws SysYException {
        final Optional<VarDefNode> defNode = symbolTable.find(name);
        if (defNode.isPresent()) {
            final Pair<ExprNode, ExprNode> defInfo = defNode.get().getInfo().get(name).second;
            final int dimDef = defInfo.first == ZERO ? 0 : (defInfo.second == ZERO ? 1 : 2);
            final int calDef = indexes.second == ZERO ? 0 : (indexes.first == ZERO ? 1 : 2);
            switch (dimDef - calDef) {
                case 0: return ReturnType.INT;
                case 1: return ReturnType.DIM1;
                case 2: return ReturnType.DIM2;
                default: throw new SysYException(SysYException.Code.e);
            }
        }
        throw new SysYException(SysYException.Code.c);
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        indexes.first.check(symbolTable, inLoop);
        indexes.second.check(symbolTable, inLoop);
        final Optional<VarDefNode> defNode = symbolTable.find(name);
        if (!defNode.isPresent()) errors.add(Pair.of(line, SysYException.Code.c));
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        final Pair<ExprNode, ExprNode> p = Pair.of((ExprNode) indexes.first.simplify(symbolTable).second,
                (ExprNode) indexes.second.simplify(symbolTable).second);
        if (p.first instanceof ConstNode && p.second instanceof ConstNode) {
            final Optional<VarDefNode> defNode = symbolTable.find(name);
            assert defNode.isPresent();
            final VarDefNode varDefNode = defNode.get();
            if (!varDefNode.getInfo().get(name).first && varDefNode instanceof DeclNode) {
                final DeclNode declNode = (DeclNode) varDefNode;
                final DefNode initDef = declNode.defNodes.stream().filter(d -> d.name.equals(name))
                        .collect(Collectors.toList()).get(0);
                final int index = ((ConstNode) p.first).constant * ((ConstNode) initDef.dimensions.second).constant
                        + ((ConstNode) p.second).constant;
                return Pair.of(symbolTable, initDef.initValues.get(index));
            }
        }
        return Pair.of(symbolTable, new LValNode(name, line, p));
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        final Optional<VarDefNode> defNode = st.find(name);
        final int depth = st.tellDepth(name);
        assert defNode.isPresent();
        final VarDefNode varDefNode = defNode.get();
        final Pair<ExprNode, ExprNode> dim = varDefNode.getInfo().get(name).second;
        final int defDim1 = ((ConstNode) dim.first).constant;
        final int defDim2 = ((ConstNode) dim.second).constant;
        if (defDim1 == 0 && defDim2 == 0) {
            final WordValue word = new WordValue(String.valueOf(tc + 1));
            final IntermediateCode code = new Move(true, word, new WordValue(name + "%" + depth));
            return Pair.of(st, new ICodeInfo(code, code, word, tc + 1));
        } else if (defDim1 != 0 && defDim2 == 0) {
            if (indexes.second == ZERO) { // addr
                final AddrValue addr = new AddrValue(String.valueOf(tc + 1));
                final IntermediateCode fetch = new Move(true, addr, new AddrValue(name + "%" + depth));
                return Pair.of(st, new ICodeInfo(fetch, fetch, addr, tc + 1));
            } else { // word
                final ICodeInfo exprCode = indexes.second.iCode(lt, st, lpBegin, lpEnd, tc).second;
                final AddrValue addr = new AddrValue(String.valueOf(exprCode.tempCount + 1));
                final IntermediateCode calAddr = new AssignBinaryOperation(true, addr,
                        AssignBinaryOperation.BinaryOperation.ADD,
                        new AddrValue(name + "%" + depth), exprCode.finalSym);
                exprCode.second.link(calAddr);
                final WordValue word = new WordValue(String.valueOf(exprCode.tempCount + 2));
                final IntermediateCode load = new Load(true, word, addr);
                calAddr.link(load);
                return Pair.of(st, new ICodeInfo(exprCode.first, load, word, exprCode.tempCount + 2));
            }
        } else {
            assert defDim1 != 0;
            if (indexes.first == ZERO && indexes.second == ZERO) { // addr
                final AddrValue addr = new AddrValue(String.valueOf(tc + 1));
                final IntermediateCode fetch = new Move(true, addr, new AddrValue(name + "%" + depth));
                return Pair.of(st, new ICodeInfo(fetch, fetch, addr, tc + 1));
            } else if (indexes.first == ZERO) { // addr
                final ICodeInfo exprCode = indexes.second.iCode(lt, st, lpBegin, lpEnd, tc).second;
                final WordValue offset = new WordValue(String.valueOf(exprCode.tempCount + 1));
                final IntermediateCode calOffset = new AssignBinaryOperation(true, offset,
                        AssignBinaryOperation.BinaryOperation.MUL,
                        exprCode.finalSym, new ImmValue(defDim2));
                exprCode.second.link(calOffset);
                final AddrValue addr = new AddrValue(String.valueOf(exprCode.tempCount + 2));
                final IntermediateCode calAddr = new AssignBinaryOperation(true, addr,
                        AssignBinaryOperation.BinaryOperation.ADD,
                        new AddrValue(name + "%" + depth), offset);
                calOffset.link(calAddr);
                return Pair.of(st, new ICodeInfo(exprCode.first, calAddr, addr, exprCode.tempCount + 2));
            } else { // word
                final ICodeInfo rowExprCode = indexes.first.iCode(lt, st, lpBegin, lpEnd, tc).second;
                final WordValue rowOffset = new WordValue(String.valueOf(rowExprCode.tempCount + 1));
                final IntermediateCode calRowOffset = new AssignBinaryOperation(true, rowOffset,
                        AssignBinaryOperation.BinaryOperation.MUL,
                        rowExprCode.finalSym, new ImmValue(defDim2));
                rowExprCode.second.link(calRowOffset);
                final ICodeInfo colExprCode = indexes.second.iCode(lt, st, lpBegin, lpEnd,
                        rowExprCode.tempCount + 1).second;
                final WordValue offset = new WordValue(String.valueOf(colExprCode.tempCount + 1));
                final IntermediateCode calOffset = new AssignBinaryOperation(true, offset,
                        AssignBinaryOperation.BinaryOperation.ADD,
                        rowOffset, colExprCode.finalSym);
                calRowOffset.link(colExprCode.first);
                colExprCode.second.link(calOffset);
                final AddrValue addr = new AddrValue(String.valueOf(colExprCode.tempCount + 2));
                final IntermediateCode calAddr = new AssignBinaryOperation(true, addr,
                        AssignBinaryOperation.BinaryOperation.ADD,
                        new AddrValue(name + "%" + depth), offset);
                calOffset.link(calAddr);
                final WordValue word = new WordValue(String.valueOf(colExprCode.tempCount + 3));
                final IntermediateCode load = new Load(true, word, addr);
                calAddr.link(load);
                return Pair.of(st, new ICodeInfo(rowExprCode.first, load, word, colExprCode.tempCount + 3));
            }
        }
    }
}
