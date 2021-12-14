package midend;

import utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JUST for function body, not for global
 */
public class BasicBlockOptimizer implements Optimizer.BlockOptimizer {
    /**
     * transfer a list of code (in function) to basic block
     * @param lt label table
     * @param iCodeList list of code, the first should be the first code after parameter-fetch,
     *                  and the second should be the last code of function(last return)
     * @return flow graph and the list of basic block(the first is the first block of the function, and the second is
     * the last block of the function)
     */
    public static Pair<FlowGraph, Pair<BasicBlock, BasicBlock>> extract(LabelTable lt, Pair<IntermediateCode, IntermediateCode> iCodeList) {
        final FlowGraph flowGraph = new FlowGraph();
        final BasicBlock head = BasicBlock.allocNopBlock();
        BasicBlock tail = head;
        IntermediateCode p = iCodeList.first;
        IntermediateCode start = p;
        while (p != iCodeList.second) {
            final Optional<List<String>> labels = lt.find(p.getNext());
            if (labels.isPresent() || p instanceof Jump || p instanceof Branch || p instanceof Return) {
                final BasicBlock block = new BasicBlock(start, p);
                final Optional<List<String>> startLabels = lt.find(start);
                startLabels.orElseGet(Collections::emptyList).forEach(l -> flowGraph.addBlockLabel(l, block));
                tail.link(block);
                tail = tail.getNext();
                p = p.getNext();
                start = p;
            } else {
                p = p.getNext();
            }
        }
        final BasicBlock block = new BasicBlock(start, p);
        final Optional<List<String>> startLabels = lt.find(start);
        startLabels.orElseGet(Collections::emptyList).forEach(l -> flowGraph.addBlockLabel(l, block));
        tail.link(block);
        tail = tail.getNext();
        BasicBlock q = head;
        do {
            q = q.getNext();
            if (q == null) break;
            final IntermediateCode lastICode = q.getTail();
            if (lastICode instanceof Jump) {
                flowGraph.link(q, flowGraph.getBlockByLabel(((Jump) lastICode).label));
            } else if (lastICode instanceof Branch) {
                flowGraph.link(q, flowGraph.getBlockByLabel(((Branch) lastICode).label));
                if (q != tail) flowGraph.link(q, q.getNext());
            } else if (!(lastICode instanceof Return)) {
                if (q != tail) flowGraph.link(q, q.getNext());
            }
        } while (q != tail);
        return Pair.of(flowGraph, Pair.of(head == tail ? head : head.getNext(), tail));
    }

    private final Map<IntermediateCode, Set<IntermediateCode>> reachGenOfICode = new HashMap<>();
    private final Map<IntermediateCode, Set<IntermediateCode>> reachKilOfICode = new HashMap<>();
    private final Map<BasicBlock, Set<IntermediateCode>> reachGenOfBlock = new HashMap<>();
    private final Map<BasicBlock, Set<IntermediateCode>> reachKilOfBlock = new HashMap<>();
    private final Map<IntermediateCode, Set<IntermediateCode>> reachInOfICode = new HashMap<>();
    private final Map<IntermediateCode, Set<IntermediateCode>> reachOutOfICode = new HashMap<>();
    private final Map<BasicBlock, Set<IntermediateCode>> reachInOfBlock = new HashMap<>();
    private final Map<BasicBlock, Set<IntermediateCode>> reachOutOfBlock = new HashMap<>();

    private void reachGenKil(Pair<BasicBlock, BasicBlock> basicBlock) {
        BasicBlock p = basicBlock.first;
        while (p != null) {
            IntermediateCode code = p.getHead();
            while (true) {
                if (code instanceof Definite) {
                    reachGenOfICode.put(code, new HashSet<>());
                    reachKilOfICode.put(code, new HashSet<>());
                    reachGenOfICode.get(code).add(code);
                } else {
                    reachGenOfICode.put(code, Collections.emptySet());
                    reachKilOfICode.put(code, Collections.emptySet());
                }
                if (code == p.getTail()) break;
                code = code.getNext();
            }
            if (p == basicBlock.second) break;
            p = p.getNext();
        }
        for (IntermediateCode code : reachGenOfICode.keySet()) {
            if (code instanceof Definite) {
                for (IntermediateCode other : reachGenOfICode.keySet()) {
                    if (other != code) {
                        if (reachGenOfICode.get(other).stream()
                                .anyMatch(i -> ((Definite) i).getDef().equals(((Definite) code).getDef()))) {
                            reachKilOfICode.get(code).add(other);
                        }
                    }
                }
            }
        }
        p = basicBlock.first;
        while (p != null) {
            IntermediateCode code = p.getTail();
            final Set<IntermediateCode> kilSet = new HashSet<>();
            final Set<IntermediateCode> genSet = new HashSet<>();
            while (true) {
                final Set<IntermediateCode> realGen = new HashSet<>(reachGenOfICode.get(code));
                realGen.removeAll(kilSet);
                genSet.addAll(realGen);
                kilSet.addAll(reachKilOfICode.get(code));
                if (code == p.getHead()) break;
                code = code.getPrev();
            }
            reachGenOfBlock.put(p, genSet);
            reachKilOfBlock.put(p, kilSet);
            if (p == basicBlock.second) break;
            p = p.getNext();
        }
    }

