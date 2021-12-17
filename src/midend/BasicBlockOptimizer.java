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
        while (true) {
            IntermediateCode code = q.getHead();
            while (true) {
                flowGraph.put(code, q);
                if (code == q.getTail()) break;
                code = code.getNext();
            }
            if (q == tail) break;
            q = q.getNext();
        }
        q = head;
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

    private boolean diffSim = false;
    private final Map<BasicBlock, Set<IntermediateCode>> reachInOfBlock = new HashMap<>();
    private final Map<BasicBlock, Set<IntermediateCode>> reachOutOfBlock = new HashMap<>();

    protected final Map<Value, Set<IntermediateCode>> activeDefOfICode = new HashMap<>();
    protected final Map<BasicBlock, Set<Value>> activeUse = new HashMap<>();
    protected final Map<BasicBlock, Set<Value>> activeDef = new HashMap<>();
    protected final Map<BasicBlock, Set<Value>> activeIn = new HashMap<>();
    protected final Map<BasicBlock, Set<Value>> activeOut = new HashMap<>();

    protected boolean pathFind(FlowGraph flowGraph, BasicBlock curBlock,
                               IntermediateCode curCode, IntermediateCode tarCode, Value target) {
        final Set<BasicBlock> visited = new HashSet<>();
        return pathFinder(flowGraph, curBlock, curCode, tarCode, target, visited);
    }

    protected boolean pathFinder(FlowGraph flowGraph, BasicBlock curBlock,
                               IntermediateCode curCode, IntermediateCode tarCode, Value targetValue,
                               Set<BasicBlock> visited) {
        IntermediateCode p = curCode;
        visited.add(curBlock);
        while (p != tarCode) {
            if (p instanceof Assignment && ((Assignment) p).left().equals(targetValue)) {
                return true;
            }
            if (p == curBlock.getTail()) break;
            p = p.getNext();
        }
        if (p == tarCode) {
            return false;
        }
        for (BasicBlock b : flowGraph.nextOf(curBlock)) {
            if (!visited.contains(b)) {
                if (pathFinder(flowGraph, b, b.getHead(), tarCode, targetValue, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void reachInOut(FlowGraph flowGraph, Pair<BasicBlock, BasicBlock> basicBlock) {
        final Map<BasicBlock, Set<IntermediateCode>> gen = new HashMap<>();
        final Map<BasicBlock, Set<IntermediateCode>> kil = new HashMap<>();
        BasicBlock p = basicBlock.first;
        while (true) {
            gen.put(p, new HashSet<>());
            kil.put(p, new HashSet<>());
            IntermediateCode code = p.getTail();
            while (true) {
                if (code instanceof Definite) {
                    if (!kil.get(p).contains(code)) {
                        gen.get(p).add(code);
                    }
                    kil.get(p).addAll(activeDefOfICode.get(((Definite) code).getDef()));
                }
                if (code == p.getHead()) break;
                code = code.getPrev();
            }
            if (p == basicBlock.second) break;
            p = p.getNext();
        }
        boolean diff;
        do {
            diff = false;
            p = basicBlock.first;
            while (true) {
                if (!reachInOfBlock.containsKey(p)) {
                    reachInOfBlock.put(p, new HashSet<>());
                }
                if (!reachOutOfBlock.containsKey(p)) {
                    reachOutOfBlock.put(p, new HashSet<>(gen.get(p)));
                }
                for (BasicBlock block : flowGraph.prevOf(p)) {
                    reachInOfBlock.get(p).addAll(reachOutOfBlock.getOrDefault(block, Collections.emptySet()));
                }
                for (IntermediateCode code : reachInOfBlock.get(p)) {
                    if (!kil.get(p).contains(code) && !reachOutOfBlock.get(p).contains(code)) {
                        diff = true;
                        reachOutOfBlock.get(p).add(code);
                    }
                }
                if (p == basicBlock.second) break;
                p = p.getNext();
            }
        } while (diff);
    }

    protected void activeDefUse(Pair<BasicBlock, BasicBlock> basicBlock, boolean reachDef) {
        BasicBlock p = basicBlock.first;
        while (p != null) {
            activeDef.put(p, new HashSet<>());
            activeUse.put(p, new HashSet<>());
            if (reachDef) {
                IntermediateCode code = p.getHead();
                while (true) {
                    if (code instanceof Definite) {
                        final Value def = ((Definite) code).getDef();
                        activeDef.get(p).add(def);
                        if (!activeDefOfICode.containsKey(def)) activeDefOfICode.put(def, new HashSet<>());
                        activeDefOfICode.get(def).add(code);
                    }
                    if (code instanceof Usage) {
                        activeUse.get(p).addAll(((Usage<?>) code).getUse().stream()
                                .filter(i -> !(i instanceof ImmValue)).collect(Collectors.toList()));
                    }
                    if (code == p.getTail()) break;
                    code = code.getNext();
                }
            } else {
                IntermediateCode code = p.getTail();
                while (true) {
                    if (code instanceof Definite) {
                        final Value def = ((Definite) code).getDef();
                        activeDef.get(p).add(def);
                        if (!activeDefOfICode.containsKey(def)) activeDefOfICode.put(def, new HashSet<>());
                        activeDefOfICode.get(def).add(code);
                        activeUse.get(p).remove(def);
                    }
                    if (code instanceof Usage) {
                        activeUse.get(p).addAll(((Usage<?>) code).getUse().stream()
                                .filter(i -> !(i instanceof ImmValue)).collect(Collectors.toList()));
                    }
                    if (code == p.getHead()) break;
                    code = code.getPrev();
                }
            }
            if (p == basicBlock.second) break;
            p = p.getNext();
        }
    }

    protected void activeInOut(FlowGraph flowGraph, Pair<BasicBlock, BasicBlock> basicBlock) {
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
                for (Value v : activeOut.get(p)) {
                    if (!activeDef.get(p).contains(v)) {
                        activeIn.get(p).add(v);
                    }
                }
                activeIn.get(p).addAll(activeUse.get(p));
                if (iSize != activeIn.get(p).size()) diff = true;
                if (p == basicBlock.first) break;
                p = p.getPrev();
            }
        } while (diff);
    }

    protected Set<IntermediateCode> reachInOfCode(FlowGraph flowGraph, IntermediateCode code) {
        final BasicBlock block = flowGraph.getBlock(code);
        final Set<IntermediateCode> res = reachInOfBlock.get(block);
        IntermediateCode p = block.getHead();
        while (p != code) {
            if (p instanceof Definite) {
                final Value def = ((Definite) p).getDef();
                res.removeAll(activeDefOfICode.get(def));
                res.add(p);
            }
            p = p.getNext();
        }
        return res;
    }

    private void propagateValue(FlowGraph flowGraph, LabelTable lt, Pair<BasicBlock, BasicBlock> basicBlock) {
        BasicBlock p = basicBlock.first;
        while (p != null) {
            IntermediateCode code = p.getHead();
            while (true) {
                if (code instanceof Usage) {
                    final List<Value> use = ((Usage<?>) code).getUse();
                    final List<Value> aft = new ArrayList<>(use);
                    for (int i = 0; i < use.size(); ++i) {
                        final List<Assignment> reach = new LinkedList<>();
                        Set<IntermediateCode> reachIn = reachInOfCode(flowGraph, code);
                        for (IntermediateCode c : reachIn) {
                            if (c instanceof Assignment) {
                                final Assignment assignment = (Assignment) c;
                                if (assignment.right().size() == 1 && assignment.left() instanceof WordValue &&
                                        assignment.left().equals(use.get(i))) {
                                    reach.add(assignment);
                                }
                            }
                        }
                        if (reach.size() == 1) {
                            final Value value = reach.get(0).right().get(0);
                            if (value instanceof ImmValue) { // const
                                aft.set(i, value);
                                diffSim = true;
                            } else if (!use.get(0).symbol.endsWith("%1") && !value.symbol.equals(Return.RET_SYM)) {
                                final IntermediateCode curCode = (IntermediateCode) reach.get(0);
                                if (!pathFind(flowGraph, flowGraph.getBlock(curCode), curCode, code, value)) { // copy value
                                    aft.set(i, value);
                                    diffSim = true;
                                }
                            }
                        }
                    }
                    final IntermediateCode aftCode = ((Usage<?>) code).replaceUse(aft);
                    code.replaceWith(aftCode);
                    flowGraph.put(aftCode, p);
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
                        diffSim = true;
                        if (code == p.getHead()) {
                            p.setHead(nop);
                        }
                        if (code == p.getTail()) {
                            p.setTail(nop);
                            break;
                        }
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

    protected void prepare(FlowGraph flowGraph, Pair<BasicBlock, BasicBlock> basicBlock) {
        activeDefUse(basicBlock, true);
        reachInOut(flowGraph, basicBlock);
    }

    public boolean diff() {
        return diffSim;
    }

    @Override
    public Pair<BasicBlock, BasicBlock> optimize(LabelTable lt, FlowGraph flowGraph, Pair<BasicBlock, BasicBlock> basicBlock) {
        prepare(flowGraph, basicBlock);
        propagateValue(flowGraph, lt, basicBlock);
        deleteUnusedCode(lt, basicBlock);
        final Pair<IntermediateCode, IntermediateCode> s =
                new Optimizer.SimplifyConst().apply(lt, Pair.of(basicBlock.first.getHead(), basicBlock.second.getTail()));
        basicBlock.first.setHead(s.first);
        basicBlock.second.setTail(s.second);
        return basicBlock;
    }
}
