package midend;

import utils.LinkedNode;
import utils.Pair;

public class BasicBlock extends LinkedNode<BasicBlock> {
    public static BasicBlock allocNopBlock() {
        final IntermediateCode nop = new Nop();
        return new BasicBlock(nop, nop);
    }

    private final Pair<IntermediateCode, IntermediateCode> pair;

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

    @Override
    public BasicBlock link(BasicBlock node) {
        final BasicBlock ret = super.link(node);
        getTail().link(node.getHead());
        return ret;
    }

    @Override
    public BasicBlock replaceWith(BasicBlock node) {
        final BasicBlock last = super.replaceWith(node);
        prev.getTail().link(node.getHead());
        last.getTail().link(next.getHead());
        return last;
    }
}