    private void reachInOut(FlowGraph flowGraph, Pair<BasicBlock, BasicBlock> basicBlock) {
        boolean diff;
        do {
            BasicBlock p = basicBlock.first;
            diff = false;
            while (p != null) {
                final List<BasicBlock> prev = flowGraph.prevOf(p);
                if (!reachInOfBlock.containsKey(p)) reachInOfBlock.put(p, new HashSet<>());
                final int iSize = reachInOfBlock.get(p).size();
                // in[p] = \cup_{q\in prev[p]} out[q]
                reachInOfBlock.get(p).addAll(prev.stream()
                        .map(b -> reachOutOfBlock.getOrDefault(b, Collections.emptySet()))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet()));
                if (iSize != reachInOfBlock.get(p).size()) diff = true;

                if (!reachOutOfBlock.containsKey(p)) reachOutOfBlock.put(p, new HashSet<>());
                final int oSize = reachOutOfBlock.get(p).size();
                final Set<IntermediateCode> realIn = new HashSet<>(reachInOfBlock.get(p));
                realIn.removeAll(reachKilOfBlock.getOrDefault(p, Collections.emptySet()));
                // out[p] = gen[p] \cup (in[p] - kill[p])
                reachOutOfBlock.get(p).addAll(reachGenOfBlock.getOrDefault(p, Collections.emptySet()));
                reachOutOfBlock.get(p).addAll(realIn);
                if (oSize != reachOutOfBlock.get(p).size()) diff = true;
                if (p == basicBlock.second) break;
                p = p.getNext();
            }
        } while(diff);
        BasicBlock p = basicBlock.first;
        while (p != null) {
            IntermediateCode code = p.getHead();
            reachInOfICode.put(code, new HashSet<>(reachInOfBlock.get(p)));
            final Set<IntermediateCode> realIn = new HashSet<>(reachInOfICode.get(code));
            realIn.removeAll(reachKilOfICode.getOrDefault(code, Collections.emptySet()));
            reachOutOfICode.put(code, new HashSet<>(reachGenOfICode.get(code)));
            reachOutOfICode.get(code).addAll(realIn);
            code = code.getNext();
            while (code != null) {
                reachInOfICode.put(code, new HashSet<>(reachOutOfICode.get(code.getPrev())));
                final Set<IntermediateCode> rin = new HashSet<>(reachInOfICode.get(code));
                rin.removeAll(reachKilOfICode.getOrDefault(code, Collections.emptySet()));
                reachOutOfICode.put(code, new HashSet<>(reachGenOfICode.getOrDefault(code, Collections.emptySet())));
                reachOutOfICode.get(code).addAll(rin);
                if (code == p.getTail()) break;
                code = code.getNext();
            }
            if (p == basicBlock.second) break;
            p = p.getNext();
        }
    }

    private final Map<BasicBlock, Set<Value>> activeUse = new HashMap<>();
    private final Map<BasicBlock, Set<Value>> activeDef = new HashMap<>();
    private final Map<BasicBlock, Set<Value>> activeIn = new HashMap<>();
    private final Map<BasicBlock, Set<Value>> activeOut = new HashMap<>();

    private void activeDefUse(Pair<BasicBlock, BasicBlock> basicBlock) {
        BasicBlock p = basicBlock.first;
        while (p != null) {
            activeDef.put(p, new HashSet<>());
            activeUse.put(p, new HashSet<>());
            IntermediateCode code = p.getHead();
            while (true) {
                if (code instanceof Definite) {
                    activeDef.get(p).add(((Definite) code).getDef());
                }
                if (code instanceof Usage) {
                    activeUse.get(p).addAll(((Usage<?>) code).getUse());
                }
                if (code == p.getTail()) break;
                code = code.getNext();
            }
            if (p == basicBlock.second) break;
            p = p.getNext();
        }
    }

    private void activeInOut(FlowGraph flowGraph, Pair<BasicBlock, BasicBlock> basicBlock) {
        BasicBlock q = basicBlock.first;
        while (q != null) {
            activeIn.put(q, new HashSet<>());
            activeOut.put(q, new HashSet<>());
            if (q == basicBlock.second) break;
            q = q.getNext();
        }
        boolean diff;
        do {
            BasicBlock p = basicBlock.second;
            diff = false;
            while (p != null) {
                final int oSize = activeOut.get(p).size();
                activeOut.get(p).addAll(flowGraph.nextOf(p).stream()
                        .map(b -> activeIn.getOrDefault(b, Collections.emptySet()))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet()));
                if (oSize != activeOut.get(p).size()) diff = true;
                final int iSize = activeIn.get(p).size();
                final Set<Value> realOut = new HashSet<>(activeOut.get(p));
                realOut.removeAll(activeDef.get(p));
                activeIn.get(p).addAll(activeUse.get(p));
                activeIn.get(p).addAll(realOut);
                if (iSize != activeIn.get(p).size()) diff = true;
                if (p == basicBlock.first) break;
                p = p.getPrev();
            }
        } while (diff);
    }

    private void propagateConst(LabelTable lt, Pair<BasicBlock, BasicBlock> basicBlock) {
        BasicBlock p = basicBlock.first;
        while (p != null) {
            IntermediateCode code = p.getHead();
            while (true) {
                if (code instanceof Usage) {
                    final List<Value> use = ((Usage<?>) code).getUse();
                    final List<Value> aft = new ArrayList<>(use);
                    for (int i = 0; i < use.size(); ++i) {
                        final int finalI = i;
                        final List<Move> reach = reachInOfICode.get(code).stream()
                                .filter(c -> c instanceof Move)
                                .map(c -> (Move) c)
                                .filter(c -> c.left.equals(use.get(finalI)))
                                .collect(Collectors.toList());
                        if (reach.size() == 1 && reach.get(0).right instanceof ImmValue) {
                            aft.set(i, reach.get(0).right);
                        }
                    }
                    final IntermediateCode aftCode = ((Usage<?>) code).replaceUse(aft);
                    code.replaceWith(aftCode);
                    if (code == p.getHead()) p.setHead(aftCode);
                    if (code == p.getTail()) p.setTail(aftCode);
                    lt.reassignCode(code, aftCode);
                    code = aftCode;
                }
                if (code == p.getTail()) break;
                code = code.getNext();
            }
            if (p == basicBlock.second) break;
            p = p.getNext();
        }
    }

    private void deleteUnusedCode(LabelTable lt, Pair<BasicBlock, BasicBlock> basicBlock) {
        BasicBlock p = basicBlock.first;
        while (p != null) {
            IntermediateCode code = p.getHead();
            while (true) {
                if (code instanceof Definite) {
                    final Value defValue = ((Definite) code).getDef();
                    if (!defValue.symbol.endsWith("%1") /* global */ && activeUse.values().stream()
                            .flatMap(Collection::stream)
                            .distinct().noneMatch(v -> v.equals(defValue))) {
                        code.remove();
                        lt.reassignCode(code, code.getNext());
                        // System.out.println(">>> REMOVE : " + code);
                    }
                }
                if (code == p.getTail()) break;
                code = code.getNext();
            }
            if (p == basicBlock.second) break;
            p = p.getNext();
        }
    }

    @Override
    public Pair<BasicBlock, BasicBlock> optimize(LabelTable lt, FlowGraph flowGraph, Pair<BasicBlock, BasicBlock> basicBlock) {
        reachGenKil(basicBlock);
        reachInOut(flowGraph, basicBlock);
        activeDefUse(basicBlock);
        activeInOut(flowGraph, basicBlock);
        propagateConst(lt, basicBlock);
        deleteUnusedCode(lt, basicBlock);
        // TODO: delete dead-code, allocate registers
        return basicBlock;
    }
}
