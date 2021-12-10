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
            while (p != block.second) {
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
                p = p.getNext();
            }
            return Pair.of(tail == head ? head : head.getNext(), tail);
        }
    }

    class RemoveRedundantLabel implements Optimizer {
        private final Set<String> usedLabels = new HashSet<>();

        @Override
        public Pair<IntermediateCode, IntermediateCode> apply(LabelTable lt, Pair<IntermediateCode, IntermediateCode> block) {
            final IntermediateCode head = new Nop();
            IntermediateCode tail = head;
            IntermediateCode p = block.first;
            while (p != block.second) {
                if (p instanceof Jump) {
                    final String realTarget = getLeafLabel(lt, ((Jump) p).label);
                    usedLabels.add(realTarget);
                    tail.link(new Jump(realTarget));
                } else if (p instanceof Branch) {
                    final String realTarget = getLeafLabel(lt, ((Branch) p).label);
                    usedLabels.add(realTarget);
                    tail.link(new Branch(((Branch) p).condition, realTarget));
                } else {
                    tail.link(p);
                }
                tail = tail.getNext();
                p = p.getNext();
            }
            lt.minifyLabels(usedLabels);
            return Pair.of(tail == head ? head : head.getNext(), tail);
        }

        private String getLeafLabel(LabelTable lt, String label) {
            final IntermediateCode p = lt.find(label);
            return p instanceof Jump ? getLeafLabel(lt, ((Jump) p).label) : label;
        }
    }
}
