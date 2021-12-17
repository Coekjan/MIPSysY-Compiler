package midend;

import utils.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface Optimizer {
    Pair<IntermediateCode, IntermediateCode> apply(LabelTable lt, Pair<IntermediateCode, IntermediateCode> block);

    class RemoveNop implements Optimizer {
        @Override
        public Pair<IntermediateCode, IntermediateCode> apply(LabelTable lt, Pair<IntermediateCode, IntermediateCode> block) {
            final IntermediateCode head = new Nop();
            IntermediateCode tail = head;
            IntermediateCode p = block.first;
            while (true) {
                if (!(p instanceof Nop)) {
                    tail.link(p);
                    tail = tail.getNext();
                } else {
                    final Optional<List<String>> labels = lt.find(p);
                    if (labels.isPresent()) {
                        for (String s : labels.get()) {
                            lt.assignLabelToCode(s, p.getNext());
                        }
                    }
                }
                if (p == block.second) break;
                p = p.getNext();
            }
            return Pair.of(tail == head ? head : head.getNext(), tail);
        }
    }

    class RemoveRedundantLabel implements Optimizer {
        private final Set<String> usedLabels = new HashSet<>();
        private final Set<String> referredLabels = new HashSet<>();

        @Override
        public Pair<IntermediateCode, IntermediateCode> apply(LabelTable lt, Pair<IntermediateCode, IntermediateCode> block) {
            final IntermediateCode head = new Nop();
            IntermediateCode tail = head;
            IntermediateCode p = block.first;
            while (true) {
                if (p instanceof Jump) {
                    final String realTarget = getLeafLabel(lt, ((Jump) p).label);
                    referredLabels.add(((Jump) p).label);
                    usedLabels.add(realTarget);
                    tail.link(new Jump(realTarget));
                } else if (p instanceof Branch) {
                    final String realTarget = getLeafLabel(lt, ((Branch) p).label);
                    referredLabels.add(((Branch) p).label);
                    usedLabels.add(realTarget);
                    tail.link(new Branch(((Branch) p).condition, realTarget));
                } else if (p instanceof FuncEntry) {
                    lt.find(p).ifPresent(usedLabels::addAll);
                    tail.link(p);
                } else {
                    tail.link(p);
                }
                tail = tail.getNext();
                if (p == block.second) break;
                p = p.getNext();
            }
            lt.minifyLabels(usedLabels, referredLabels);
            return Pair.of(tail == head ? head : head.getNext(), tail);
        }

        private String getLeafLabel(LabelTable lt, String label) {
            IntermediateCode p = lt.find(label);
            while (p instanceof Nop && p.getNext() != null) {
                p = p.getNext();
            }
            return (p instanceof Jump && !((Jump) p).label.equals(label)) ? getLeafLabel(lt, ((Jump) p).label) : label;
        }
    }

    class SimplifyConst implements Optimizer {
        @Override
        public Pair<IntermediateCode, IntermediateCode> apply(LabelTable lt, Pair<IntermediateCode, IntermediateCode> block) {
            final IntermediateCode head = new Nop();
            IntermediateCode tail = head;
            IntermediateCode p = block.first;
            while (true) {
                if (p instanceof ProbablyConst) {
                    tail.link(((ProbablyConst) p).simplify());
                } else {
                    tail.link(p);
                }
                tail = tail.getNext();
                if (p == block.second) break;
                p = p.getNext();
            }
            return Pair.of(tail == head ? head : head.getNext(), tail);
        }
    }

    interface BlockOptimizer extends Optimizer {
        Pair<BasicBlock, BasicBlock> optimize(LabelTable lt, FlowGraph flowGraph, Pair<BasicBlock, BasicBlock> basicBlock);

        @Override
        default Pair<IntermediateCode, IntermediateCode> apply(LabelTable lt, Pair<IntermediateCode, IntermediateCode> block) {
            final Pair<FlowGraph, Pair<BasicBlock, BasicBlock>> extractResult = BasicBlockOptimizer.extract(lt, block);
            final Pair<BasicBlock, BasicBlock> optimized = optimize(lt, extractResult.first, extractResult.second);
            return Pair.of(optimized.first.getHead(), optimized.second.getTail());
        }
    }
}
