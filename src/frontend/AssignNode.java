package frontend;

import exceptions.SysYException;
import midend.*;
import utils.Pair;

import java.util.Optional;

public class AssignNode implements StmtNode {
    public final LValNode left;
    public final ExprNode right;

    public AssignNode(LValNode left, ExprNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + left.toString() + ") := (" + right.toString() + ")";
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        right.check(symbolTable, inLoop);
        left.check(symbolTable, inLoop);
        final Optional<VarDefNode> defNode = symbolTable.find(left.name);
        if (defNode.isPresent()) {
            if (!defNode.get().getInfo().get(left.name).first) {
                errors.add(Pair.of(left.line, SysYException.Code.h));
            }
        }
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        return Pair.of(symbolTable, new AssignNode((LValNode) left.simplify(symbolTable).second,
                (ExprNode) right.simplify(symbolTable).second));
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        final Optional<VarDefNode> defNode = st.find(left.name);
        final int depth = st.tellDepth(left.name);
        assert defNode.isPresent();
        final Pair<ExprNode, ExprNode> dim = defNode.get().getInfo().get(left.name).second;
        final int defDim1 = ((ConstNode) dim.first).constant;
        final int defDim2 = ((ConstNode) dim.second).constant;
        final ICodeInfo rightValue = right.iCode(lt, st, lpBegin, lpEnd, tc).second;
        if (defDim1 == 0 && defDim2 == 0) {
            final WordValue word = new WordValue(left.name + "%" + depth);
            final IntermediateCode code = new Move(false, word, rightValue.finalSym);
            rightValue.second.link(code);
            return Pair.of(st, new ICodeInfo(rightValue.first, code, word, rightValue.tempCount));
        } else if (defDim1 != 0 && defDim2 == 0) {
            final ICodeInfo offsetCode = left.indexes.second.iCode(lt, st, lpBegin, lpEnd, rightValue.tempCount).second;
            rightValue.second.link(offsetCode.first);
            final AddrValue addr = new AddrValue(String.valueOf(offsetCode.tempCount + 1));
            final IntermediateCode calAddr = new AssignBinaryOperation(true, addr,
                    AssignBinaryOperation.BinaryOperation.ADD,
                    new AddrValue(left.name + "%" + depth), offsetCode.finalSym);
            offsetCode.second.link(calAddr);
            final IntermediateCode save = new Save(addr, rightValue.finalSym);
            calAddr.link(save);
            return Pair.of(st, new ICodeInfo(rightValue.first, save, null, offsetCode.tempCount + 1));
        } else {
            assert defDim1 != 0;
            final ICodeInfo rowExprCode = left.indexes.first.iCode(lt, st, lpBegin, lpEnd, rightValue.tempCount).second;
            rightValue.second.link(rowExprCode.first);
            final WordValue rowOffset = new WordValue(String.valueOf(rowExprCode.tempCount + 1));
            final IntermediateCode calRowOffset = new AssignBinaryOperation(true, rowOffset,
                    AssignBinaryOperation.BinaryOperation.MUL,
                    rowExprCode.finalSym, new ImmValue(defDim2));
            rowExprCode.second.link(calRowOffset);
            final ICodeInfo colExprCode = left.indexes.second.iCode(lt, st, lpBegin, lpEnd,
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
                    new AddrValue(left.name + "%" + depth), offset);
            calOffset.link(calAddr);
            final IntermediateCode save = new Save(addr, rightValue.finalSym);
            calAddr.link(save);
            return Pair.of(st, new ICodeInfo(rightValue.first, save, null, colExprCode.tempCount + 2));
        }
    }
}
