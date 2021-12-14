package midend;

import utils.Pair;

import java.util.*;

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
            final Optional<List<String>> labels = p.getNext() != null ? lt.find(p.getNext()) : Optional.empty();
            if (labels.isPresent() || p instanceof Jump || p instanceof Branch || p instanceof Return) {
                final BasicBlock block = new BasicBlock(start, p);
                lt.find(start).orElseGet(Collections::emptyList).forEach(l -> flowGraph.addBlockLabel(l, block));
                tail.link(block);
                tail = tail.getNext();
                p = p.getNext();
                start = p;
            } else {
                p = p.getNext();
            }
        }
        final BasicBlock block = new BasicBlock(start, p);
        lt.find(start).orElseGet(Collections::emptyList).forEach(l -> flowGraph.addBlockLabel(l, block));
        tail.link(block);
        tail = tail.getNext();
        BasicBlock q = head;
        do {
            q = q.getNext();
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
    private final Map<IntermediateCode, Set<IntermediateCode>> reachInOfICode = new HashMap<>();
    private final Map<IntermediateCode, Set<IntermediateCode>> reachOutOfICode = new HashMap<>();
    private final Map<BasicBlock, Set<IntermediateCode>> reachInOfBlock = new HashMap<>();
    private final Map<BasicBlock, Set<IntermediateCode>> reachOutOfBlock = new HashMap<>();

    private final Map<Value, Set<IntermediateCode>> activeDefOfICode = new HashMap<>();
    private final Map<BasicBlock, Set<Value>> activeUse = new HashMap<>();
    private final Map<BasicBlock, Set<Value>> activeDef = new HashMap<>();
    private final Map<BasicBlock, Set<Value>> activeIn = new HashMap<>();
    private final Map<BasicBlock, Set<Value>> activeOut = new HashMap<>();

    private void reachInOut(FlowGraph flowGraph, Pair<BasicBlock, BasicBlock> basicBlock) {
        final Map<BasicBlock, Set<IntermediateCode>> reachGenOfBlock = new HashMap<>();
        final Map<BasicBlock, Set<IntermediateCode>> reachKilOfBlock = new HashMap<>();
        BasicBlock p = basicBlock.first;
        while (p != null) {
            IntermediateCode code = p.getHead();
            while (true) {
                if (code instanceof Definite) {
                    reachGenOfICode.put(code, new HashSet<>());
                    reachKilOfICode.put(code, new HashSet<>(activeDefOfICode.get(((Definite) code).getDef())));
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
        boolean diff;
        do {
            BasicBlock bp = basicBlock.first;
            diff = false;
            while (bp != null) {
                final List<BasicBlock> prev = flowGraph.prevOf(bp);
                if (!reachInOfBlock.containsKey(bp)) reachInOfBlock.put(bp, new HashSet<>());
                final int iSize = reachInOfBlock.get(bp).size();
                // in[p] = \cup_{q\in prev[p]} out[q]
                for (BasicBlock b : prev) {
                    reachInOfBlock.get(bp).addAll(reachOutOfBlock.getOrDefault(b, Collections.emptySet()));
                }
                if (iSize != reachInOfBlock.get(bp).size()) diff = true;

                if (!reachOutOfBlock.containsKey(bp)) reachOutOfBlock.put(bp, new HashSet<>());
                final int oSize = reachOutOfBlock.get(bp).size();
                final Set<IntermediateCode> realIn = new HashSet<>(reachInOfBlock.get(bp));
                realIn.removeAll(reachKilOfBlock.get(bp));
                // out[p] = gen[p] \cup (in[p] - kill[p])
                reachOutOfBlock.get(bp).addAll(reachGenOfBlock.get(bp));
                reachOutOfBlock.get(bp).addAll(realIn);
                if (oSize != reachOutOfBlock.get(bp).size()) diff = true;
                if (bp == basicBlock.second) break;
                bp = bp.getNext();
            }
        } while(diff);
        BasicBlock bp = basicBlock.first;
        while (bp != null) {
            IntermediateCode code = bp.getHead();
            reachInOfICode.put(code, reachInOfBlock.get(bp));
            final Set<IntermediateCode> realIn = new HashSet<>(reachInOfICode.get(code));
            realIn.removeAll(reachKilOfICode.get(code));
            reachOutOfICode.put(code, new HashSet<>(reachGenOfICode.get(code)));
            reachOutOfICode.get(code).addAll(realIn);
            if (code != bp.getTail()) {
                code = code.getNext();
                while (code != null) {
                    reachInOfICode.put(code, reachOutOfICode.get(code.getPrev()));
                    final Set<IntermediateCode> rin = new HashSet<>(reachInOfICode.get(code));
                    rin.removeAll(reachKilOfICode.get(code));
                    reachOutOfICode.put(code, new HashSet<>(reachGenOfICode.get(code)));
                    reachOutOfICode.get(code).addAll(rin);
                    if (code == bp.getTail()) break;
                    code = code.getNext();
                }
            }
            if (bp == basicBlock.second) break;
            bp = bp.getNext();
        }
    }

    private void activeDefUse(Pair<BasicBlock, BasicBlock> basicBlock) {
        BasicBlock p = basicBlock.first;
        while (p != null) {
            activeDef.put(p, new HashSet<>());
            activeUse.put(p, new HashSet<>());
            IntermediateCode code = p.getHead();
            while (true) {
                if (code instanceof Definite) {
                    final Value def = ((Definite) code).getDef();
                    activeDef.get(p).add(def);
                    if (!activeDefOfICode.containsKey(def)) activeDefOfICode.put(def, new HashSet<>());
                    activeDefOfICode.get(def).add(code);
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
                for (BasicBlock b : flowGraph.nextOf(p)) {
                    activeOut.get(p).addAll(activeIn.get(b));
                }
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
                        final List<Assignment> reach = new LinkedList<>();
                        for (IntermediateCode c : reachInOfICode.get(code)) {
                            if (c instanceof Assignment) {
                                final Assignment assignment = (Assignment) c;
                                if (assignment.right().size() == 1 && assignment.right().get(0).equals(use.get(i))) {
                                    reach.add(assignment);
                                }
                            }
                        }
                        if (reach.size() == 1 && reach.get(0).right().get(0) instanceof ImmValue) {
                            aft.set(i, reach.get(0).right().get(0));
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
                    boolean find = false;
                    for (Set<Value> vs : activeUse.values()) {
                        if (vs.contains(defValue)) {
                            find = true;
                            break;
                        }
                    }
                    if (!defValue.symbol.endsWith("%1") /* global */ && !find) {
                        final IntermediateCode nop = new Nop();
                        code.replaceWith(nop);
                        lt.reassignCode(code, nop);
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
        activeDefUse(basicBlock);
        reachInOut(flowGraph, basicBlock);
        activeInOut(flowGraph, basicBlock);
        propagateConst(lt, basicBlock);
        deleteUnusedCode(lt, basicBlock);
        // TODO: allocate registers
        final Pair<IntermediateCode, IntermediateCode> s =
                new Optimizer.SimplifyConst().apply(lt, Pair.of(basicBlock.first.getHead(), basicBlock.second.getTail()));
        basicBlock.first.setHead(s.first);
        basicBlock.second.setTail(s.second);
        return basicBlock;
    }
}
