package backend;

import midend.*;
import utils.Pair;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RegAllocator extends BasicBlockOptimizer {
    private static class ConflictGraph {
        private final Map<Value, Set<Value>> mapper = new HashMap<>();

        public int size() {
            return mapper.keySet().size();
        }

        public Set<Value> valSet() {
            return new HashSet<>(mapper.keySet());
        }

        public void addVal(Value value) {
            if (!mapper.containsKey(value)) {
                mapper.put(value, new HashSet<>());
            }
        }

        public void addEdge(Value a, Value b) {
            if (!mapper.containsKey(a)) {
                mapper.put(a, new HashSet<>());
            }
            if (!mapper.containsKey(b)) {
                mapper.put(b, new HashSet<>());
            }
            if (a.equals(b)) return;
            mapper.get(a).add(b);
            mapper.get(b).add(a);
        }

        public Optional<Value> find(Predicate<Set<Value>> predicate) {
            return mapper.keySet().stream().filter(v -> predicate.test(mapper.get(v))).findFirst();
        }

        public Set<Value> removeVal(Value value) {
            final Set<Value> edges = mapper.remove(value);
            for (Value v : mapper.keySet()) {
                mapper.get(v).remove(value);
            }
            return edges;
        }

        public void restore(Value value, Set<Value> edges) {
            mapper.put(value, edges);
            for (Value v : edges) {
                if (mapper.containsKey(v)) {
                    mapper.get(v).add(value);
                }
            }
        }
    }

    private final Map<Value, Reg> regMap = new HashMap<>();
    private final ConflictGraph conflictGraph = new ConflictGraph();
    private FlowGraph flowGraph;

    public Optional<Reg> findReg(Value value) {
        return regMap.containsKey(value) ? Optional.of(regMap.get(value)) : Optional.empty();
    }

    @Override
    protected void prepare(FlowGraph flowGraph, Pair<BasicBlock, BasicBlock> basicBlock) {
        activeDefUse(basicBlock, false);
        reachInOut(flowGraph, basicBlock);
        activeInOut(flowGraph, basicBlock);
    }

    @Override
    public Pair<BasicBlock, BasicBlock> optimize(LabelTable lt, FlowGraph flowGraph,
                                                 Pair<BasicBlock, BasicBlock> basicBlock) {
        this.flowGraph = flowGraph;
        prepare(flowGraph, basicBlock);
        BasicBlock p = basicBlock.first;
        while (true) {
            IntermediateCode code = p.getTail();
            final Set<Value> out = new HashSet<>(activeOut.get(p));
            while (true) {
                boolean add = false;
                if (code instanceof Definite) {
                    final Value def = ((Definite) code).getDef();
                    if (!def.isGlobal()) {
                        if (code instanceof Usage<?>) {
                            for (Value v : out) {
                                if (!v.equals(def) && !v.isGlobal()) {
                                    conflictGraph.addEdge(v, def);
                                    add = true;
                                }
                            }
                        }
                        if (!add) {
                            conflictGraph.addVal(def);
                        }
                        out.remove(def);
                    }
                }
                if (code instanceof Usage<?>) {
                    out.addAll(((Usage<?>) code).getUse().stream()
                            .filter(v -> !(v instanceof ImmValue) && !v.isGlobal()).collect(Collectors.toSet()));
                }
                if (code == p.getHead()) break;
                code = code.getPrev();
            }
            if (p == basicBlock.second) break;
            p = p.getNext();
        }
        final Stack<Pair<Value, Set<Value>>> stack = new Stack<>();
        final Set<Value> noReg = new HashSet<>();
        final List<Reg> registers = RegScheduler.regs.stream().filter(Reg::isSaved).collect(Collectors.toList());
        while (conflictGraph.size() > 0) {
            final Optional<Value> value = conflictGraph.find(s -> s.size() < registers.size());
            if (value.isPresent()) {
                final Set<Value> edges = conflictGraph.removeVal(value.get());
                stack.push(Pair.of(value.get(), edges));
            } else {
                final Set<Value> vals = conflictGraph.valSet();
                final Value v = new ArrayList<>(vals).get(vals.size() / 2);
                final Set<Value> edges = conflictGraph.removeVal(v);
                stack.push(Pair.of(v, edges));
                noReg.add(v);
                conflictGraph.removeVal(v);
            }
        }
        while (!stack.isEmpty()) {
            final Pair<Value, Set<Value>> value = stack.pop();
            conflictGraph.restore(value.first, value.second);
            if (!noReg.contains(value.first)) {
                final Set<Reg> conflictRegs = value.second.stream()
                        .map(this::findReg).filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet());
                regMap.put(value.first, registers.stream()
                        .filter(r -> !conflictRegs.contains(r))
                        .collect(Collectors.toList()).get(0));
            }
        }
        return basicBlock;
    }

    public boolean active(IntermediateCode code, Value value) {
        final BasicBlock block = flowGraph.getBlock(code);
        IntermediateCode p = block.getTail();
        final Set<Value> active = new HashSet<>(activeOut.get(block));
        while (p != code) {
            if (p instanceof Definite) {
                final Value def = ((Definite) p).getDef();
                active.remove(def);
            }
            if (p instanceof Usage) {
                active.addAll(((Usage<?>) p).getUse().stream()
                        .filter(i -> !(i instanceof ImmValue)).collect(Collectors.toList()));
            }
            if (p == block.getHead()) break;
            p = p.getPrev();
        }
        return active.contains(value);
    }
}
