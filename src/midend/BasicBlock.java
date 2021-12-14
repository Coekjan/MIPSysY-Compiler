package midend;

import utils.LinkedNode;
import utils.Pair;

public class BasicBlock extends LinkedNode<BasicBlock> {
    public static BasicBlock allocNopBlock() {
        final IntermediateCode nop = new Nop();
        return new BasicBlock(nop, nop);
    }

    private Pair<IntermediateCode, IntermediateCode> pair;

    public BasicBlock(Pair<IntermediateCode, IntermediateCode> iCodePair) {
        pair = iCodePair;
    }

    public BasicBlock(IntermediateCode head, IntermediateCode tail) {
        this(Pair.of(head, tail));
    }

    public IntermediateCode getHead() {
        return pair.first;
    }

    public IntermediateCode getTail() {
        return pair.second;
    }

    public Pair<IntermediateCode, IntermediateCode> getPair() {
        return pair;
    }

    public void setHead(IntermediateCode code) {
        pair = Pair.of(code, pair.second);
    }

    public void setTail(IntermediateCode code) {
        pair = Pair.of(pair.first, code);
    }
}
