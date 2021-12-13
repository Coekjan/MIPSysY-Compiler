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
                            if (code.op1 instanceof AddrValue || code.op2 instanceof AddrValue) {
                                assert !(code.op1 instanceof AddrValue) || !(code.op2 instanceof AddrValue);
                                if (code.op1 instanceof AddrValue) {
                                    if (op2.second instanceof Imm) {
                                        final Pair<MIPSCode, MIPSCode> tr =
                                                fromBinaryOperation(code, left.second, op1.second, new Imm(((Imm) op2.second).value << 2));
                                        p.link(tr.first);
                                        p = tr.second;
                                        return Pair.of(Pair.of(f, p), c.getNext());
                                    } else {
                                        p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.SLL,
                                                (Reg) op2.second, (Reg) op2.second, new Imm(2)));
                                    }
                                } else {
                                    if (op1.second instanceof Imm) {
                                        final Pair<MIPSCode, MIPSCode> tr =
                                                fromBinaryOperation(code, left.second, new Imm(((Imm) op1.second).value << 2), op2.second);
                                        p.link(tr.first);
                                        p = tr.second;
                                        return Pair.of(Pair.of(f, p), c.getNext());
                                    } else {
                                        p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.SLL,
                                                (Reg) op1.second, (Reg) op1.second, new Imm(2)));
                                    }
                                }
                            }
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
                            final List<Reg> removeList = new LinkedList<>();
                            for (Reg r : cur.keySet()) {
                                final Value v = cur.get(r);
                                if (v instanceof WordValue || Character.isDigit(v.symbol.charAt(0))) {
                                    p = p.link(new MIPSCode.StoreCode(r, t.addressMap.get(v)));
                                }
                                removeList.add(r);
                            }
                            removeList.forEach(t.scheduler::remove);
                            p = p.link(new MIPSCode.StoreCode(Reg.RET_ADR, new RelativeAddress(Reg.SP, 0)));
                            final CallFunction code = (CallFunction) c;
                            assert !code.label.startsWith("@");
                            p = p.link(new MIPSCode.JumpAndLink("funct_" + code.label));
                            p = p.link(new MIPSCode.LoadCode(Reg.RET_ADR, new RelativeAddress(Reg.SP, 0)));
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
                                if (op.second instanceof Imm) {
                                    p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.ADDIU,
                                            Reg.CT, Reg._0, (Imm) op.second));
                                    p = p.link(new MIPSCode.StoreCode(Reg.CT,
                                            new RelativeAddress(address.base, address.offset + i * 4)));
                                } else {
                                    p = p.link(new MIPSCode.StoreCode((Reg) op.second,
                                            new RelativeAddress(address.base, address.offset + i * 4)));
                                }
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
                            t.params.clear();
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            int count = 4; // $ra -> 0($sp)
                            final FuncEntry code = (FuncEntry) c;
                            while (c.getNext() != null && !(c.getNext() instanceof FuncEntry)) {
                                c = c.getNext();
                            }
                            while (c != null && !(c instanceof FuncEntry) && !(c instanceof ParameterFetch)) {
                                if (c instanceof IntroSpace) {
                                    final Pair<Value, Integer> valuePairSize = ((IntroSpace) c).getSize();
                                    // allocate space
                                    if (valuePairSize.second > 0) {
                                        t.addressMap.put(valuePairSize.first, new RelativeAddress(Reg.SP, count));
                                        count += valuePairSize.second * 4;
                                    }
                                }
                                c = c.getPrev();
                            }
                            c = code.getNext();
                            while (c instanceof ParameterFetch) {
                                final Pair<Value, Integer> valuePairSize = ((ParameterFetch) c).getSize();
                                t.addressMap.put(valuePairSize.first, new RelativeAddress(Reg.SP, count));
                                count += valuePairSize.second * 4;
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
                            final Pair<MIPSCode, Element> left = getRegForSymOrImm(t, code.left, p, regs, false);
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
                            t.params.add(((ParameterFetch) c).name);
                            final MIPSCode f = new MIPSCode.NopCode();
                            return Pair.of(Pair.of(f, f), c.getNext());
                        });
                        put(Print.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            final Print code = (Print) c;
                            String s = code.string.substring(1, code.string.length() - 1);
                            int i = s.indexOf("%d");
                            int count = 0;
                            while (i >= 0) {
                                final String out = s.substring(0, i);
                                if (!out.isEmpty()) {
                                    final String label = "string_" + t.strCount++;
                                    t.symbolList.insertDirective(new AsciizDirective(label, out));
                                    p = p.link(new MIPSCode.LoadLabelAddressCode(Reg.AR, label));
                                    p = p.link(new MIPSCode.LoadImmCode(Reg.RET_VAL, new Imm(4)));
                                    p = p.link(new MIPSCode.Syscall());
                                }
                                p = p.link(new MIPSCode.LoadCode(Reg.AR,
                                        new RelativeAddress(Reg.SP, -(t.pushCount * 4) + count * 4)));
                                p = p.link(new MIPSCode.LoadImmCode(Reg.RET_VAL, new Imm(1)));
                                p = p.link(new MIPSCode.Syscall());
                                s = s.substring(i + 2);
                                i = s.indexOf("%d");
                                count++;
                            }
                            if (!s.isEmpty()) {
                                final String label = "string_" + t.strCount++;
                                t.symbolList.insertDirective(new AsciizDirective(label, s));
                                p = p.link(new MIPSCode.LoadLabelAddressCode(Reg.AR, label));
                                p = p.link(new MIPSCode.LoadImmCode(Reg.RET_VAL, new Imm(4)));
                                p = p.link(new MIPSCode.Syscall());
                            }
                            t.pushCount = 0;
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(PushArgument.class, (t, c) -> {
                            t.pushCount++;
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            final PushArgument code = (PushArgument) c;
                            final Pair<MIPSCode, Element> arg = getRegForSymOrImm(t, code.name, p, new LinkedList<>());
                            p = arg.first;
                            if (arg.second instanceof Reg) {
                                p = p.link(new MIPSCode.StoreCode((Reg) arg.second,
                                        new RelativeAddress(Reg.SP, -(t.pushCount * 4))));
                            } else {
                                p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.ADDIU,
                                        Reg.CT, Reg._0, (Imm) arg.second));
                                p = p.link(new MIPSCode.StoreCode(Reg.CT,
                                        new RelativeAddress(Reg.SP, -(t.pushCount * 4))));
                            }
                            return Pair.of(Pair.of(f, p), c.getNext());
                        });
                        put(Return.class, (t, c) -> {
                            final MIPSCode f = new MIPSCode.NopCode();
                            MIPSCode p = f;
                            final Return code = (Return) c;
                            if (code.name != null) {
                                final Pair<MIPSCode, Element> ret = getRegForSymOrImm(t, code.name, p, new LinkedList<>());
                                p = ret.first;
                                if (ret.second instanceof Imm) {
                                    p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.ADDIU,
                                            Reg.RET_VAL, Reg._0, (Imm) ret.second));
                                } else {
                                    p = p.link(new MIPSCode.BinaryRegImmCode(MIPSCode.BinaryRegImmCode.Op.ADDIU,
                                            Reg.RET_VAL, (Reg) ret.second, Imm.ZERO_IMM));
                                }
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
                            final Pair<MIPSCode, Element> base = getRegForSymOrImm(t, code.base, p, regs, true, true);
                            p = base.first;
                            if (right.second instanceof Imm) {
                                p = p.link(new MIPSCode.LoadImmCode(Reg.CT, (Imm) right.second));
                                p = p.link(new MIPSCode.StoreCode(Reg.CT,
                                        new RelativeAddress((Reg) base.second, 0)));
                            } else {
                                p = p.link(new MIPSCode.StoreCode((Reg) right.second,
                                        new RelativeAddress((Reg) base.second, 0)));
                            }
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
            p = p.link(new MIPSCode.LoadImmCode(Reg.CT, (Imm) op1));
            p = p.link(new MIPSCode.BinaryRegRegCode(MIPSCode.BinaryRegRegCode.Op.fromBinary(code.operation),
                    (Reg) left, Reg.CT, (Reg) op2));
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

    private static Pair<MIPSCode, Element> getRegForSymOrImm(Translator t, Value varName, MIPSCode p, List<Reg> h, boolean load, boolean left) {
        if (varName.symbol.equals(Return.RET_SYM)) return Pair.of(p, Reg.RET_VAL);
        if (!(varName instanceof ImmValue)) {
            final Pair<MIPSCode, Reg> q = allocReg(t, varName, p, h, load, left);
            h.add(q.second);
            return Pair.of(q.first, q.second); // Reg
        } else {
            return Pair.of(p, new Imm(((ImmValue) varName).value)); // Imm
        }
    }

    private static Pair<MIPSCode, Element> getRegForSymOrImm(Translator t, Value varName, MIPSCode p, List<Reg> h) {
        return getRegForSymOrImm(t, varName, p, h, true);
    }

    private static Pair<MIPSCode, Element> getRegForSymOrImm(Translator t, Value varName, MIPSCode p, List<Reg> h, boolean load) {
        return getRegForSymOrImm(t, varName, p, h, load, false);
    }

    private static Pair<MIPSCode, Reg> allocReg(Translator t, Value varName, MIPSCode p, Collection<Reg> h, boolean load, boolean left) {
        final Optional<Reg> find = t.scheduler.find(varName);
        if (find.isPresent()) return Pair.of(p, find.get());
        Optional<Reg> op = t.scheduler.allocReg(varName, h);
        while (!op.isPresent()) {
            final Pair<Value, Reg> store = t.scheduler.overflow(h);
            if (store.first instanceof WordValue || Character.isDigit(store.first.symbol.charAt(0))) {
                p = p.link(new MIPSCode.StoreCode(store.second, t.addressMap.get(store.first)));
            }
            op = t.scheduler.allocReg(varName, h);
        }
        if (load) {
            final Address address = t.addressMap.get(varName);
            if (address instanceof RelativeAddress || varName instanceof WordValue) {
                if (varName instanceof AddrValue && !left && !t.params.contains(varName) &&
                        !Character.isDigit(varName.symbol.charAt(0))) {
                    p = p.link(new MIPSCode.LoadAddressCode(op.get(), address));
                } else {
                    p = p.link(new MIPSCode.LoadCode(op.get(), address));
                }
            } else {
                p = p.link(new MIPSCode.LoadLabelAddressCode(op.get(), ((AbsoluteAddress) address).label));
            }
        }
        return Pair.of(p, op.get());
    }

    private final IntermediateCode head;
    private final RegScheduler scheduler;
    private final LabelTable labelTable;
    private final SymbolList symbolList;
    private final Map<Value, Address> addressMap = new HashMap<>();
    private final List<Value> params = new LinkedList<>();
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
            q = q.link(new MIPSCode.Comment(interP.toString()));
            if (labels.isPresent() || interP instanceof Jump || interP instanceof Branch || interP instanceof Return) {
                for (Reg reg : scheduler.current().keySet()) {
                    final Value key = scheduler.current().get(reg);
                    final Address address = addressMap.get(key);
                    if (address instanceof RelativeAddress || key instanceof WordValue) {
                        if (!(key instanceof AddrValue)) {
                            q = q.link(new MIPSCode.StoreCode(reg, address));
                        }
                    }
                }
                scheduler.clear();
            }
            final Pair<Pair<MIPSCode, MIPSCode>, IntermediateCode> nextCode;
            try {
                nextCode = trans.get(interP.getClass()).trans(this, interP);
            } catch (ClassCastException e) {
                System.err.println(interP);
                e.printStackTrace();
                System.exit(1);
                return null;
            }
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
