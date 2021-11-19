package backend;

import midend.*;
import utils.Pair;

import java.util.*;

public class Translator {
    @FunctionalInterface
    private interface Mapper {
        Pair<Pair<MIPSCode, MIPSCode>, IntermediateCode> trans(Translator translator, IntermediateCode code);
    }

    private static final Map<Class<? extends IntermediateCode>, Mapper> trans =
            Collections.unmodifiableMap(
                    new HashMap<Class<? extends IntermediateCode>, Mapper>() {{
                        put(AssignBinaryOperation.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            final List<Reg> regs = new LinkedList<>();
                            MIPSCode p = f;
                            final AssignBinaryOperation code = (AssignBinaryOperation) c;
                            final Pair<MIPSCode, Element> op1 = getRegForSymOrImm(t, code.op1, p, regs);
                            p = op1.first;
                            final Pair<MIPSCode, Element> op2 = getRegForSymOrImm(t, code.op2, p, regs);
                            p = op2.first;
                            final Pair<MIPSCode, Element> left = getRegForSymOrImm(t, code.left, p, regs, false);
                            p = left.first;
                            final Pair<MIPSCode, MIPSCode> tr =
                                    fromBinaryOperation(code, left.second, op1.second, op2.second);
                            p.link(tr.first);
                            p = tr.second;
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(AssignUnaryOperation.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            final List<Reg> regs = new LinkedList<>();
                            MIPSCode p = f;
                            final AssignUnaryOperation code = (AssignUnaryOperation) c;
                            final Pair<MIPSCode, Element> op = getRegForSymOrImm(t, code.op, p, regs);
                            p = op.first;
                            final Pair<MIPSCode, Element> left = getRegForSymOrImm(t, code.left, p, regs, false);
                            p = left.first;
                            final Pair<MIPSCode, MIPSCode> tr =
                                    fromUnaryOperation(code, left.second, op.second);
                            p.link(tr.first);
                            p = tr.second;
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(Branch.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            final Branch code = (Branch) c;
                            final Pair<MIPSCode, Element> op = getRegForSymOrImm(t, code.condition, p, new LinkedList<>());
                            p = op.first;
                            assert code.label.startsWith("@");
                            p = p.link(new MIPSCode.BranchNotEq((Reg) op.second, Reg._0, code.label.substring(1)));
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(CallFunction.class, (t, c) -> {
                            t.pushCount = 0;
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            final Map<Reg, Value> cur = t.scheduler.current();
                            for (Reg r : cur.keySet()) {
                                p = p.link(new MIPSCode.StoreCode(r, t.addressMap.get(cur.get(r))));
                            }
                            p = p.link(new MIPSCode.StoreCode(Reg.SP, new RelativeAddress(Reg.SP, 4)));
                            p = p.link(new MIPSCode.StoreCode(Reg.RET_ADR, new RelativeAddress(Reg.SP, 0)));
                            final CallFunction code = (CallFunction) c;
                            assert !code.label.startsWith("@");
                            p = p.link(new MIPSCode.JumpAndLink("funct_" + code.label));
                            p = p.link(new MIPSCode.LoadCode(Reg.RET_ADR, new RelativeAddress(Reg.SP, 0)));
                            p = p.link(new MIPSCode.LoadCode(Reg.SP, new RelativeAddress(Reg.SP, 4)));
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(Declaration.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            final Declaration code = (Declaration) c;
                            assert !code.global;
                            final RelativeAddress address = (RelativeAddress) t.addressMap.get(code.symbol);
                            List<Value> initValues = code.initValues;
                            for (int i = 0, initValuesSize = initValues.size(); i < initValuesSize; ++i) {
                                Value v = initValues.get(i);
                                final Pair<MIPSCode, Element> op = getRegForSymOrImm(t, v, p, new LinkedList<>());
                                p = op.first;
                                p = p.link(new MIPSCode.StoreCode((Reg) op.second,
                                        new RelativeAddress(address.base, address.offset + i * 4)));
                            }
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(Exit.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.LoadImmCode(Reg.RET_VAL, new Imm(10));
                            MIPSCode p = f;
                            p = p.link(new MIPSCode.Syscall());
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(FuncEntry.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            int count = 8; // $ra -> 0($sp); $sp -> 4($sp)
                            final FuncEntry code = (FuncEntry) c;
                            while (c.getNext() != null && !(c.getNext() instanceof FuncEntry)) {
                                if (c instanceof IntroSpace) {
                                    final Pair<Value, Integer> valuePairSize = ((IntroSpace) c).getSize();
                                    // allocate space
                                    t.addressMap.put(valuePairSize.first, new RelativeAddress(Reg.SP, count));
                                    count += valuePairSize.second * 4;
                                }
                                c = c.getNext();
                            }
                            t.frameSize = count;
                            p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.ADDIU,
                                    Reg.SP, Reg.SP, new Imm(-count)));
                            t.symbolList.tagTable.assign("funct_" + code.label, f);
                            return Pair.of(Pair.of(f, p), code.getNext());
                        });
                        put(GetInt.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.LoadImmCode(Reg.RET_VAL, new Imm(5));
                            MIPSCode p = f;
                            p = p.link(new MIPSCode.Syscall());
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(Jump.class, (t, c) -> {
                            assert ((Jump) c).label.startsWith("@");
                            final MIPSCode f = new MIPSCode.JumpCode(((Jump) c).label.substring(1));
                            return Pair.of(Pair.of(f, f), c.getNext());
                        });
                        put(Load.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            final List<Reg> regs = new LinkedList<>();
                            final Load code = (Load) c;
                            final Pair<MIPSCode, Element> op = getRegForSymOrImm(t, code.base, p, regs);
                            p = op.first;
                            final Pair<MIPSCode, Element> left = getRegForSymOrImm(t, code.left, p, regs, false);
                            p = left.first;
                            p = p.link(new MIPSCode.LoadCode((Reg) left.second,
                                    new RelativeAddress((Reg) op.second, 0)));
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(Move.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            final List<Reg> regs = new LinkedList<>();
                            final Move code = (Move) c;
                            final Pair<MIPSCode, Element> right = getRegForSymOrImm(t, code.right, p, regs);
                            p = right.first;
                            final Pair<MIPSCode, Element> left = getRegForSymOrImm(t, code.left, p, regs);
                            p = left.first;
                            if (right.second instanceof Reg) {
                                p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.ADDIU,
                                        (Reg) left.second, (Reg) right.second, Imm.ZERO_IMM));
                            } else {
                                p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.ADDIU,
                                        (Reg) left.second, Reg._0, (Imm) right.second));
                            }
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(Nop.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            return Pair.of(Pair.of(f, f), c.getNext());
                        });
                        put(ParameterFetch.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            return Pair.of(Pair.of(f, f), c.getNext());
                        });
                        put(Print.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            final Print code = (Print) c;
                            final List<String> strings = Arrays.asList(code.string
                                    .substring(1, code.string.length() - 1).split("%d"));
                            for (int i = 0; i < strings.size(); ++i) {
                                final String s = strings.get(i);
                                final String label = "str_" + t.strCount++;
                                t.symbolList.insertDirective(new AsciizDirective(label, s));
                                p = p.link(new MIPSCode.LoadAddressCode(Reg.AR, label));
                                p = p.link(new MIPSCode.LoadImmCode(Reg.RET_VAL, new Imm(4)));
                                p = p.link(new MIPSCode.Syscall());
                                if (i < strings.size() - 1) { // %d
                                    p = p.link(new MIPSCode.LoadCode(Reg.AR, new RelativeAddress(Reg.SP, i - 4)));
                                    p = p.link(new MIPSCode.LoadImmCode(Reg.RET_VAL, new Imm(1)));
                                    p = p.link(new MIPSCode.Syscall());
                                }
                            }
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(PushArgument.class, (t, c) -> {
                            t.pushCount++;
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            final PushArgument code = (PushArgument) c;
                            final Pair<MIPSCode, Element> arg = getRegForSymOrImm(t, code.name, p, new LinkedList<>());
                            p = arg.first;
                            p = p.link(new MIPSCode.StoreCode((Reg) arg.second,
                                    new RelativeAddress(Reg.SP, t.pushCount * 4)));
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(Return.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            final Return code = (Return) c;
                            if (code.name != null) {
                                final Pair<MIPSCode, Element> ret = getRegForSymOrImm(t, code.name, p, new LinkedList<>());
                                p = ret.first;
                                p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.ADDIU,
                                        Reg.RET_VAL, (Reg) ret.second, Imm.ZERO_IMM));
                            }
                            p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.ADDIU,
                                    Reg.SP, Reg.SP, new Imm(t.frameSize)));
                            p = p.link(new MIPSCode.JumpReg(Reg.RET_ADR));
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(Save.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            final Save code = (Save) c;
                            final List<Reg> regs = new LinkedList<>();
                            final Pair<MIPSCode, Element> right = getRegForSymOrImm(t, code.right, p, regs);
                            p = right.first;
                            final Pair<MIPSCode, Element> base = getRegForSymOrImm(t, code.base, p, regs);
                            p = base.first;
                            p = p.link(new MIPSCode.StoreCode((Reg) right.second,
                                    new RelativeAddress((Reg) base.second, 0)));
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                    }});

    private static Pair<MIPSCode, MIPSCode> fromBinaryOperation(AssignBinaryOperation code,
                                                                Element left, Element op1, Element op2) {
        assert left instanceof Reg;
        final MIPSCode f = new MIPSCode.NopCode();
        MIPSCode p = f;
        if (op1 instanceof Imm && op2 instanceof Imm) {
            p = p.link(new MIPSCode.LoadImmCode((Reg) left, (Imm) op1));
            p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.fromBinary(code.operation),
                    (Reg) left, (Reg) left, (Imm) op2));
        } else if (op1 instanceof Imm) {
            p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.fromBinary(code.operation),
                    (Reg) left, (Reg) op2, (Imm) op1));
        } else if (op2 instanceof Imm) {
            p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.fromBinary(code.operation),
                    (Reg) left, (Reg) op1, (Imm) op2));
        } else {
            p = p.link(new MIPSCode.BinaryRegRegCode(MIPSCode.BinaryRegRegCode.Op.fromBinary(code.operation),
                    (Reg) left, (Reg) op1, (Reg) op2));
        }
        return Pair.of(f, p);
    }

    private static Pair<MIPSCode, MIPSCode> fromUnaryOperation(AssignUnaryOperation code,
                                                               Element left, Element op) {
        assert left instanceof Reg;
        final MIPSCode f = new MIPSCode.NopCode();
        MIPSCode p = f;
        if (op instanceof Imm) {
            switch (code.operation) {
                case POS:
                    p = p.link(new MIPSCode.LoadImmCode((Reg) left, (Imm) op));
                    break;
                case NEG:
                    p = p.link(new MIPSCode.LoadImmCode((Reg) left, new Imm(-((Imm) op).value)));
                    break;
                case NOT:
                    p = p.link(new MIPSCode.LoadImmCode((Reg) left, new Imm(((Imm) op).value != 0 ? 0 : 1)));
                    break;
            }
        } else {
            switch (code.operation) {
                case POS:
                    p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.ADDIU,
                            (Reg) left, (Reg) op, Imm.ZERO_IMM));
                    break;
                case NEG:
                    p = p.link(new MIPSCode.BinaryRegRegCode(MIPSCode.BinaryRegRegCode.Op.SUBU,
                            (Reg) left, Reg._0, (Reg) op));
                    break;
                case NOT:
                    p = p.link(new MIPSCode.BinaryRegRegCode(MIPSCode.BinaryRegRegCode.Op.SEQ,
                            (Reg) left, (Reg) op, Reg._0));
                    break;
            }
        }
        return Pair.of(f, p);
    }

    private static Pair<MIPSCode, Element> getRegForSymOrImm(Translator t, Value varName, MIPSCode p, List<Reg> h, boolean load) {
        if (varName.symbol.equals(Return.RET_SYM)) return Pair.of(p, Reg.RET_VAL);
        if (!(varName instanceof ImmValue)) {
            final Pair<MIPSCode, Reg> q = allocReg(t, varName, p, h, load);
            h.add(q.second);
            return Pair.of(q.first, q.second); // Reg
        } else {
            return Pair.of(p, new Imm(((ImmValue) varName).value)); // Imm
        }
    }

    private static Pair<MIPSCode, Element> getRegForSymOrImm(Translator t, Value varName, MIPSCode p, List<Reg> h) {
        return getRegForSymOrImm(t, varName, p, h, true);
    }

    private static Pair<MIPSCode, Reg> allocReg(Translator t, Value varName, MIPSCode p, Collection<Reg> h, boolean load) {
        Optional<Reg> op = t.scheduler.allocReg(varName, h);
        while (!op.isPresent()) {
            final Pair<Value, Reg> store = t.scheduler.overflow(h);
            p = p.link(new MIPSCode.StoreCode(store.second, t.addressMap.get(store.first)));
            op = t.scheduler.allocReg(varName, h);
            if (op.isPresent()) {
                if (load) {
                    p = p.link(new MIPSCode.LoadCode(op.get(), t.addressMap.get(varName)));
                }
                break;
            }
        }
        return Pair.of(p, op.get());
    }

    private final IntermediateCode head;
    private final RegScheduler scheduler;
    private final LabelTable labelTable;
    private final SymbolList symbolList;
    private final Map<Value, Address> addressMap = new HashMap<>();
    private int frameSize = 0;
    private int strCount = 0;
    private int pushCount = 0;

    public Translator(IntermediateCode head, RegScheduler scheduler, LabelTable labelTable) {
        final SymbolList symbolList = new SymbolList();
        extractDataSymbols(labelTable, symbolList, head);
        symbolList.getGlobalDirectives()
                .forEach(d -> addressMap.put(((WordDirective) d).name, new AbsoluteAddress(d.label)));
        this.head = head;
        this.scheduler = scheduler;
        this.labelTable = labelTable;
        this.symbolList = symbolList;
    }

    public String translate() {
        final StringJoiner sj = new StringJoiner("\n");
        Pair<MIPSCode, MIPSCode> emit = emitMIPSCode();
        sj.add(symbolList.displayDirectives());
        MIPSCode p = emit.first;
        sj.add(".text");
        while (p != null) {
            final Optional<List<String>> labels = symbolList.tagTable.find(p);
            labels.ifPresent(strings -> sj.add(strings.stream()
                    .map(s -> s + ":").reduce((x, y) -> x + "\n" + y).orElse("")));
            if (!(p instanceof MIPSCode.NopCode)) {
                sj.add(p.toString());
            }
            p = p.getNext();
        }
        return sj.toString();
    }

    private Pair<MIPSCode, MIPSCode> emitMIPSCode() {
        IntermediateCode interP = head;
        while (!(interP instanceof CallFunction)) interP = interP.getNext();
        final Pair<Pair<MIPSCode, MIPSCode>, IntermediateCode> firstTrans =
                trans.get(interP.getClass()).trans(this, interP);
        final MIPSCode firstCode = new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.ADDIU,
                Reg.SP, Reg.SP, new Imm(-8));
        firstCode.link(firstTrans.first.first);
        MIPSCode q = firstTrans.first.second;
        interP = firstTrans.second;
        while (interP != null) {
            Optional<List<String>> labels = labelTable.find(interP);
            Pair<Pair<MIPSCode, MIPSCode>, IntermediateCode> nextCode =
                    trans.get(interP.getClass()).trans(this, interP);
            labels.ifPresent(s -> s.forEach(l -> symbolList.tagTable.assign(l.startsWith("@") ? l.substring(1) : l,
                    nextCode.first.first)));
            q.link(nextCode.first.first);
            q = nextCode.first.second;
            interP = nextCode.second;
        }
        return Pair.of(firstCode, q);
    }

    private static void extractDataSymbols(LabelTable labelTable, SymbolList symbolList, IntermediateCode head) {
        final IntermediateVirtualMachine machine = new IntermediateVirtualMachine(head);
        machine.run(labelTable, p -> p instanceof CallFunction);
        final List<Map<Value, List<Integer>>> globalSymbols = machine.getProgStack();
        assert globalSymbols.size() == 1;
        for (Value symName : globalSymbols.get(0).keySet()) {
            symbolList.insertDirective(new WordDirective(symName, globalSymbols.get(0).get(symName)));
        }
    }
}
